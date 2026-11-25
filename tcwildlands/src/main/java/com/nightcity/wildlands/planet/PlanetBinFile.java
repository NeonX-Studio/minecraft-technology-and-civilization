package com.nightcity.wildlands.planet;

import com.nightcity.tcivilization.api.TcMigrators;
import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.DepositType;
import com.nightcity.tcivilization.api.world.Reservoir;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Deterministic big-endian TCPL (planet.bin) binary serializer, reader, and formatVersion=1 migrator (10 §11.2, §11.8).
 */
public final class PlanetBinFile {
    public static final int MAGIC = 0x54434C50; // "TCPL"
    public static final int CURRENT_FORMAT_VERSION = 1;
    public static final int GENERATOR_VERSION = 1;
    public static final int HEADER_BYTES = 256;
    public static final int DIR_ENTRY_BYTES = 48;
    public static final Identifier FILE_ID = Identifier.fromNamespaceAndPath("tcwildlands", "planet_bin");

    private static final byte CODEC_DEFLATE = 1;

    private PlanetBinFile() {
    }

    public static void registerMigrators() {
        // v0 -> v1 schema migration registration in core TcMigrators
        TcMigrators.register(FILE_ID, 0, (fromVer, tag) -> {
            if (!tag.contains("currentsRecomputed")) {
                tag.putBoolean("currentsRecomputed", true);
            }
            return tag;
        });
    }

    public static byte[] computeParamsDigest(long seed, double landTarget, int plateCount, int erosionSteps) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            ByteBuffer buf = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
            buf.putLong(seed);
            buf.putDouble(landTarget);
            buf.putInt(plateCount);
            buf.putInt(erosionSteps);
            buf.putInt(CURRENT_FORMAT_VERSION);
            buf.putInt(GENERATOR_VERSION);
            return sha.digest(buf.array());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private record NamedChunk(String name, byte cellType, byte[] raw, byte[] compressed, int crc32) {
    }

