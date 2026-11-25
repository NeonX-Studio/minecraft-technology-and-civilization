package com.nightcity.atlas.map;

import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Compact 16x16 column terrain summary for map rendering (22 §5.13).
 * Raw layout: 256 columns * 4 bytes = 1,024 bytes (compressed ~200 bytes on disk).
 */
public final class ChunkTerrainSummary {
    public static final int COLUMNS = 256;
    public static final int RAW_BYTES = 1024;
    public static final int HEIGHT_BIAS = 512;

    private final int[] argbColumns;
    private final short[] metaColumns; // bits 0..10: (y + 512) clamped 0..2047, bit 11: water, bits 12..15: depthGrade

    public ChunkTerrainSummary(int[] argbColumns, short[] metaColumns) {
        if (argbColumns.length != COLUMNS || metaColumns.length != COLUMNS) {
            throw new IllegalArgumentException("ChunkTerrainSummary requires 256 columns");
        }
        this.argbColumns = argbColumns;
        this.metaColumns = metaColumns;
    }

    public int argb(int localX, int localZ) {
        return argbColumns[((localZ & 15) << 4) | (localX & 15)];
    }

    public int surfaceY(int localX, int localZ) {
        int meta = metaColumns[((localZ & 15) << 4) | (localX & 15)] & 0xFFFF;
        return (meta & 0x7FF) - HEIGHT_BIAS;
    }

    public boolean isWater(int localX, int localZ) {
        int meta = metaColumns[((localZ & 15) << 4) | (localX & 15)] & 0xFFFF;
        return (meta & 0x800) != 0;
    }

    public int waterDepthGrade(int localX, int localZ) {
        int meta = metaColumns[((localZ & 15) << 4) | (localX & 15)] & 0xFFFF;
        return (meta >>> 12) & 0xF;
    }

    public int averageArgb() {
        long r = 0, g = 0, b = 0;
        for (int c : argbColumns) {
            r += (c >>> 16) & 0xFF;
            g += (c >>> 8) & 0xFF;
            b += c & 0xFF;
        }
        return 0xFF000000 | ((int) (r >>> 8) << 16) | ((int) (g >>> 8) << 8) | (int) (b >>> 8);
    }

    public static short packMeta(int surfaceY, boolean water, int waterDepthBlocks) {
        int biasedY = Math.clamp(surfaceY + HEIGHT_BIAS, 0, 2047);
        int depthGrade = Math.clamp((waterDepthBlocks + 3) >> 2, 0, 15);
        int bits = biasedY | (water ? 0x800 : 0) | (depthGrade << 12);
        return (short) bits;
    }

