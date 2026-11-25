package com.nightcity.wildlands.network;

import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.PlanetRuntime;
import com.nightcity.wildlands.planet.WildlandsCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jspecify.annotations.Nullable;

/**
 * All 15 Appendix A.7 CustomPacketPayloads registered under protocol version "3" (10 §12.3).
 */
public final class WildlandsPayloads {
    public static final String PROTOCOL_VERSION = "3";
    private static final Map<UUID, long[]> SUMMARY_RATE_LIMIT = new ConcurrentHashMap<>();

    @Nullable
    private static volatile Consumer<PlanetProgressPayload> clientProgressHandler;
    @Nullable
    private static volatile Consumer<PlanetPreviewPayload> clientPreviewHandler;
    @Nullable
    private static volatile Consumer<StartSiteOptionsPayload> clientStartSiteHandler;
    @Nullable
    private static volatile Consumer<LocalClimatePayload> clientLocalClimateHandler;
    @Nullable
    private static volatile Consumer<WeatherCellsPayload> clientWeatherCellsHandler;

    private WildlandsPayloads() {
    }

    public static void bindClientHandlers(
            Consumer<PlanetProgressPayload> onProgress,
            Consumer<PlanetPreviewPayload> onPreview,
            Consumer<StartSiteOptionsPayload> onStartSite,
            Consumer<LocalClimatePayload> onLocalClimate,
            Consumer<WeatherCellsPayload> onWeatherCells
    ) {
        clientProgressHandler = onProgress;
        clientPreviewHandler = onPreview;
        clientStartSiteHandler = onStartSite;
        clientLocalClimateHandler = onLocalClimate;
        clientWeatherCellsHandler = onWeatherCells;
    }

