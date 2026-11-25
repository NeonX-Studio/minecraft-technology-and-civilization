package com.nightcity.wildlands.worldgen;

import com.nightcity.wildlands.WildlandsConfig;
import com.nightcity.wildlands.network.WildlandsPayloads;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Implements DA-13 / VO-WG-08 east-west seamless wrap at x = ±81,920 with ±256-block mirror buffer,
 * TELEPORT_AT = ±82,048 root-vehicle + passenger hierarchy + velocity preservation, and polar Z pushback.
 */
public final class PlanetWrapManager {
    public static final int PLANET_WIDTH = PlanetCoordinates.PLANET_WIDTH;     // 163,840
    public static final int X_MAX = PlanetCoordinates.X_MAX;                   // 81,920
    public static final int X_MIN = PlanetCoordinates.X_MIN;                   // -81,920
    public static final int WRAP_BUFFER = PlanetCoordinates.WRAP_BUFFER;       // 256
    public static final int TELEPORT_AT = PlanetCoordinates.TELEPORT_AT;       // 82,048
    public static final double WORLD_BORDER_SIZE = (X_MAX + WRAP_BUFFER) * 2.0;// 164,352
    public static final int POLAR_Z_LIMIT = PlanetCoordinates.Z_MAX;           // 40,960

    private static final Set<UUID> PRELOAD_SENT = ConcurrentHashMap.newKeySet();

    /**
     * Pure, deterministic vehicle/mount/boat + passenger tree node for unit/GameTest verification
     * and state-preserving wrap calculations (EC-P1-2).
     */
    public static final class WrapEntityNode {
        private final String id;
        private double x;
        private double y;
        private double z;
        private Vec3 velocity;
        private float yRot;
        private float xRot;
        private WrapEntityNode vehicle;
        private final List<WrapEntityNode> passengers = new ArrayList<>();

        public WrapEntityNode(String id, double x, double y, double z, Vec3 velocity, float yRot, float xRot) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.velocity = velocity;
            this.yRot = yRot;
            this.xRot = xRot;
        }

        public void addPassenger(WrapEntityNode passenger) {
            passenger.vehicle = this;
            this.passengers.add(passenger);
        }

        public String id() { return id; }
        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public Vec3 velocity() { return velocity; }
        public float yRot() { return yRot; }
        public float xRot() { return xRot; }
        public WrapEntityNode vehicle() { return vehicle; }
        public List<WrapEntityNode> passengers() { return List.copyOf(passengers); }