    public static ChunkTerrainSummary fromLoadedChunk(Level level, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        int[] colors = new int[COLUMNS];
        short[] metas = new short[COLUMNS];
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                WildlandsTerrainBridge.TerrainSample s = WildlandsTerrainBridge.scanLoadedColumn(level, chunk, baseX + lx, baseZ + lz);
                int idx = (lz << 4) | lx;
                colors[idx] = s.baseArgb() | 0xFF000000;
                metas[idx] = packMeta(s.surfaceY(), s.water(), s.waterDepth());
            }
        }
        applyReliefShading(colors, metas);
        return new ChunkTerrainSummary(colors, metas);
    }

    public static ChunkTerrainSummary fromPreview(long worldSeed, int chunkX, int chunkZ, boolean wrapX) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int[] colors = new int[COLUMNS];
        short[] metas = new short[COLUMNS];
        int step = WildlandsTerrainBridge.isWildlandsLoaded() ? 4 : 8;
        for (int lz = 0; lz < 16; lz += step) {
            for (int lx = 0; lx < 16; lx += step) {
                int wx = wrapX ? PlanetProjection.wrapBlockX(baseX + lx) : (baseX + lx);
                int wz = baseZ + lz;
                WildlandsTerrainBridge.TerrainSample s = WildlandsTerrainBridge.sampleTerrain(null, worldSeed, wx, wz);
                short meta = packMeta(s.surfaceY(), s.water(), s.waterDepth());
                int col = s.baseArgb() | 0xFF000000;
                for (int dz = 0; dz < step && (lz + dz) < 16; dz++) {
                    for (int dx = 0; dx < step && (lx + dx) < 16; dx++) {
                        int idx = ((lz + dz) << 4) | (lx + dx);
                        colors[idx] = col;
                        metas[idx] = meta;
                    }
                }
            }
        }
        applyReliefShading(colors, metas);
        return new ChunkTerrainSummary(colors, metas);
    }

    private static void applyReliefShading(int[] colors, short[] metas) {
        for (int lz = 0; lz < 16; lz++) {
            int northZ = Math.max(0, lz - 1);
            for (int lx = 0; lx < 16; lx++) {
                int westX = Math.max(0, lx - 1);
                int idx = (lz << 4) | lx;
                if (((metas[idx] & 0xFFFF) & 0x800) != 0) {
                    continue;
                }
                int h = (metas[idx] & 0x7FF);
                int hN = (metas[(northZ << 4) | lx] & 0x7FF);
                int hW = (metas[(lz << 4) | westX] & 0x7FF);
                int slope = (h - hN) + (h - hW);
                if (slope != 0) {
                    int delta = Math.clamp(slope * 4, -28, 28);
                    int c = colors[idx];
                    int r = Math.clamp(((c >>> 16) & 0xFF) + delta, 0, 255);
                    int g = Math.clamp(((c >>> 8) & 0xFF) + delta, 0, 255);
                    int b = Math.clamp((c & 0xFF) + delta, 0, 255);
                    colors[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    /**
     * Encodes to 1,024 raw bytes matching 22 §5.13 (4 bytes/column: rgb565+shade4 u16 + meta u16).
     */
    public byte[] toRawBytes() {
        ByteBuffer buf = ByteBuffer.allocate(RAW_BYTES);
        for (int i = 0; i < COLUMNS; i++) {
            int c = argbColumns[i];
            int r5 = ((c >>> 19) & 0x1F);
            int g6 = ((c >>> 10) & 0x3F);
            int b5 = ((c >>> 3) & 0x1F);
            short rgb565 = (short) ((r5 << 11) | (g6 << 5) | b5);
            buf.putShort(rgb565);
            buf.putShort(metaColumns[i]);
        }
        return buf.array();
    }

    public static ChunkTerrainSummary fromRawBytes(byte[] raw) {
        if (raw.length != RAW_BYTES) {
            throw new IllegalArgumentException("Expected 1024 bytes, got " + raw.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(raw);
        int[] colors = new int[COLUMNS];
        short[] metas = new short[COLUMNS];
        for (int i = 0; i < COLUMNS; i++) {
            int rgb565 = buf.getShort() & 0xFFFF;
            int r = ((rgb565 >>> 11) & 0x1F) * 255 / 31;
            int g = ((rgb565 >>> 5) & 0x3F) * 255 / 63;
            int b = (rgb565 & 0x1F) * 255 / 31;
            colors[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            metas[i] = buf.getShort();
        }
        return new ChunkTerrainSummary(colors, metas);
    }

    public byte[] toCompressedBytes() {
        byte[] raw = toRawBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        Deflater deflater = new Deflater(3);
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater); DataOutputStream out = new DataOutputStream(dos)) {
            out.write(raw);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to compress ChunkTerrainSummary", ex);
        } finally {
            deflater.end();
        }
        return baos.toByteArray();
    }

    public static ChunkTerrainSummary fromCompressedBytes(byte[] compressed) throws IOException {
        try (DataInputStream in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(compressed)))) {
            byte[] raw = in.readNBytes(RAW_BYTES);
            return fromRawBytes(raw);
        }
    }
}
