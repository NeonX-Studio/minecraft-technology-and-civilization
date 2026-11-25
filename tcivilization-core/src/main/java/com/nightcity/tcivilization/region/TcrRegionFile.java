package com.nightcity.tcivilization.region;

import com.mojang.logging.LogUtils;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionLayer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

/**
 * Binary .tcr region container storing an 8x8 block of regions (03 §5.2.5).
 * Header: magic "TCR1" (4B), formatVersion=1 (4B), dataVersion (4B), 64 * [offset(4B), length(4B), crc32(4B)].
 */
public final class TcrRegionFile {
    public static final byte[] MAGIC = "TCR1".getBytes(StandardCharsets.US_ASCII);
    public static final int FORMAT_VERSION = 1;
    public static final int REGIONS_PER_SIDE = 8;
    public static final int REGIONS_PER_FILE = REGIONS_PER_SIDE * REGIONS_PER_SIDE;
    public static final int INDEX_OFFSET = 12;
    public static final int INDEX_ENTRY_BYTES = 12;
    public static final int HEADER_BYTES = INDEX_OFFSET + REGIONS_PER_FILE * INDEX_ENTRY_BYTES;
    public static final int DEFLATE_LEVEL = 4;

    private static final Logger LOGGER = LogUtils.getLogger();

    private TcrRegionFile() {
    }

    public record RegionBlob(int version, Map<String, Tag> layers) {
        public static RegionBlob empty() {
            return new RegionBlob(FORMAT_VERSION, Map.of());
        }
    }