        public WrapEntityNode rootVehicle() {
            WrapEntityNode cur = this;
            while (cur.vehicle != null) {
                cur = cur.vehicle;
            }
            return cur;
        }
    }

    private PlanetWrapManager() {
    }

    public static boolean needsEastWestTeleport(double x) {
        return x >= TELEPORT_AT || x < -TELEPORT_AT;
    }

    public static double computeWrappedTeleportX(double x) {
        if (x >= TELEPORT_AT) {
            return x - PLANET_WIDTH;
        }
        if (x < -TELEPORT_AT) {
            return x + PLANET_WIDTH;
        }
        return x;
    }

    /**
     * Applies seamless east-west wrap and polar Z pushback to a root vehicle and all nested passengers,
     * preserving exact velocity vectors, yaw/pitch rotations, relative offsets, and riding relationships.
     */
    public static boolean applyWrapToTree(WrapEntityNode anyNode) {
        WrapEntityNode root = anyNode.rootVehicle();
        boolean changed = false;

        if (needsEastWestTeleport(root.x)) {
            double targetRootX = computeWrappedTeleportX(root.x);
            double deltaX = targetRootX - root.x;
            shiftTreeX(root, deltaX);
            changed = true;
        }

        if (root.z > POLAR_Z_LIMIT || root.z < -POLAR_Z_LIMIT) {
            double clampedZ = Math.clamp(root.z, -POLAR_Z_LIMIT + 1.0, POLAR_Z_LIMIT - 1.0);
            double deltaZ = clampedZ - root.z;
            clampTreePolarZ(root, deltaZ);
            changed = true;
        }
        return changed;
    }

    private static void shiftTreeX(WrapEntityNode node, double deltaX) {
        Vec3 savedVel = node.velocity;
        float savedYaw = node.yRot;
        float savedPitch = node.xRot;
        node.x += deltaX;
        node.velocity = savedVel;
        node.yRot = savedYaw;
        node.xRot = savedPitch;
        for (WrapEntityNode p : node.passengers) {
            shiftTreeX(p, deltaX);
            p.vehicle = node;
        }
    }

    private static void clampTreePolarZ(WrapEntityNode node, double deltaZ) {
        node.z += deltaZ;
        double vz = node.velocity.z;
        if ((node.z > 0 && vz > 0) || (node.z < 0 && vz < 0)) {
            node.velocity = new Vec3(node.velocity.x, node.velocity.y, 0.0);
        }
        for (WrapEntityNode p : node.passengers) {
            clampTreePolarZ(p, deltaZ);
        }
    }

    /**
     * Initializes the overworld world border once to ±(81,920 + 256) = size 164,352 centered at (0, 0) (VO-WG-08).
     */
    public static void initializeWorldBorderIfDefault(ServerLevel overworld) {
        WorldBorder border = overworld.getWorldBorder();
        if (border.getSize() >= 59_999_900.0D) {
            border.setCenter(0.0D, 0.0D);
            border.setSize(WORLD_BORDER_SIZE);
        }
    }

    /**
     * Ticks live overworld entities for preload notifications, seamless wrap teleportation, and polar boundary pushback.
     */
    public static void tickOverworldEntity(ServerLevel level, Entity entity) {
        if (entity.isPassenger()) {
            return; // Handled atomically via root vehicle
        }

        double x = entity.getX();
        double z = entity.getZ();
        int preloadDist = WildlandsConfig.SERVER_SPEC.isLoaded() ? WildlandsConfig.WRAP_PRELOAD_DISTANCE.getAsInt() : 192;

        if (entity instanceof ServerPlayer player) {
            if (Math.abs(x) >= X_MAX - preloadDist) {
                if (PRELOAD_SENT.add(player.getUUID())) {
                    int targetChunkX = ((int) Math.floor(x > 0 ? x - PLANET_WIDTH : x + PLANET_WIDTH)) >> 4;
                    int targetChunkZ = ((int) Math.floor(z)) >> 4;
                    PacketDistributor.sendToPlayer(player, new WildlandsPayloads.WrapPreloadPayload(targetChunkX, targetChunkZ));
                }
            } else {
                PRELOAD_SENT.remove(player.getUUID());
            }
        }

        if (needsEastWestTeleport(x)) {
            teleportLiveVehicleTree(level, entity, computeWrappedTeleportX(x) - x);
        }

        if (Math.abs(z) > POLAR_Z_LIMIT) {
            double clampedZ = Math.clamp(z, -POLAR_Z_LIMIT + 1.0, POLAR_Z_LIMIT - 1.0);
            Vec3 vel = entity.getDeltaMovement();
            entity.teleportTo(level, entity.getX(), entity.getY(), clampedZ, Set.of(), entity.getYRot(), entity.getXRot(), false);
            entity.setDeltaMovement(vel.x, vel.y, 0.0);
            entity.syncVelocity = true;
            entity.needsSync = true;
        }
    }

    private static void teleportLiveVehicleTree(ServerLevel level, Entity root, double deltaX) {
        double destX = root.getX() + deltaX;
        ChunkPos destChunk = new ChunkPos(((int) Math.floor(destX)) >> 4, ((int) Math.floor(root.getZ())) >> 4);
        level.getChunk(destChunk.x(), destChunk.z());

        List<MotionEntry> tree = new ArrayList<>();
        collectLiveTree(root, null, root.getX(), tree);

        for (MotionEntry sm : tree) {
            Entity e = sm.entity();
            double nx = destX + sm.relX();
            e.teleportTo(level, nx, e.getY(), e.getZ(), Set.<Relative>of(), sm.yRot(), sm.xRot(), false);
            e.setDeltaMovement(sm.velocity());
            e.syncVelocity = true;
            e.needsSync = true;
        }
        for (MotionEntry sm : tree) {
            if (sm.parent() != null && sm.entity().getVehicle() != sm.parent()) {
                sm.entity().startRiding(sm.parent(), true, false);
                sm.entity().setDeltaMovement(sm.velocity());
            }
        }
    }

    private static void collectLiveTree(Entity cur, Entity parent, double rootX, List<MotionEntry> out) {
        out.add(new MotionEntry(cur, parent, cur.getDeltaMovement(), cur.getYRot(), cur.getXRot(), cur.getX() - rootX));
        for (Entity passenger : cur.getPassengers()) {
            collectLiveTree(passenger, cur, rootX, out);
        }
    }

    private record MotionEntry(Entity entity, Entity parent, Vec3 velocity, float yRot, float xRot, double relX) {
    }
}