    public static byte[] serializeToBytes(PlanetModel model) {
        List<NamedChunk> chunks = new ArrayList<>(20);
        addByteLayer(chunks, "plate", model.rawPlate());
        addByteLayer(chunks, "crust", model.rawCrust());
        addShortLayer(chunks, "elevation", model.rawElevation());
        addByteLayer(chunks, "relief", model.rawRelief());
        addByteLayer(chunks, "sediment", model.rawSediment());
        addByteLayer(chunks, "flowDir", model.rawFlowDir());
        addShortLayer(chunks, "flowAcc", model.rawFlowAcc());
        addShortLayer(chunks, "basin", model.rawBasin());
        addShortLayer(chunks, "lake", model.rawLake());
        addByteLayer(chunks, "riverOrder", model.rawRiverOrder());
        addByteLayer(chunks, "province", model.rawProvince());
        addByteLayer(chunks, "rockTop", model.rawRockTop());
        addByteLayer(chunks, "groundwater", model.rawGroundwater());
        addByteLayer(chunks, "koppen", model.rawKoppen());
        addByteLayer(chunks, "biome", model.rawBiome());
        addByteLayer(chunks, "tempMonthly", model.rawTempMonthly());
        addByteLayer(chunks, "precipMonthly", model.rawPrecipMonthly());
        addByteLayer(chunks, "windSeasonal", model.rawWindSeasonal());
        addByteLayer(chunks, "currents", model.rawCurrents());
        addByteLayer(chunks, "objects", serializeObjects(model));

        int dirCount = chunks.size();
        long dataOffset = HEADER_BYTES + (long) dirCount * DIR_ENTRY_BYTES;
        long totalBytesLong = dataOffset + 8L; // footer: u32 fileCrc32 + u32 magic
        for (NamedChunk c : chunks) {
            totalBytesLong += c.compressed().length;
        }
        if (totalBytesLong > Integer.MAX_VALUE - 64) {
            throw new IllegalStateException("planet.bin exceeds array size: " + totalBytesLong);
        }

        ByteBuffer out = ByteBuffer.allocate((int) totalBytesLong).order(ByteOrder.BIG_ENDIAN);
        // Header (256 bytes)
        out.putInt(MAGIC);                                        // 0
        out.putShort((short) model.formatVersion());              // 4
        out.putShort((short) model.generatorVersion());           // 6
        out.putLong(model.worldSeed());                           // 8
        out.putInt(PlanetCoordinates.PLANET_WIDTH);               // 16
        out.putInt(PlanetCoordinates.PLANET_HEIGHT);              // 20
        out.putShort((short) PlanetModel.GRID_W);                 // 24
        out.putShort((short) PlanetModel.GRID_H);                 // 26
        out.putShort((short) PlanetModel.CLIM_W);                 // 28
        out.putShort((short) PlanetModel.CLIM_H);                 // 30
        out.put((byte) model.plateCount());                       // 32
        out.put((byte) model.continentCount());                   // 33
        out.put((byte) model.hotspotCount());                     // 34
        out.put((byte) 0);                                        // 35
        out.putFloat(model.oceanFraction());                      // 36
        out.putFloat((float) PlanetCoordinates.SEA_LEVEL);        // 40
        out.putFloat((float) PlanetCoordinates.METERS_PER_BLOCK); // 44
        out.putFloat((float) PlanetCoordinates.VERTICAL_GAMMA);   // 48
        out.putInt(dirCount - 1);                                 // 52 layerCount
        out.putInt(10);                                           // 56 objectTableCount
        byte[] pDigest = model.paramsDigest();
        out.put(Arrays.copyOf(pDigest, 32));                      // 60..91
        out.putLong(model.planetDigest());                        // 92..99
        out.position(224);
        int headerCrc = crc32(out.array(), 0, 224);
        out.putInt(headerCrc);                                    // 224..227
        out.position(HEADER_BYTES);

        // Directory
        long cursor = dataOffset;
        for (NamedChunk c : chunks) {
            byte[] nameBytes = Arrays.copyOf(c.name().getBytes(StandardCharsets.US_ASCII), 24);
            out.put(nameBytes);                                   // 0..23
            out.put(c.cellType());                                // 24
            out.put(CODEC_DEFLATE);                               // 25
            out.putShort((short) 0);                              // 26..27
            out.putLong(cursor);                                  // 28..35
            out.putInt(c.compressed().length);                    // 36..39
            out.putInt(c.raw().length);                           // 40..43
            out.putInt(c.crc32());                                // 44..47
            cursor += c.compressed().length;
        }

        for (NamedChunk c : chunks) {
            out.put(c.compressed());
        }

        int bodyLen = out.position();
        int fileCrc = crc32(out.array(), 0, bodyLen);
        out.putInt(fileCrc);
        out.putInt(MAGIC);
        return out.array();
    }

    public static void write(Path path, PlanetModel model) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        byte[] bytes = serializeToBytes(model);
        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        Files.write(tmp, bytes);
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static PlanetModel read(Path path) throws IOException {
        return deserializeFromBytes(Files.readAllBytes(path));
    }

    /**
     * Migrates legacy v0 planet.bin bytes (missing currents or older header version) to CURRENT_FORMAT_VERSION = 1.
     */
    public static byte[] migrateBytesIfNeeded(byte[] rawFileBytes) {
        ByteBuffer buf = ByteBuffer.wrap(rawFileBytes).order(ByteOrder.BIG_ENDIAN);
        if (buf.remaining() < HEADER_BYTES + 8 || buf.getInt(0) != MAGIC) {
            throw new IllegalArgumentException("Invalid TCPL magic in planet.bin");
        }
        int ver = Short.toUnsignedInt(buf.getShort(4));
        if (ver == CURRENT_FORMAT_VERSION) {
            return rawFileBytes;
        }
        if (ver > CURRENT_FORMAT_VERSION) {
            throw new IllegalStateException("Unsupported newer planet.bin formatVersion: " + ver + " > " + CURRENT_FORMAT_VERSION);
        }
        PlanetModel loaded = deserializeInternal(rawFileBytes, true);
        PlanetModel upgraded = new PlanetModel(
                CURRENT_FORMAT_VERSION,
                loaded.generatorVersion(),
                loaded.worldSeed(),
                loaded.planetDigest(),
                loaded.paramsDigest(),
                loaded.plateCount(),
                loaded.continentCount(),
                loaded.hotspotCount(),
                loaded.oceanFraction(),
                loaded.rawPlate(),
                loaded.rawCrust(),
                loaded.rawElevation(),
                loaded.rawRelief(),
                loaded.rawSediment(),
                loaded.rawFlowDir(),
                loaded.rawFlowAcc(),
                loaded.rawBasin(),
                loaded.rawLake(),
                loaded.rawRiverOrder(),
                loaded.rawProvince(),
                loaded.rawRockTop(),
                loaded.rawGroundwater(),
                loaded.rawKoppen(),
                loaded.rawBiome(),
                loaded.rawTempMonthly(),
                loaded.rawPrecipMonthly(),
                loaded.rawWindSeasonal(),
                loaded.rawCurrents(),
                loaded.plates(),
                loaded.hotspots(),
                loaded.volcanoes(),
                loaded.faults(),
                loaded.lakes(),
                loaded.deposits(),
                loaded.reservoirs(),
                loaded.deepConduits(),
                loaded.structures(),
                loaded.startSites(),
                loaded.continentStats()
        );
        return serializeToBytes(upgraded);
    }

