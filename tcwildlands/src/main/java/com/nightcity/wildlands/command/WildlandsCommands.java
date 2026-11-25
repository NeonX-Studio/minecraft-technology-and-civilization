package com.nightcity.wildlands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.world.ClimateNormals;
import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.GroundwaterInfo;
import com.nightcity.tcivilization.api.world.LatLon;
import com.nightcity.tcivilization.api.world.LocalSeason;
import com.nightcity.tcivilization.api.world.PlanetApi;
import com.nightcity.tcivilization.api.world.PlanetSummary;
import com.nightcity.tcivilization.api.world.PrecipType;
import com.nightcity.tcivilization.api.world.TcWorldApis;
import com.nightcity.wildlands.planet.PlanetBinFile;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.PlanetRuntime;
import com.nightcity.wildlands.weather.RegionalWeatherEngine;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/**
 * Implements /tcwildlands status | here | climate | weather | season | locate (10 §9.6, §13.4).
 */
public final class WildlandsCommands {
    private WildlandsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tcwildlands")
                .then(Commands.literal("status").executes(ctx -> sendStatus(ctx.getSource())))
                .then(Commands.literal("here").executes(ctx -> sendHere(ctx.getSource())))
                .then(Commands.literal("climate").executes(ctx -> {
                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
                    return sendClimate(ctx.getSource(), pos.getX(), pos.getZ());
                }).then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> sendClimate(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "x"),
                                        IntegerArgumentType.getInteger(ctx, "z"))))))
                .then(Commands.literal("weather")
                        .executes(ctx -> sendWeatherQuery(ctx.getSource()))
                        .then(Commands.literal("set")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(20, 2_400_000))
                                                .executes(ctx -> setWeather(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "type"),
                                                        IntegerArgumentType.getInteger(ctx, "ticks"),
                                                        false
                                                ))
                                                .then(Commands.literal("global")
                                                        .executes(ctx -> setWeather(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "type"),
                                                                IntegerArgumentType.getInteger(ctx, "ticks"),
                                                                true
                                                        )))))))
                .then(Commands.literal("season")
                        .executes(ctx -> sendSeasonQuery(ctx.getSource()))
                        .then(Commands.literal("set")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("doy", IntegerArgumentType.integer(0, 47))
                                        .executes(ctx -> setSeasonDoy(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "doy"))))))
                .then(Commands.literal("locate")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(ctx -> locateObject(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "kind"),
                                                StringArgumentType.getString(ctx, "id")
                                        ))))));
    }

    private static int sendStatus(CommandSourceStack source) {
        PlanetApi planet = TcWorldApis.planet();
        boolean active = planet.isPlanetActive();
        PlanetSummary sum = planet.summary();
        PlanetModel m = PlanetRuntime.model();
        int ver = m != null ? m.formatVersion() : PlanetBinFile.CURRENT_FORMAT_VERSION;
        long digest = m != null ? m.planetDigest() : 0L;

        source.sendSuccess(() -> Component.translatable("command.tcwildlands.status"), false);
        source.sendSuccess(() -> Component.translatable(
                "command.tcwildlands.status.planet",
                active,
                PlanetRuntime.isDeepEarthActive(),
                ver,
                Long.toHexString(digest),
                sum.continentCount(),
                String.format(Locale.ROOT, "%.1f%%", sum.oceanFraction() * 100.0f),
                sum.maxElevationY(),
                String.format(Locale.ROOT, "%+.2f", RegionalWeatherEngine.globalDeltaT()),
                RegionalWeatherEngine.activeSystemCount()
        ), false);
        return 1;
    }

    private static int sendHere(CommandSourceStack source) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        PlanetApi planet = TcWorldApis.planet();
        LatLon ll = planet.latLon(pos.getX(), pos.getZ());
        double altMeters = planet.altitudeMeters(pos.getY());
        Identifier biome = planet.biomeAt(pos.getX(), pos.getZ());
        String koppen = TcWorldApis.climate().koppen(pos);
        String prov = TcWorldApis.geo().province(pos.getX(), pos.getZ()).name();
        Identifier rock = TcWorldApis.geo().rockAt(pos.getX(), pos.getY(), pos.getZ());
        GroundwaterInfo gw = TcWorldApis.hydro().groundwater(pos);
        ServerLevel overworld = source.getServer().overworld();
        long totalDays = overworld != null ? Math.floorDiv(RegionalWeatherEngine.effectiveDayTime(overworld.getOverworldClockTime()), CivCalendar.DAY_TICKS) : 0L;
        LocalSeason ls = TcWorldApis.seasons().season(source.getLevel(), pos, totalDays);

        source.sendSuccess(() -> Component.translatable(
                "command.tcwildlands.here.summary",
                String.format(Locale.ROOT, "%.2f", ll.latDeg()),
                String.format(Locale.ROOT, "%.2f", ll.lonDeg()),
                String.format(Locale.ROOT, "%.0f", altMeters),
                pos.getY(),
                biome.toString(),
                koppen,
                prov,
                rock.toString(),
                String.format(Locale.ROOT, "%.1f", gw.waterTableDepthMeters()),
                Component.translatable(ls.labelKey())
        ), false);
        return 1;
    }

    private static int sendClimate(CommandSourceStack source, int x, int z) {
        ClimateNormals normals = TcWorldApis.climate().normals(new BlockPos(x, 64, z));
        source.sendSuccess(() -> Component.translatable(
                "command.tcwildlands.climate.header",
                x, z, normals.koppen(),
                String.format(Locale.ROOT, "%.1f", normals.annualMeanC()),
                String.format(Locale.ROOT, "%.0f", normals.annualPrecipMm())
        ), false);
        return 1;
    }

    private static int sendWeatherQuery(CommandSourceStack source) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        var st = TcWorldApis.weather().current(source.getLevel(), pos);
        source.sendSuccess(() -> Component.translatable(
                "command.tcwildlands.weather.info",
                st.type().name(),
                String.format(Locale.ROOT, "%.1f", st.precipMmPerHour()),
                String.format(Locale.ROOT, "%.1f", st.windSpeed()),
                String.format(Locale.ROOT, "%.0f", st.windDirDeg())
        ), false);
        return 1;
    }

    private static int setWeather(CommandSourceStack source, String typeStr, int ticks, boolean global) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        String lower = typeStr.toLowerCase(Locale.ROOT);
        PrecipType pt = switch (lower) {
            case "clear", "none" -> PrecipType.NONE;
            case "snow" -> PrecipType.SNOW;
            case "hail" -> PrecipType.HAIL;
            default -> PrecipType.RAIN;
        };
        int conv = "thunder".equals(lower) ? 2 : 0;
        RegionalWeatherEngine.setWeatherOverride(pos, pt, conv, ticks, global);
        if (global && pt == PrecipType.NONE && source.getServer().overworld() != null) {
            source.getServer().overworld().resetWeatherCycle();
        }
        source.sendSuccess(() -> Component.translatable("command.tcwildlands.weather.set", pt.name(), ticks, global), true);
        return 1;
    }

    private static int sendSeasonQuery(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        long dayTime = RegionalWeatherEngine.effectiveDayTime(overworld != null ? overworld.getOverworldClockTime() : 0L);
        CivCalendar.CalendarDate cal = CivCalendar.fromDayTime(dayTime);
        BlockPos pos = BlockPos.containing(source.getPosition());
        LocalSeason ls = TcWorldApis.seasons().season(source.getLevel(), pos, cal.totalDays());
        source.sendSuccess(() -> Component.translatable(
                "command.tcwildlands.season.info",
                cal.year(),
                (int) Math.floorMod(cal.totalDays(), CivCalendar.YEAR_DAYS),
                Component.translatable(ls.labelKey())
        ), false);
        return 1;
    }

    private static int setSeasonDoy(CommandSourceStack source, int doy) {
        ServerLevel overworld = source.getServer().overworld();
        if (overworld != null) {
            RegionalWeatherEngine.setDayOfYearOverride(overworld.getOverworldClockTime(), doy);
        }
        source.sendSuccess(() -> Component.translatable("command.tcwildlands.season.set", doy), true);
        return 1;
    }

    private static int locateObject(CommandSourceStack source, String kind, String targetId) {
        PlanetModel m = PlanetRuntime.model();
        if (m == null) {
            source.sendFailure(Component.translatable("command.tcwildlands.locate.inactive"));
            return 0;
        }
        BlockPos origin = BlockPos.containing(source.getPosition());
        if ("deposit".equalsIgnoreCase(kind)) {
            Deposit best = null;
            double bestDist = Double.MAX_VALUE;
            for (Deposit d : m.deposits()) {
                if (d.mineral().toString().contains(targetId)) {
                    double dist = PlanetCoordinates.wrappedDistance(origin, d.center());
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = d;
                    }
                }
            }
            if (best != null) {
                BlockPos found = best.center();
                source.sendSuccess(() -> Component.translatable("command.tcwildlands.locate.found", targetId, found.getX(), found.getY(), found.getZ()), false);
                return 1;
            }
        } else if ("biome".equalsIgnoreCase(kind)) {
            int bestX = 0;
            int bestZ = 0;
            double bestDist = Double.MAX_VALUE;
            boolean hit = false;
            for (int gz = 0; gz < PlanetModel.GRID_H; gz += 4) {
                for (int gx = 0; gx < PlanetModel.GRID_W; gx += 4) {
                    int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP;
                    int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP;
                    if (m.biomeSpecAt(bx, bz).id().toString().contains(targetId)) {
                        double dist = Math.hypot(PlanetCoordinates.wrappedDx(origin.getX(), bx), bz - origin.getZ());
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestX = bx;
                            bestZ = bz;
                            hit = true;
                        }
                    }
                }
            }
            if (hit) {
                int fx = bestX;
                int fz = bestZ;
                int fy = m.sampleL1SurfaceY(fx, fz);
                source.sendSuccess(() -> Component.translatable("command.tcwildlands.locate.found", targetId, fx, fy, fz), false);
                return 1;
            }
        }
        source.sendFailure(Component.translatable("command.tcwildlands.locate.not_found", targetId));
        return 0;
    }
}
