package com.nightcity.tcivilization.api;

import com.nightcity.tcivilization.CivilizationSavedData;
import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.TCivilization;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side notification dispatcher with category registration, 10s mergeKey coalescing,
 * per-player 50-item history, and rate limiting (03 §5.10.5, §12.1).
 */
public final class TcNotifications {
    public static final Identifier CATEGORY_SYSTEM = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "system");
    public static final Identifier CATEGORY_CIVILIZATION = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "civilization");
    public static final int MAX_PERSISTED_PER_PLAYER = 50;
    public static final int DEFAULT_INFO_PER_MINUTE = 12;

    public record CategorySpec(Identifier id, Component name, Identifier icon, Severity defaultMinimum) {}

    public record MergedEntry(Notification notification, int count, long lastTimestampMs) {}

    private static final Map<Identifier, CategorySpec> CATEGORIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<MergedEntry>> HISTORY = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<Long>> RATE_WINDOW_MS = new ConcurrentHashMap<>();

    static {
        registerCategory(
                CATEGORY_SYSTEM,
                Component.translatable("notify.category.tcivilization.system"),
                Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "icon/system"),
                Severity.WARNING
        );
        registerCategory(
                CATEGORY_CIVILIZATION,
                Component.translatable("notify.category.tcivilization.civilization"),
                Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "icon/civilization"),
                Severity.INFO
        );
    }

    private TcNotifications() {
    }

    public static void registerCategory(Identifier id, Component name, Identifier icon, Severity defaultMinimum) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(defaultMinimum, "defaultMinimum");
        CATEGORIES.putIfAbsent(id, new CategorySpec(id, name, icon, defaultMinimum));
    }

    public static Map<Identifier, CategorySpec> categories() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(CATEGORIES));
    }

    public static void send(ServerPlayer player, Notification notification) {
        if (player == null) {
            return;
        }
        recordForPlayer(player.getUUID(), notification, System.currentTimeMillis());
    }

    public static void send(ServerLevel level, UUID civId, Notification notification) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(civId, "civId");
        Objects.requireNonNull(notification, "notification");
        CivilizationSavedData data = CivilizationSavedData.get(level.getServer().overworld());
        long now = System.currentTimeMillis();
        data.civ(civId).ifPresent(civ -> {
            for (UUID memberId : civ.members().keySet()) {
                ServerPlayer online = level.getServer().getPlayerList().getPlayer(memberId);
                if (online != null || notification.severity().ordinal() >= Severity.WARNING.ordinal()) {
                    recordForPlayer(memberId, notification, now);
                }
            }
        });
    }

    public static void sendToRegion(ServerLevel level, RegionCell region, Notification notification) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(region, "region");
        long now = System.currentTimeMillis();
        for (ServerPlayer player : level.players()) {
            int rx = RegionCoordinates.regionX(player.getBlockX());
            int rz = RegionCoordinates.regionZ(player.getBlockZ());
            if (rx == region.x() && rz == region.z()) {
                recordForPlayer(player.getUUID(), notification, now);
            }
        }
    }

    public static synchronized boolean recordForPlayer(UUID playerId, Notification notification, long nowMs) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(notification, "notification");

        if (notification.severity() == Severity.INFO || notification.severity() == Severity.SUCCESS) {
            Deque<Long> window = RATE_WINDOW_MS.computeIfAbsent(playerId, k -> new ArrayDeque<>());
            while (!window.isEmpty() && nowMs - window.peekFirst() > 60_000L) {
                window.pollFirst();
            }
            if (window.size() >= DEFAULT_INFO_PER_MINUTE) {
                return false;
            }
            window.addLast(nowMs);
        }

        Deque<MergedEntry> deque = HISTORY.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        if (!notification.mergeKey().isEmpty() && !deque.isEmpty()) {
            MergedEntry last = deque.peekLast();
            if (last != null
                    && last.notification().mergeKey().equals(notification.mergeKey())
                    && (nowMs - last.lastTimestampMs()) <= 10_000L) {
                deque.pollLast();
                deque.addLast(new MergedEntry(notification, last.count() + 1, nowMs));
                return true;
            }
        }
        deque.addLast(new MergedEntry(notification, 1, nowMs));
        while (deque.size() > MAX_PERSISTED_PER_PLAYER) {
            deque.pollFirst();
        }
        return true;
    }

    public static synchronized List<MergedEntry> history(UUID playerId) {
        Deque<MergedEntry> deque = HISTORY.get(playerId);
        return deque == null ? List.of() : List.copyOf(deque);
    }

    public static synchronized void clearForTests() {
        HISTORY.clear();
        RATE_WINDOW_MS.clear();
    }
}