    public static PlanetModel deserializeFromBytes(byte[] fileBytes) {
        byte[] migrated = migrateBytesIfNeeded(fileBytes);
        return deserializeInternal(migrated, false);
    }

    private static PlanetModel deserializeInternal(byte[] fileBytes, boolean allowLegacyVersion) {
        if (fileBytes.length < HEADER_BYTES + 8) {
            throw new IllegalArgumentException("planet.bin truncated: " + fileBytes.length);
        }
        ByteBuffer in = ByteBuffer.wrap(fileBytes).order(ByteOrder.BIG_ENDIAN);
        if (in.getInt(0) != MAGIC) {
            throw new IllegalArgumentException("Invalid header magic");
        }
        int footerPos = fileBytes.length - 8;
        int expectedFileCrc = in.getInt(footerPos);
        int footerMagic = in.getInt(footerPos + 4);
        if (footerMagic != MAGIC) {
            throw new IllegalArgumentException("Invalid footer magic");
        }
        int actualFileCrc = crc32(fileBytes, 0, footerPos);
        if (expectedFileCrc != actualFileCrc) {
            throw new IllegalArgumentException("planet.bin file CRC mismatch");
        }
        int expectedHeaderCrc = in.getInt(224);
        if (expectedHeaderCrc != crc32(fileBytes, 0, 224)) {
            throw new IllegalArgumentException("planet.bin header CRC mismatch");
        }

        int formatVer = Short.toUnsignedInt(in.getShort(4));
        if (!allowLegacyVersion && formatVer != CURRENT_FORMAT_VERSION) {
            throw new IllegalStateException("Unexpected formatVersion: " + formatVer);
        }
        int genVer = Short.toUnsignedInt(in.getShort(6));
        long seed = in.getLong(8);
        int plateCount = Byte.toUnsignedInt(in.get(32));
        int continentCount = Byte.toUnsignedInt(in.get(33));
        int hotspotCount = Byte.toUnsignedInt(in.get(34));
        float oceanFraction = in.getFloat(36);
        int layerCount = in.getInt(52);
        byte[] pDigest = Arrays.copyOfRange(fileBytes, 60, 92);
        long planetDigest = in.getLong(92);

        int totalChunks = layerCount + 1;
        Map<String, byte[]> rawMap = new LinkedHashMap<>(totalChunks);
        in.position(HEADER_BYTES);
        for (int i = 0; i < totalChunks; i++) {
            byte[] nb = new byte[24];
            in.get(nb);
            int nul = 0;
            while (nul < 24 && nb[nul] != 0) {
                nul++;
            }
            String name = new String(nb, 0, nul, StandardCharsets.US_ASCII);
            in.get(); // cellType
            byte codec = in.get();
            in.getShort(); // flags
            long offset = in.getLong();
            int compSize = in.getInt();
            int rawSize = in.getInt();
            int entryCrc = in.getInt();

            byte[] comp = Arrays.copyOfRange(fileBytes, (int) offset, (int) offset + compSize);
            if (crc32(comp, 0, comp.length) != entryCrc) {
                throw new IllegalArgumentException("Layer CRC mismatch for " + name);
            }
            byte[] raw = codec == CODEC_DEFLATE ? inflate(comp, rawSize) : comp;
            rawMap.put(name, raw);
        }

        int l0Len = PlanetModel.GRID_W * PlanetModel.GRID_H;
        int climLen = PlanetModel.CLIM_W * PlanetModel.CLIM_H;
        byte[] currents = rawMap.getOrDefault("currents", new byte[climLen * 2]);
        byte[] windSeasonal = rawMap.get("windSeasonal");
        if (!rawMap.containsKey("currents") && windSeasonal != null) {
            // Recompute surface ocean currents from seasonal wind field (10 §11.8)
            currents = new byte[climLen * 2];
            for (int i = 0; i < climLen; i++) {
                int u = (windSeasonal[i * 8] + windSeasonal[i * 8 + 2] + windSeasonal[i * 8 + 4] + windSeasonal[i * 8 + 6]) / 4;
                int v = (windSeasonal[i * 8 + 1] + windSeasonal[i * 8 + 3] + windSeasonal[i * 8 + 5] + windSeasonal[i * 8 + 7]) / 4;
                currents[i * 2] = (byte) Math.clamp(u / 3, -127, 127);
                currents[i * 2 + 1] = (byte) Math.clamp(v / 3, -127, 127);
            }
        }

        DeserializedObjects objs = deserializeObjects(rawMap.get("objects"));
        return new PlanetModel(
                formatVer,
                genVer,
                seed,
                planetDigest,
                pDigest,
                plateCount,
                continentCount,
                hotspotCount,
                oceanFraction,
                rawMap.get("plate"),
                rawMap.get("crust"),
                bytesToShorts(rawMap.get("elevation"), l0Len),
                rawMap.get("relief"),
                rawMap.get("sediment"),
                rawMap.get("flowDir"),
                bytesToShorts(rawMap.get("flowAcc"), l0Len),
                bytesToShorts(rawMap.get("basin"), l0Len),
                bytesToShorts(rawMap.get("lake"), l0Len),
                rawMap.get("riverOrder"),
                rawMap.get("province"),
                rawMap.get("rockTop"),
                rawMap.get("groundwater"),
                rawMap.get("koppen"),
                rawMap.get("biome"),
                rawMap.get("tempMonthly"),
                rawMap.get("precipMonthly"),
                windSeasonal,
                currents,
                objs.plates,
                objs.hotspots,
                objs.volcanoes,
                objs.faults,
                objs.lakes,
                objs.deposits,
                objs.reservoirs,
                objs.deepConduits,
                objs.structures,
                objs.startSites,
                objs.continentStats
        );
    }