    public static Path resolvePath(Path worldRoot, String dimension, int fileX, int fileZ) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        Objects.requireNonNull(dimension, "dimension");
        String fileName = "r." + fileX + "." + fileZ + ".tcr";
        if ("minecraft:overworld".equals(dimension)) {
            return worldRoot.resolve("tc").resolve("regions").resolve(fileName);
        }
        int colon = dimension.indexOf(':');
        String ns = colon >= 0 ? dimension.substring(0, colon) : "minecraft";
        String dimPath = colon >= 0 ? dimension.substring(colon + 1) : dimension;
        return worldRoot.resolve("tc").resolve("regions").resolve(ns).resolve(dimPath).resolve(fileName);
    }

    public static int slotIndex(int regionX, int regionZ) {
        int lx = Math.floorMod(regionX, REGIONS_PER_SIDE);
        int lz = Math.floorMod(regionZ, REGIONS_PER_SIDE);
        return lz * REGIONS_PER_SIDE + lx;
    }

    public static int serializedTagSize(Tag tag) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            NbtIo.writeAnyTag(tag, dos);
        }
        return baos.size();
    }

    public static void validateLayerSizes(Map<String, Tag> layers) throws IOException {
        for (Map.Entry<String, Tag> entry : layers.entrySet()) {
            int bytes = serializedTagSize(entry.getValue());
            if (bytes > RegionLayer.MAX_SERIALIZED_BYTES) {
                throw new IllegalArgumentException("Region layer '" + entry.getKey() + "' exceeds 16 KiB limit: " + bytes + " bytes");
            }
        }
    }

    public static RegionBlob readRegion(
            Path file,
            RegionCell cell,
            long currentTick,
            BiConsumer<Path, String> onCorruptIsolated
    ) {
        if (!Files.exists(file)) {
            return RegionBlob.empty();
        }
        try {
            byte[] allBytes = Files.readAllBytes(file);
            validateHeader(allBytes);
            ByteBuffer header = ByteBuffer.wrap(allBytes, 0, HEADER_BYTES);
            int slot = slotIndex(cell.x(), cell.z());
            int entryPos = INDEX_OFFSET + slot * INDEX_ENTRY_BYTES;
            int offset = header.getInt(entryPos);
            int length = header.getInt(entryPos + 4);
            int expectedCrc = header.getInt(entryPos + 8);
            if (offset == 0 && length == 0) {
                return RegionBlob.empty();
            }
            if (offset < HEADER_BYTES || length <= 0 || offset + length > allBytes.length) {
                throw new IOException("Invalid region slice bounds at slot " + slot + ": offset=" + offset + ", length=" + length);
            }
            int actualCrc = computeCrc32(allBytes, offset, length);
            if (actualCrc != expectedCrc) {
                throw new IOException("CRC32 mismatch at slot " + slot + ": expected=" + Integer.toHexString(expectedCrc) + ", actual=" + Integer.toHexString(actualCrc));
            }
            return decodeBlob(allBytes, offset, length);
        } catch (Exception ex) {
            isolateCorruptFile(file, currentTick, ex.getMessage(), onCorruptIsolated);
            return RegionBlob.empty();
        }
    }

    public static void writeRegions(
            Path file,
            int dataVersion,
            Map<Integer, RegionBlob> updatesBySlot,
            long currentTick,
            BiConsumer<Path, String> onCorruptIsolated
    ) throws IOException {
        for (RegionBlob blob : updatesBySlot.values()) {
            if (blob != null) {
                validateLayerSizes(blob.layers());
            }
        }

        byte[][] existingCompressed = new byte[REGIONS_PER_FILE][];
        int[] existingCrcs = new int[REGIONS_PER_FILE];

        if (Files.exists(file)) {
            try {
                byte[] current = Files.readAllBytes(file);
                validateHeader(current);
                ByteBuffer hdr = ByteBuffer.wrap(current, 0, HEADER_BYTES);
                for (int slot = 0; slot < REGIONS_PER_FILE; slot++) {
                    if (updatesBySlot.containsKey(slot)) {
                        continue;
                    }
                    int pos = INDEX_OFFSET + slot * INDEX_ENTRY_BYTES;
                    int off = hdr.getInt(pos);
                    int len = hdr.getInt(pos + 4);
                    int crc = hdr.getInt(pos + 8);
                    if (off == 0 && len == 0) {
                        continue;
                    }
                    if (off < HEADER_BYTES || len <= 0 || off + len > current.length || computeCrc32(current, off, len) != crc) {
                        throw new IOException("Existing slot " + slot + " failed integrity check during update");
                    }
                    byte[] slice = new byte[len];
                    System.arraycopy(current, off, slice, 0, len);
                    existingCompressed[slot] = slice;
                    existingCrcs[slot] = crc;
                }
            } catch (Exception ex) {
                isolateCorruptFile(file, currentTick, ex.getMessage(), onCorruptIsolated);
                existingCompressed = new byte[REGIONS_PER_FILE][];
                existingCrcs = new int[REGIONS_PER_FILE];
            }
        }

        for (Map.Entry<Integer, RegionBlob> update : updatesBySlot.entrySet()) {
            int slot = update.getKey();
            if (slot < 0 || slot >= REGIONS_PER_FILE) {
                throw new IllegalArgumentException("Invalid region slot: " + slot);
            }
            RegionBlob blob = update.getValue();
            if (blob == null || blob.layers().isEmpty()) {
                existingCompressed[slot] = null;
                existingCrcs[slot] = 0;
            } else {
                byte[] compressed = encodeBlob(blob);
                existingCompressed[slot] = compressed;
                existingCrcs[slot] = computeCrc32(compressed, 0, compressed.length);
            }
        }

        int totalPayload = 0;
        for (byte[] block : existingCompressed) {
            if (block != null) {
                totalPayload += block.length;
            }
        }

        ByteBuffer out = ByteBuffer.allocate(HEADER_BYTES + totalPayload);
        out.put(MAGIC);
        out.putInt(FORMAT_VERSION);
        out.putInt(dataVersion);

        int cursor = HEADER_BYTES;
        for (int slot = 0; slot < REGIONS_PER_FILE; slot++) {
            byte[] block = existingCompressed[slot];
            if (block == null) {
                out.putInt(0);
                out.putInt(0);
                out.putInt(0);
            } else {
                out.putInt(cursor);
                out.putInt(block.length);
                out.putInt(existingCrcs[slot]);
                cursor += block.length;
            }
        }
        for (int slot = 0; slot < REGIONS_PER_FILE; slot++) {
            byte[] block = existingCompressed[slot];
            if (block != null) {
                out.put(block);
            }
        }
        out.flip();

        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            while (out.hasRemaining()) {
                channel.write(out);
            }
            channel.force(true);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void validateHeader(byte[] bytes) throws IOException {
        if (bytes.length < HEADER_BYTES) {
            throw new IOException("Truncated .tcr header: " + bytes.length + " bytes");
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (bytes[i] != MAGIC[i]) {
                throw new IOException("Invalid .tcr magic header");
            }
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes, 0, HEADER_BYTES);
        int formatVer = buf.getInt(4);
        if (formatVer < 1 || formatVer > FORMAT_VERSION) {
            throw new IOException("Unsupported .tcr formatVersion: " + formatVer);
        }
    }

    private static byte[] encodeBlob(RegionBlob blob) throws IOException {
        CompoundTag root = new CompoundTag();
        root.putInt("version", blob.version());
        CompoundTag layersTag = new CompoundTag();
        for (Map.Entry<String, Tag> entry : blob.layers().entrySet()) {
            layersTag.put(entry.getKey(), entry.getValue().copy());
        }
        root.put("layers", layersTag);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream(512);
        Deflater deflater = new Deflater(DEFLATE_LEVEL);
        try (DeflaterOutputStream dos = new DeflaterOutputStream(compressed, deflater);
             DataOutputStream dataOut = new DataOutputStream(dos)) {
            NbtIo.write(root, dataOut);
        } finally {
            deflater.end();
        }
        return compressed.toByteArray();
    }

    private static RegionBlob decodeBlob(byte[] bytes, int offset, int length) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes, offset, length);
             InflaterInputStream iis = new InflaterInputStream(bais);
             DataInputStream dataIn = new DataInputStream(iis)) {
            CompoundTag root = NbtIo.read(dataIn, NbtAccounter.unlimitedHeap());
            int version = root.getIntOr("version", FORMAT_VERSION);
            CompoundTag layersTag = root.getCompoundOrEmpty("layers");
            Map<String, Tag> map = new LinkedHashMap<>();
            for (String key : layersTag.keySet()) {
                Tag value = layersTag.get(key);
                if (value != null) {
                    map.put(key, value.copy());
                }
            }
            return new RegionBlob(version, map);
        }
    }

    public static int computeCrc32(byte[] data, int offset, int length) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, offset, length);
        return (int) crc32.getValue();
    }

    private static void isolateCorruptFile(
            Path file,
            long currentTick,
            String reason,
            BiConsumer<Path, String> callback
    ) {
        Path quarantine = file.resolveSibling(file.getFileName().toString() + ".corrupt." + Math.max(0L, currentTick));
        try {
            if (Files.exists(file)) {
                Files.move(file, quarantine, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException moveError) {
            LOGGER.error("Failed to quarantine corrupt region file {}: {}", file, moveError.getMessage());
        }
        LOGGER.warn("Isolated corrupt region file {} -> {} ({})", file, quarantine, reason);
        if (callback != null) {
            callback.accept(quarantine, reason == null ? "corrupt" : reason);
        }
    }
}
