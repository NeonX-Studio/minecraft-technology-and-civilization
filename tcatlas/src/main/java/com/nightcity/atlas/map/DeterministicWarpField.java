package com.nightcity.atlas.map;

/**
 * Deterministic low-precision cartographic error warp field W(p) (22 §5.4).
 * Wavelength = 256 blocks (640 exact periods across 163,840-block wrapped planet width), |W| <= 1.
 */
public final class DeterministicWarpField {
    public static final double WAVELENGTH_BLOCKS = 256.0;
    public static final int PLANET_PERIOD_CELLS = PlanetProjection.WORLD_WIDTH / 256; // 640

    @FunctionalInterface
    public interface ChunkEpsilonLookup {
        double epsilonAtChunk(int chunkX, int chunkZ);
    }

    public record Vec2d(double x, double z) {
        public double length() {
            return Math.hypot(x, z);
        }
    }

    private final long seed;
    private final boolean wrapEastWest;

    public DeterministicWarpField(long worldSeed, long holderSalt, long sheetSalt, boolean wrapEastWest) {
        this.seed = mix64(worldSeed ^ Long.rotateLeft(holderSalt, 21) ^ Long.rotateLeft(sheetSalt, 43) ^ 0x517CC1B727220A95L);
        this.wrapEastWest = wrapEastWest;
    }

    public long seed() {
        return seed;
    }

    /**
     * Smooth 2D vector noise W(x, z) with |W| <= 1.0 and 256-block wavelength.
     */
    public Vec2d sampleUnitVector(double x, double z) {
        double nx = wrapEastWest ? (PlanetProjection.wrapX(x) + PlanetProjection.HALF_WIDTH) / WAVELENGTH_BLOCKS : x / WAVELENGTH_BLOCKS;
        double nz = z / WAVELENGTH_BLOCKS;

        int ix0 = (int) Math.floor(nx);
        int iz0 = (int) Math.floor(nz);
        double fx = nx - ix0;
        double fz = nz - iz0;

        double ux = quintic(fx);
        double uz = quintic(fz);

        int gx0 = wrapEastWest ? Math.floorMod(ix0, PLANET_PERIOD_CELLS) : ix0;
        int gx1 = wrapEastWest ? Math.floorMod(ix0 + 1, PLANET_PERIOD_CELLS) : (ix0 + 1);
        int gz0 = iz0;
        int gz1 = iz0 + 1;

        Vec2d v00 = latticeUnit(gx0, gz0);
        Vec2d v10 = latticeUnit(gx1, gz0);
        Vec2d v01 = latticeUnit(gx0, gz1);
        Vec2d v11 = latticeUnit(gx1, gz1);

        double wx0 = lerp(v00.x, v10.x, ux);
        double wx1 = lerp(v01.x, v11.x, ux);
        double wx = lerp(wx0, wx1, uz);

        double wz0 = lerp(v00.z, v10.z, ux);
        double wz1 = lerp(v01.z, v11.z, ux);
        double wz = lerp(wz0, wz1, uz);

        double len = Math.hypot(wx, wz);
        if (len > 1.0) {
            wx /= len;
            wz /= len;
        }
        return new Vec2d(wx, wz);
    }

    /**
     * Bilinearly interpolates chunk-center epsilon and applies smoothstep(2, 4, eps) so eps <= 2 has zero warp.
     */
    public double effectiveEpsilon(double x, double z, ChunkEpsilonLookup lookup) {
        double cx = (x - 8.0) / 16.0;
        double cz = (z - 8.0) / 16.0;
        int x0 = (int) Math.floor(cx);
        int z0 = (int) Math.floor(nzSafe(cz));
        double tx = cx - x0;
        double tz = cz - z0;

        int wx0 = wrapChunkX(x0);
        int wx1 = wrapChunkX(x0 + 1);

        double e00 = lookup.epsilonAtChunk(wx0, z0);
        double e10 = lookup.epsilonAtChunk(wx1, z0);
        double e01 = lookup.epsilonAtChunk(wx0, z0 + 1);
        double e11 = lookup.epsilonAtChunk(wx1, z0 + 1);

        double e0 = lerp(e00, e10, tx);
        double e1 = lerp(e01, e11, tx);
        double eps = Math.max(0.0, lerp(e0, e1, tz));
        return eps * smoothstep(2.0, 4.0, eps);
    }

    private static double nzSafe(double v) {
        return Math.floor(v);
    }

    /**
     * Forward warp p' = p + eps(p) * W(p) for displaying waypoints, player position, and vector features.
     */
    public Vec2d warpForward(double x, double z, ChunkEpsilonLookup lookup) {
        double effEps = effectiveEpsilon(x, z, lookup);
        if (effEps <= 1e-6) {
            return new Vec2d(wrapEastWest ? PlanetProjection.wrapX(x) : x, z);
        }
        Vec2d w = sampleUnitVector(x, z);
        double px = x + effEps * w.x;
        double pz = z + effEps * w.z;
        return new Vec2d(wrapEastWest ? PlanetProjection.wrapX(px) : px, pz);
    }

    /**
     * 2-iteration fixed-point inverse warp p ≈ q - eps(p) * W(p) for rasterizing distorted map tiles (22 §5.4).
     */
    public Vec2d warpInverse(double qx, double qz, ChunkEpsilonLookup lookup) {
        double px = qx;
        double pz = qz;
        for (int iter = 0; iter < 2; iter++) {
            double effEps = effectiveEpsilon(px, pz, lookup);
            if (effEps <= 1e-6) {
                px = qx;
                pz = qz;
                break;
            }
            Vec2d w = sampleUnitVector(px, pz);
            px = qx - effEps * w.x;
            pz = qz - effEps * w.z;
        }
        return new Vec2d(wrapEastWest ? PlanetProjection.wrapX(px) : px, pz);
    }

    private int wrapChunkX(int chunkX) {
        if (!wrapEastWest) {
            return chunkX;
        }
        int totalChunks = PlanetProjection.WORLD_WIDTH >> 4; // 10,240
        int halfChunks = PlanetProjection.HALF_WIDTH >> 4;   // 5,120
        return Math.floorMod(chunkX + halfChunks, totalChunks) - halfChunks;
    }

    private Vec2d latticeUnit(int gx, int gz) {
        long h = mix64(seed ^ ((long) gx * 0x9E3779B97F4A7C15L) ^ ((long) gz * 0xBF58476D1CE4E5B9L));
        double angle = ((h >>> 11) * 0x1.0p-53) * (Math.PI * 2.0);
        double mag = 0.35 + 0.65 * (((mix64(h) >>> 11) * 0x1.0p-53));
        return new Vec2d(Math.cos(angle) * mag, Math.sin(angle) * mag);
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double quintic(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