    private record DeserializedObjects(
            List<PlanetModel.PlateInfo> plates,
            List<PlanetModel.HotspotInfo> hotspots,
            List<PlanetModel.VolcanoInfo> volcanoes,
            List<PlanetModel.FaultInfo> faults,
            List<PlanetModel.LakeInfo> lakes,
            List<Deposit> deposits,
            List<Reservoir> reservoirs,
            List<PlanetModel.DeepConduitInfo> deepConduits,
            List<PlanetModel.StructureSiteInfo> structures,
            List<PlanetModel.StartSiteCandidate> startSites,
            List<PlanetModel.ContinentStat> continentStats
    ) {
    }

    private static byte[] serializeObjects(PlanetModel m) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1_048_576);
        ByteBuffer tmp = ByteBuffer.allocate(256).order(ByteOrder.BIG_ENDIAN);

        writeInt(baos, tmp, m.plates().size());
        for (PlanetModel.PlateInfo p : m.plates()) {
            tmp.clear();
            tmp.putInt(p.id());
            tmp.put((byte) (p.continental() ? 1 : 0));
            tmp.putFloat(p.centerX()).putFloat(p.centerY()).putFloat(p.centerZ());
            tmp.putFloat(p.poleX()).putFloat(p.poleY()).putFloat(p.poleZ());
            tmp.putFloat(p.omega()).putFloat(p.weight());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.hotspots().size());
        for (PlanetModel.HotspotInfo h : m.hotspots()) {
            tmp.clear();
            tmp.putInt(h.id()).putInt(h.blockX()).putInt(h.blockZ()).putFloat(h.strength());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.volcanoes().size());
        for (PlanetModel.VolcanoInfo v : m.volcanoes()) {
            tmp.clear();
            tmp.putInt(v.id()).putInt(v.blockX()).putInt(v.blockZ()).put(v.kind()).put(v.maxVei()).putInt(v.recurrenceDays());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.faults().size());
        for (PlanetModel.FaultInfo f : m.faults()) {
            tmp.clear();
            tmp.putInt(f.id()).putInt(f.x1()).putInt(f.z1()).putInt(f.x2()).putInt(f.z2())
                    .put(f.boundaryKind()).putFloat(f.maxMagnitude()).putFloat(f.loadingRate());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.lakes().size());
        for (PlanetModel.LakeInfo l : m.lakes()) {
            tmp.clear();
            tmp.putInt(l.id()).putInt(l.centerX()).putInt(l.centerZ()).putShort(l.surfaceMeters())
                    .putInt(l.areaCells()).put((byte) (l.endorheic() ? 1 : 0)).putFloat(l.salinity());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.deposits().size());
        for (Deposit d : m.deposits()) {
            int minIdx = findMineralIndex(d.mineral());
            int hostIdx = findRockIndex(d.hostRock());
            int signMask = 0;
            for (Deposit.Sign s : d.surfaceSigns()) {
                signMask |= (1 << s.ordinal());
            }
            tmp.clear();
            tmp.putLong(d.id());
            tmp.put((byte) d.type().ordinal());
            tmp.put((byte) minIdx);
            tmp.put((byte) hostIdx);
            tmp.putInt(d.center().getX()).putInt(d.center().getY()).putInt(d.center().getZ());
            tmp.putShort((short) d.radiusXZ()).putShort((short) d.radiusY());
            tmp.putFloat(d.strikeDeg()).putFloat(d.dipDeg());
            tmp.putFloat(d.gradeMean()).putLong(d.tonnage());
            tmp.putInt(signMask);
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.reservoirs().size());
        for (Reservoir r : m.reservoirs()) {
            tmp.clear();
            tmp.putLong(r.id()).put((byte) r.kind().ordinal())
                    .putInt(r.center().getX()).putInt(r.center().getY()).putInt(r.center().getZ())
                    .putInt(r.radiusXZ()).putInt(r.topY()).putInt(r.bottomY())
                    .putLong(r.initialCapacityMb()).putFloat(r.pressureBar());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.deepConduits().size());
        for (PlanetModel.DeepConduitInfo dc : m.deepConduits()) {
            tmp.clear();
            tmp.putInt(dc.id()).putInt(dc.blockX()).putInt(dc.blockZ()).putInt(dc.thresholdY()).putFloat(dc.heatFlowMw());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.structures().size());
        for (PlanetModel.StructureSiteInfo st : m.structures()) {
            tmp.clear();
            tmp.putInt(st.id()).put(st.structureIndex()).putInt(st.blockX()).putInt(st.blockZ());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.startSites().size());
        for (PlanetModel.StartSiteCandidate sc : m.startSites()) {
            byte[] kBytes = sc.koppen().getBytes(StandardCharsets.UTF_8);
            int bIdx = WildlandsCatalog.BIOME_BY_ID.getOrDefault(sc.biome(), WildlandsCatalog.BIOMES.get(43)).index();
            int rIdx = findRockIndex(sc.primaryRock());
            tmp.clear();
            tmp.putInt(sc.index()).putInt(sc.blockX()).putInt(sc.blockZ()).putInt(sc.surfaceY()).putInt(sc.score());
            tmp.put((byte) kBytes.length).put(kBytes);
            tmp.putShort((short) bIdx).putShort((short) rIdx).putInt(sc.waterDistanceBlocks());
            baos.write(tmp.array(), 0, tmp.position());
        }

        writeInt(baos, tmp, m.continentStats().size());
        for (PlanetModel.ContinentStat cs : m.continentStats()) {
            tmp.clear();
            tmp.putInt(cs.id()).putInt(cs.cellCount()).putInt(cs.spanXBlocks()).putInt(cs.spanZBlocks())
                    .putInt(cs.maxSpanBlocks()).put((byte) (cs.touchesMidLatitude() ? 1 : 0));
            baos.write(tmp.array(), 0, tmp.position());
        }

        return baos.toByteArray();
    }

    private static DeserializedObjects deserializeObjects(byte[] bytes) {
        ByteBuffer in = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        int nPlates = in.getInt();
        List<PlanetModel.PlateInfo> plates = new ArrayList<>(nPlates);
        for (int i = 0; i < nPlates; i++) {
            plates.add(new PlanetModel.PlateInfo(
                    in.getInt(), in.get() != 0,
                    in.getFloat(), in.getFloat(), in.getFloat(),
                    in.getFloat(), in.getFloat(), in.getFloat(),
                    in.getFloat(), in.getFloat()
            ));
        }

        int nHot = in.getInt();
        List<PlanetModel.HotspotInfo> hotspots = new ArrayList<>(nHot);
        for (int i = 0; i < nHot; i++) {
            hotspots.add(new PlanetModel.HotspotInfo(in.getInt(), in.getInt(), in.getInt(), in.getFloat()));
        }

        int nVol = in.getInt();
        List<PlanetModel.VolcanoInfo> volcanoes = new ArrayList<>(nVol);
        for (int i = 0; i < nVol; i++) {
            volcanoes.add(new PlanetModel.VolcanoInfo(in.getInt(), in.getInt(), in.getInt(), in.get(), in.get(), in.getInt()));
        }

        int nFaults = in.getInt();
        List<PlanetModel.FaultInfo> faults = new ArrayList<>(nFaults);
        for (int i = 0; i < nFaults; i++) {
            faults.add(new PlanetModel.FaultInfo(in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get(), in.getFloat(), in.getFloat()));
        }

        int nLakes = in.getInt();
        List<PlanetModel.LakeInfo> lakes = new ArrayList<>(nLakes);
        for (int i = 0; i < nLakes; i++) {
            lakes.add(new PlanetModel.LakeInfo(in.getInt(), in.getInt(), in.getInt(), in.getShort(), in.getInt(), in.get() != 0, in.getFloat()));
        }

        int nDep = in.getInt();
        List<Deposit> deposits = new ArrayList<>(nDep);
        DepositType[] dTypes = DepositType.values();
        Deposit.Sign[] allSigns = Deposit.Sign.values();
        for (int i = 0; i < nDep; i++) {
            long id = in.getLong();
            DepositType dt = dTypes[Math.clamp(Byte.toUnsignedInt(in.get()), 0, dTypes.length - 1)];
            int minIdx = Math.clamp(Byte.toUnsignedInt(in.get()), 0, WildlandsCatalog.MINERALS.size() - 1);
            int hostIdx = Math.clamp(Byte.toUnsignedInt(in.get()), 0, WildlandsCatalog.ROCKS.size() - 1);
            BlockPos center = new BlockPos(in.getInt(), in.getInt(), in.getInt());
            int rxz = Short.toUnsignedInt(in.getShort());
            int ry = Short.toUnsignedInt(in.getShort());
            float strike = in.getFloat();
            float dip = in.getFloat();
            float gradeMean = in.getFloat();
            long tonnage = in.getLong();
            int signMask = in.getInt();
            EnumSet<Deposit.Sign> signs = EnumSet.noneOf(Deposit.Sign.class);
            for (Deposit.Sign s : allSigns) {
                if ((signMask & (1 << s.ordinal())) != 0) {
                    signs.add(s);
                }
            }
            deposits.add(new Deposit(
                    id, dt,
                    WildlandsCatalog.MINERALS.get(minIdx).id(),
                    List.of(),
                    center, rxz, ry, strike, dip,
                    WildlandsCatalog.ROCKS.get(hostIdx).id(),
                    gradeMean, tonnage, false, signs
            ));
        }

        int nRes = in.getInt();
        List<Reservoir> reservoirs = new ArrayList<>(nRes);
        Reservoir.Kind[] rKinds = Reservoir.Kind.values();
        for (int i = 0; i < nRes; i++) {
            long id = in.getLong();
            Reservoir.Kind kind = rKinds[Math.clamp(Byte.toUnsignedInt(in.get()), 0, rKinds.length - 1)];
            BlockPos center = new BlockPos(in.getInt(), in.getInt(), in.getInt());
            int rxz = in.getInt();
            int topY = in.getInt();
            int botY = in.getInt();
            long cap = in.getLong();
            float bar = in.getFloat();
            reservoirs.add(new Reservoir(id, kind, center, rxz, topY, botY, cap, cap, bar));
        }

        int nDc = in.getInt();
        List<PlanetModel.DeepConduitInfo> dcs = new ArrayList<>(nDc);
        for (int i = 0; i < nDc; i++) {
            dcs.add(new PlanetModel.DeepConduitInfo(in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getFloat()));
        }

        int nSt = in.getInt();
        List<PlanetModel.StructureSiteInfo> sts = new ArrayList<>(nSt);
        for (int i = 0; i < nSt; i++) {
            sts.add(new PlanetModel.StructureSiteInfo(in.getInt(), in.get(), in.getInt(), in.getInt()));
        }

        int nSc = in.getInt();
        List<PlanetModel.StartSiteCandidate> scs = new ArrayList<>(nSc);
        for (int i = 0; i < nSc; i++) {
            int idx = in.getInt();
            int bx = in.getInt();
            int bz = in.getInt();
            int sy = in.getInt();
            int score = in.getInt();
            int kLen = Byte.toUnsignedInt(in.get());
            byte[] kb = new byte[kLen];
            in.get(kb);
            String koppen = new String(kb, StandardCharsets.UTF_8);
            int bIdx = Short.toUnsignedInt(in.getShort());
            int rIdx = Short.toUnsignedInt(in.getShort());
            int waterDist = in.getInt();
            scs.add(new PlanetModel.StartSiteCandidate(
                    idx, bx, bz, sy, score, koppen,
                    WildlandsCatalog.biomeByIndex(bIdx).id(),
                    WildlandsCatalog.rockByPalette(rIdx).id(),
                    waterDist
            ));
        }

        List<PlanetModel.ContinentStat> cStats = new ArrayList<>();
        if (in.hasRemaining()) {
            int nCs = in.getInt();
            for (int i = 0; i < nCs; i++) {
                cStats.add(new PlanetModel.ContinentStat(in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.get() != 0));
            }
        }
        return new DeserializedObjects(plates, hotspots, volcanoes, faults, lakes, deposits, reservoirs, dcs, sts, scs, cStats);
    }

    private static int findMineralIndex(Identifier minId) {
        String key = minId.getPath().startsWith("raw_") ? minId.getPath().substring(4) : minId.getPath();
        for (int i = 0; i < WildlandsCatalog.MINERALS.size(); i++) {
            if (WildlandsCatalog.MINERALS.get(i).key().equals(key)) {
                return i;
            }
        }
        return 0;
    }

    private static int findRockIndex(Identifier rockId) {
        WildlandsCatalog.RockSpec spec = WildlandsCatalog.ROCK_BY_ID.get(rockId);
        return spec != null ? spec.paletteIndex() : 0;
    }

    private static void writeInt(ByteArrayOutputStream baos, ByteBuffer tmp, int val) {
        tmp.clear();
        tmp.putInt(val);
        baos.write(tmp.array(), 0, 4);
    }

    private static void addByteLayer(List<NamedChunk> out, String name, byte[] raw) {
        byte[] comp = deflate(raw);
        out.add(new NamedChunk(name, (byte) 1, raw, comp, crc32(comp, 0, comp.length)));
    }

    private static void addShortLayer(List<NamedChunk> out, String name, short[] values) {
        byte[] raw = shortsToBytes(values);
        byte[] comp = deflate(raw);
        out.add(new NamedChunk(name, (byte) 2, raw, comp, crc32(comp, 0, comp.length)));
    }

    private static byte[] shortsToBytes(short[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 2).order(ByteOrder.BIG_ENDIAN);
        buf.asShortBuffer().put(values);
        return buf.array();
    }

    private static short[] bytesToShorts(byte[] raw, int count) {
        short[] out = new short[count];
        ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(out);
        return out;
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(6);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(256, raw.length / 3));
            byte[] buf = new byte[16_384];
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                if (n > 0) {
                    baos.write(buf, 0, n);
                }
            }
            return baos.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] inflate(byte[] compressed, int rawSize) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            byte[] out = new byte[rawSize];
            int offset = 0;
            while (offset < rawSize && !inflater.finished()) {
                int n = inflater.inflate(out, offset, rawSize - offset);
                if (n == 0 && inflater.needsInput()) {
                    break;
                }
                offset += n;
            }
            return out;
        } catch (DataFormatException ex) {
            throw new IllegalStateException("Corrupt deflate stream in planet.bin", ex);
        } finally {
            inflater.end();
        }
    }

    public static int crc32(byte[] bytes, int offset, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, length);
        return (int) crc.getValue();
    }
}