    // 1. tcwildlands:calendar_sync
    public record CalendarSyncPayload(int year, int dayOfYear, byte globalSeason, boolean planetMode, boolean deepEarthMode) implements CustomPacketPayload {
        public static final Type<CalendarSyncPayload> TYPE = new Type<>(WildlandsCatalog.wl("calendar_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CalendarSyncPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.year());
                    buf.writeVarInt(p.dayOfYear());
                    buf.writeByte(p.globalSeason());
                    buf.writeBoolean(p.planetMode());
                    buf.writeBoolean(p.deepEarthMode());
                },
                buf -> new CalendarSyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readBoolean(), buf.readBoolean())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 2. tcwildlands:weather_cells
    public record WeatherCellsPayload(int centerCellX, int centerCellZ, int radius, byte[] packedCells) implements CustomPacketPayload {
        public static final Type<WeatherCellsPayload> TYPE = new Type<>(WildlandsCatalog.wl("weather_cells"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WeatherCellsPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.centerCellX());
                    buf.writeVarInt(p.centerCellZ());
                    buf.writeByte(p.radius());
                    buf.writeByteArray(p.packedCells());
                },
                buf -> new WeatherCellsPayload(buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readByteArray(2048))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 3. tcwildlands:local_climate (exact §12.3 signature)
    public record LocalClimatePayload(
            short tempC10,
            byte precipType,
            short precipRate100,
            short windDirDeg,
            short windSpeed10,
            short altitudeM,
            int biomeIndex,
            byte hazardKind,
            byte hazardLevel,
            short hazardEtaMinutes
    ) implements CustomPacketPayload {
        public static final Type<LocalClimatePayload> TYPE = new Type<>(WildlandsCatalog.wl("local_climate"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LocalClimatePayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeShort(p.tempC10());
                    buf.writeByte(p.precipType());
                    buf.writeShort(p.precipRate100());
                    buf.writeShort(p.windDirDeg());
                    buf.writeShort(p.windSpeed10());
                    buf.writeShort(p.altitudeM());
                    buf.writeVarInt(p.biomeIndex());
                    buf.writeByte(p.hazardKind());
                    buf.writeByte(p.hazardLevel());
                    buf.writeShort(p.hazardEtaMinutes());
                },
                buf -> new LocalClimatePayload(
                        buf.readShort(), buf.readByte(), buf.readShort(), buf.readShort(), buf.readShort(),
                        buf.readShort(), buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readShort()
                )
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 4. tcwildlands:hazard_state
    public record HazardStatePayload(int activeCount, byte highestType, float maxMagnitude) implements CustomPacketPayload {
        public static final Type<HazardStatePayload> TYPE = new Type<>(WildlandsCatalog.wl("hazard_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HazardStatePayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeVarInt(p.activeCount()); buf.writeByte(p.highestType()); buf.writeFloat(p.maxMagnitude()); },
                buf -> new HazardStatePayload(buf.readVarInt(), buf.readByte(), buf.readFloat())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 5. tcwildlands:quake_shake
    public record QuakeShakePayload(float intensity, int durationTicks, float dirDeg) implements CustomPacketPayload {
        public static final Type<QuakeShakePayload> TYPE = new Type<>(WildlandsCatalog.wl("quake_shake"));
        public static final StreamCodec<RegistryFriendlyByteBuf, QuakeShakePayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeFloat(p.intensity()); buf.writeVarInt(p.durationTicks()); buf.writeFloat(p.dirDeg()); },
                buf -> new QuakeShakePayload(buf.readFloat(), buf.readVarInt(), buf.readFloat())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 6. tcwildlands:rock_tap_result
    public record RockTapResultPayload(Identifier rockId, boolean identified, String hintText) implements CustomPacketPayload {
        public static final Type<RockTapResultPayload> TYPE = new Type<>(WildlandsCatalog.wl("rock_tap_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RockTapResultPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeIdentifier(p.rockId()); buf.writeBoolean(p.identified()); buf.writeUtf(p.hintText(), 256); },
                buf -> new RockTapResultPayload(buf.readIdentifier(), buf.readBoolean(), buf.readUtf(256))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 7. tcwildlands:prospect_result
    public record ProspectResultPayload(byte methodOrdinal, int anomalyCount, String summaryLine) implements CustomPacketPayload {
        public static final Type<ProspectResultPayload> TYPE = new Type<>(WildlandsCatalog.wl("prospect_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ProspectResultPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeByte(p.methodOrdinal()); buf.writeVarInt(p.anomalyCount()); buf.writeUtf(p.summaryLine(), 512); },
                buf -> new ProspectResultPayload(buf.readByte(), buf.readVarInt(), buf.readUtf(512))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 8. tcwildlands:start_site_options
    public record StartSiteOptionsPayload(boolean canCustomize, List<PlanetModel.StartSiteCandidate> candidates) implements CustomPacketPayload {
        public static final Type<StartSiteOptionsPayload> TYPE = new Type<>(WildlandsCatalog.wl("start_site_options"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StartSiteOptionsPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.canCustomize());
                    buf.writeVarInt(p.candidates().size());
                    for (PlanetModel.StartSiteCandidate c : p.candidates()) {
                        buf.writeVarInt(c.index());
                        buf.writeInt(c.blockX());
                        buf.writeInt(c.blockZ());
                        buf.writeInt(c.surfaceY());
                        buf.writeVarInt(c.score());
                        buf.writeUtf(c.koppen(), 16);
                        buf.writeIdentifier(c.biome());
                        buf.writeIdentifier(c.primaryRock());
                        buf.writeVarInt(c.waterDistanceBlocks());
                    }
                },
                buf -> {
                    boolean custom = buf.readBoolean();
                    int n = Math.min(16, buf.readVarInt());
                    List<PlanetModel.StartSiteCandidate> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        list.add(new PlanetModel.StartSiteCandidate(
                                buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                                buf.readVarInt(), buf.readUtf(16), buf.readIdentifier(), buf.readIdentifier(), buf.readVarInt()
                        ));
                    }
                    return new StartSiteOptionsPayload(custom, list);
                }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 9. tcwildlands:start_site_choice (C -> S)
    public record StartSiteChoicePayload(byte mode, int candidateIndex, int customX, int customZ) implements CustomPacketPayload {
        public static final Type<StartSiteChoicePayload> TYPE = new Type<>(WildlandsCatalog.wl("start_site_choice"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StartSiteChoicePayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeByte(p.mode()); buf.writeVarInt(p.candidateIndex()); buf.writeInt(p.customX()); buf.writeInt(p.customZ()); },
                buf -> new StartSiteChoicePayload(buf.readByte(), buf.readVarInt(), buf.readInt(), buf.readInt())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 10. tcwildlands:planet_progress
    public record PlanetProgressPayload(int stageIndex, int totalStages, float overallFraction, String detailText) implements CustomPacketPayload {
        public static final Type<PlanetProgressPayload> TYPE = new Type<>(WildlandsCatalog.wl("planet_progress"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlanetProgressPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.stageIndex());
                    buf.writeVarInt(p.totalStages());
                    buf.writeFloat(p.overallFraction());
                    buf.writeUtf(p.detailText(), 128);
                },
                buf -> new PlanetProgressPayload(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readUtf(128))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 11. tcwildlands:planet_preview (256x128 palette image <= 40 KB)
    public record PlanetPreviewPayload(int width, int height, byte[] paletteIndices) implements CustomPacketPayload {
        public static final Type<PlanetPreviewPayload> TYPE = new Type<>(WildlandsCatalog.wl("planet_preview"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlanetPreviewPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeVarInt(p.width()); buf.writeVarInt(p.height()); buf.writeByteArray(p.paletteIndices()); },
                buf -> new PlanetPreviewPayload(buf.readVarInt(), buf.readVarInt(), buf.readByteArray(40_960))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 12. tcwildlands:region_summary_request (C -> S, rate limited 4 req/sec per AC-WLD-23)
    public record RegionSummaryRequestPayload(byte subPage, long regionKey) implements CustomPacketPayload {
        public static final Type<RegionSummaryRequestPayload> TYPE = new Type<>(WildlandsCatalog.wl("region_summary_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RegionSummaryRequestPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeByte(p.subPage()); buf.writeLong(p.regionKey()); },
                buf -> new RegionSummaryRequestPayload(buf.readByte(), buf.readLong())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 13. tcwildlands:region_summary (S -> C, <= 16 KB)
    public record RegionSummaryPayload(byte subPage, long regionKey, String koppen, Identifier biome, Identifier rock, int cityScore) implements CustomPacketPayload {
        public static final Type<RegionSummaryPayload> TYPE = new Type<>(WildlandsCatalog.wl("region_summary"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RegionSummaryPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeByte(p.subPage());
                    buf.writeLong(p.regionKey());
                    buf.writeUtf(p.koppen(), 16);
                    buf.writeIdentifier(p.biome());
                    buf.writeIdentifier(p.rock());
                    buf.writeVarInt(p.cityScore());
                },
                buf -> new RegionSummaryPayload(buf.readByte(), buf.readLong(), buf.readUtf(16), buf.readIdentifier(), buf.readIdentifier(), buf.readVarInt())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 14. tcwildlands:wrap_preload
    public record WrapPreloadPayload(int targetChunkX, int targetChunkZ) implements CustomPacketPayload {
        public static final Type<WrapPreloadPayload> TYPE = new Type<>(WildlandsCatalog.wl("wrap_preload"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WrapPreloadPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeInt(p.targetChunkX()); buf.writeInt(p.targetChunkZ()); },
                buf -> new WrapPreloadPayload(buf.readInt(), buf.readInt())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // 15. tcwildlands:flow_field
    public record FlowFieldPayload(int chunkX, int chunkZ, byte[] flowVectors32) implements CustomPacketPayload {
        public static final Type<FlowFieldPayload> TYPE = new Type<>(WildlandsCatalog.wl("flow_field"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FlowFieldPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeInt(p.chunkX()); buf.writeInt(p.chunkZ()); buf.writeByteArray(p.flowVectors32()); },
                buf -> new FlowFieldPayload(buf.readInt(), buf.readInt(), buf.readByteArray(64))
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static boolean allowRegionSummaryRequest(UUID playerUuid, long nowMillis) {
        long[] window = SUMMARY_RATE_LIMIT.computeIfAbsent(playerUuid, k -> new long[4]);
        synchronized (window) {
            int oldestIdx = 0;
            for (int i = 1; i < window.length; i++) {
                if (window[i] < window[oldestIdx]) {
                    oldestIdx = i;
                }
            }
            if (nowMillis - window[oldestIdx] < 1_000L) {
                return false; // 5th request within 1 second rejected
            }
            window[oldestIdx] = nowMillis;
            return true;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(PROTOCOL_VERSION);
        reg.playToClient(CalendarSyncPayload.TYPE, CalendarSyncPayload.STREAM_CODEC, (p, ctx) -> {});
        reg.playToClient(WeatherCellsPayload.TYPE, WeatherCellsPayload.STREAM_CODEC, (p, ctx) -> {
            Consumer<WeatherCellsPayload> h = clientWeatherCellsHandler;
            if (h != null) h.accept(p);
        });
        reg.playToClient(LocalClimatePayload.TYPE, LocalClimatePayload.STREAM_CODEC, (p, ctx) -> {
            Consumer<LocalClimatePayload> h = clientLocalClimateHandler;
            if (h != null) h.accept(p);
        });
        reg.playToClient(HazardStatePayload.TYPE, HazardStatePayload.STREAM_CODEC, (p, ctx) -> {});
        reg.playToClient(QuakeShakePayload.TYPE, QuakeShakePayload.STREAM_CODEC, (p, ctx) -> {});
        reg.playToClient(RockTapResultPayload.TYPE, RockTapResultPayload.STREAM_CODEC, (p, ctx) -> {});
        reg.playToClient(ProspectResultPayload.TYPE, ProspectResultPayload.STREAM_CODEC, (p, ctx) -> {});
        reg.playToClient(StartSiteOptionsPayload.TYPE, StartSiteOptionsPayload.STREAM_CODEC, (p, ctx) -> {
            Consumer<StartSiteOptionsPayload> h = clientStartSiteHandler;
            if (h != null) h.accept(p);
        });
        reg.playToClient(PlanetProgressPayload.TYPE, PlanetProgressPayload.STREAM_CODEC, (p, ctx) -> {
            Consumer<PlanetProgressPayload> h = clientProgressHandler;
            if (h != null) h.accept(p);
        });
        reg.playToClient(PlanetPreviewPayload.TYPE, PlanetPreviewPayload.STREAM_CODEC, (p, ctx) -> {
            Consumer<PlanetPreviewPayload> h = clientPreviewHandler;
            if (h != null) h.accept(p);
        });
        reg.playToClient(RegionSummaryPayload.TYPE, RegionSummaryPayload.STREAM_CODEC, (p, ctx) -> {});
        reg.playToClient(WrapPreloadPayload.TYPE, WrapPreloadPayload.STREAM_CODEC, (p, ctx) -> {});
        reg.playToClient(FlowFieldPayload.TYPE, FlowFieldPayload.STREAM_CODEC, (p, ctx) -> {});

        reg.playToServer(StartSiteChoicePayload.TYPE, StartSiteChoicePayload.STREAM_CODEC, WildlandsPayloads::handleStartSiteChoice);
        reg.playToServer(RegionSummaryRequestPayload.TYPE, RegionSummaryRequestPayload.STREAM_CODEC, WildlandsPayloads::handleRegionSummaryRequest);
    }

    private static void handleStartSiteChoice(StartSiteChoicePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            if (PlanetRuntime.startSite().isPresent()) {
                return;
            }
            var server = player.level().getServer();
            var chosenOpt = switch (payload.mode()) {
                case 1 -> {
                    int rnd = player.getRandom().nextInt(5);
                    yield PlanetRuntime.chooseCandidateStartSite(server, rnd, player.getUUID(), "random");
                }
                case 2 -> {
                    if (!net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(player.permissions())) {
                        yield PlanetRuntime.chooseCandidateStartSite(server, 0, player.getUUID(), "candidate");
                    }
                    yield PlanetRuntime.chooseCustomStartSite(server, payload.customX(), payload.customZ(), player.getUUID());
                }
                default -> PlanetRuntime.chooseCandidateStartSite(server, payload.candidateIndex(), player.getUUID(), "candidate");
            };
            chosenOpt.ifPresent(sel -> {
                for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                    online.teleportTo(server.overworld(), sel.chosenX() + 0.5, sel.chosenY() + 1.0, sel.chosenZ() + 0.5,
                            java.util.Set.of(), online.getYRot(), online.getXRot(), false);
                }
            });
        });
    }

    private static void handleRegionSummaryRequest(RegionSummaryRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!allowRegionSummaryRequest(player.getUUID(), System.currentTimeMillis())) {
                return;
            }
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return;
            }
            int bx = player.getBlockX();
            int bz = player.getBlockZ();
            String kop = m.climateNormalsAt(bx, bz).koppen();
            Identifier biome = m.biomeSpecAt(bx, bz).id();
            Identifier rock = m.strataAt(bx, bz).rockAtY(player.getBlockY());
            ctx.reply(new RegionSummaryPayload(payload.subPage(), payload.regionKey(), kop, biome, rock, 72));
        });
    }
}
