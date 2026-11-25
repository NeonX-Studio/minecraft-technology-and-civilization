package com.nightcity.tcivilization.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nightcity.tcivilization.CivilizationSavedData;
import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.TcServerContext;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.CivMember;
import com.nightcity.tcivilization.api.CivStats;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.ImportanceFlag;
import com.nightcity.tcivilization.api.MemoryEntry;
import com.nightcity.tcivilization.api.MemoryQuery;
import com.nightcity.tcivilization.api.PersonName;
import com.nightcity.tcivilization.api.PersonRecord;
import com.nightcity.tcivilization.api.PersonSpec;
import com.nightcity.tcivilization.api.ProficiencyBridge;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.RegionLayer;
import com.nightcity.tcivilization.api.TcApi;
import com.nightcity.tcivilization.person.PersonEntity;
import com.nightcity.tcivilization.region.CivRegionData;
import com.nightcity.tcivilization.region.CivRegionLayer;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import com.nightcity.tcivilization.region.TcrRegionFile;
import com.nightcity.tcivilization.registry.TcRegistries;
import com.nightcity.tcivilization.sim.SimulationScheduler;
import com.nightcity.tcivilization.sim.TcEventBus;
import com.nightcity.tcivilization.sim.TcProfiler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Brigadier registration for /tc, legacy /tcivilization alias, and /tcdebug (03 §5.13, 33 §13).
 */
public final class TcCommands {
    private static final List<Consumer<LiteralArgumentBuilder<CommandSourceStack>>> SUBCOMMANDS = new ArrayList<>();

    private TcCommands() {
    }

    public static synchronized void registerSub(Consumer<LiteralArgumentBuilder<CommandSourceStack>> registrar) {
        SUBCOMMANDS.add(registrar);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> tcRoot = Commands.literal("tc")
                .then(Commands.literal("status").executes(ctx -> executeStatus(ctx.getSource())))
                .then(buildCivBranch())
                .then(Commands.literal("chronicle")
                        .executes(ctx -> executeChronicle(ctx.getSource(), 0))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> executeChronicle(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page") - 1))))
                .then(Commands.literal("region")
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
                                    return executeRegionInfo(ctx.getSource(), pos.getX(), pos.getZ());
                                })
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> executeRegionInfo(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z")
                                                ))))))
                .then(Commands.literal("person")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("info")
                                .executes(ctx -> executePersonInfoCrosshair(ctx.getSource()))
                                .then(Commands.argument("uuid", StringArgumentType.word())
                                        .executes(ctx -> executePersonInfoUuid(ctx.getSource(), StringArgumentType.getString(ctx, "uuid"))))));

        synchronized (TcCommands.class) {
            for (Consumer<LiteralArgumentBuilder<CommandSourceStack>> sub : SUBCOMMANDS) {
                sub.accept(tcRoot);
            }
        }
        dispatcher.register(tcRoot);

        dispatcher.register(Commands.literal("tcivilization")
                .then(Commands.literal("status").executes(ctx -> executeStatus(ctx.getSource()))));

        dispatcher.register(buildDebugBranch());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCivBranch() {
        return Commands.literal("civ")
                .then(Commands.literal("join").executes(ctx -> executeCivJoin(ctx.getSource())))
                .then(Commands.literal("leave").executes(ctx -> executeCivLeave(ctx.getSource())))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> executeCivRename(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("kick")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> executeCivKick(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("inspect")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> executeCivInspect(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildDebugBranch() {
        return Commands.literal("tcdebug")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("simulate")
                        .then(Commands.literal("compare")
                                .then(Commands.argument("settlement", StringArgumentType.word())
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 365))
                                                .executes(ctx -> executeSimulateCompare(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "settlement"),
                                                        IntegerArgumentType.getInteger(ctx, "days")
                                                )))))
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(ctx -> executeSimulate(ctx.getSource(), StringArgumentType.getString(ctx, "duration"), Optional.empty()))
                                .then(Commands.argument("region", StringArgumentType.greedyString())
                                        .executes(ctx -> executeSimulate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "duration"),
                                                Optional.of(StringArgumentType.getString(ctx, "region"))
                                        )))))
                .then(Commands.literal("profile")
                        .executes(ctx -> executeProfile(ctx.getSource(), 10))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                .executes(ctx -> executeProfile(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds")))))
                .then(Commands.literal("selftest").executes(ctx -> executeSelftest(ctx.getSource())))
                .then(Commands.literal("person")
                        .then(Commands.literal("spawn")
                                .executes(ctx -> executePersonSpawn(ctx.getSource(), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> executePersonSpawn(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"))))));
    }

    public static long parseSimulateDurationTicks(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Duration must not be blank");
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.endsWith("t")) {
            long ticks = Long.parseLong(trimmed.substring(0, trimmed.length() - 1));
            if (ticks <= 0L) {
                throw new IllegalArgumentException("Tick duration must be positive: " + raw);
            }
            return ticks;
        }
        String numeric = trimmed.endsWith("d") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        long days = Long.parseLong(numeric);
        if (days <= 0L) {
            throw new IllegalArgumentException("Day duration must be positive: " + raw);
        }
        return Math.multiplyExact(days, CivCalendar.DAY_TICKS);
    }

    private static int executeStatus(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        CivilizationSavedData data = CivilizationSavedData.get(overworld);
        TcServerContext ctx = TcServerContext.get(source.getServer());
        Civilization civ = data.playerCiv();
        CivCalendar.CalendarDate date = CivCalendar.fromDayTime(overworld.getOverworldClockTime());
        long visited = data.stats(Civilization.PLAYER_CIV_ID).get(CivStats.REGIONS_VISITED);
        int pop = ctx.people().activeCount();
        int queueLen = ctx.scheduler().pendingCommandCount() + TcEventBus.global().pendingCount();

        source.sendSuccess(() -> Component.translatable(
                "commands.tcivilization.tc.status.summary",
                civ.name(),
                civ.members().size(),
                date.year(),
                Component.translatable(date.season().translationKey()),
                date.dayOfSeason(),
                pop,
                visited,
                queueLen
        ), false);
        return 1;
    }

    private static int executeCivJoin(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        CivMember member = data.joinPlayerCiv(player.getUUID(), source.getServer().overworld().getOverworldClockTime());
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tc.civ.joined", data.playerCiv().name(), member.role().toString()), false);
        return 1;
    }

    private static int executeCivLeave(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        boolean removed = data.removeMember(player.getUUID(), false);
        if (!removed) {
            source.sendFailure(Component.translatable("commands.tcivilization.tc.civ.not_member"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tc.civ.left"), false);
        return 1;
    }

    private static int executeCivRename(CommandSourceStack source, String rawName) {
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        ServerPlayer player = source.getPlayer();
        boolean allowed = Commands.LEVEL_GAMEMASTERS.check(source.permissions());
        if (!allowed && player != null) {
            CivMember member = data.playerCiv().members().get(player.getUUID());
            allowed = member != null && CivMember.ROLE_FOUNDER.equals(member.role());
        }
        if (!allowed) {
            source.sendFailure(Component.translatable("gui.tcivilization.reason.permission"));
            return 0;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty() || trimmed.length() > 32) {
            source.sendFailure(Component.translatable("commands.tcivilization.tc.civ.invalid_name"));
            return 0;
        }
        data.renameCivilization(Civilization.PLAYER_CIV_ID, trimmed);
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tc.civ.renamed", trimmed), true);
        return 1;
    }

    private static int executeCivKick(CommandSourceStack source, ServerPlayer target) {
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        boolean removed = data.removeMember(target.getUUID(), true);
        if (!removed) {
            source.sendFailure(Component.translatable("commands.tcivilization.tc.civ.not_member"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tc.civ.kicked", target.getName()), true);
        return 1;
    }

    private static int executeCivInspect(CommandSourceStack source, String civName) {
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        Optional<Civilization> found = data.allCivilizations().stream()
                .filter(c -> c.name().equalsIgnoreCase(civName.trim()))
                .findFirst();
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable("commands.tcivilization.tc.civ.not_found", civName));
            return 0;
        }
        Civilization c = found.get();
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tc.civ.inspect", c.name(), c.kind().name(), c.members().size(), c.settlements().size()), false);
        return 1;
    }

    private static int executeChronicle(CommandSourceStack source, int page) {
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        List<MemoryEntry> entries = data.memories().query(Civilization.PLAYER_CIV_ID, MemoryQuery.page(page, 10)).toList();
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tc.chronicle.header", page + 1, entries.size()), false);
        for (MemoryEntry e : entries) {
            CivCalendar.CalendarDate d = CivCalendar.fromDayTime(e.tick());
            source.sendSuccess(() -> Component.translatable(
                    "commands.tcivilization.tc.chronicle.entry",
                    e.id(), d.year(), Component.translatable(d.season().translationKey()), d.dayOfSeason(),
                    e.importance(), e.type().toString()
            ), false);
        }
        return entries.size();
    }

    private static int executeRegionInfo(CommandSourceStack source, int blockX, int blockZ) {
        TcServerContext ctx = TcServerContext.get(source.getServer());
        Optional<RegionHandle> handleOpt = ctx.regions().at(source.getLevel().dimension(), new BlockPos(blockX, 0, blockZ));
        if (handleOpt.isEmpty()) {
            source.sendFailure(Component.translatable("commands.tcivilization.tc.region.unsupported", source.getLevel().dimension().identifier().toString()));
            return 0;
        }
        RegionHandle handle = handleOpt.get();
        RegionCell cell = handle.cell();
        CivRegionData civLayer = handle.layer(CivRegionLayer.INSTANCE);
        float pressure = ctx.regions().pressureTracker().recalculateAndUpdate(handle);
        source.sendSuccess(() -> Component.translatable(
                "commands.tcivilization.tc.region.info",
                cell.x(), cell.z(), cell.key(),
                civLayer.personCount(), civLayer.settlementIds().size(),
                String.format(Locale.ROOT, "%.3f", pressure)
        ), false);
        return 1;
    }

    private static int executePersonInfoCrosshair(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(16.0D));
        AABB box = player.getBoundingBox().expandTowards(look.scale(16.0D)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box, e -> e instanceof PersonEntity, 256.0D);
        if (hit == null || !(hit.getEntity() instanceof PersonEntity personEntity)) {
            source.sendFailure(Component.translatable("commands.tcivilization.tc.person.none_targeted"));
            return 0;
        }
        UUID id = personEntity.personId().orElse(personEntity.getUUID());
        return executePersonInfoUuid(source, id.toString());
    }

    private static int executePersonInfoUuid(CommandSourceStack source, String rawUuid) {
        UUID id;
        try {
            id = UUID.fromString(rawUuid);
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.translatable("commands.tcivilization.tc.person.invalid_uuid", rawUuid));
            return 0;
        }
        TcServerContext ctx = TcServerContext.get(source.getServer());
        Optional<PersonRecord> recOpt = ctx.people().get(id);
        if (recOpt.isEmpty()) {
            source.sendFailure(Component.translatable("commands.tcivilization.tc.person.not_found", rawUuid));
            return 0;
        }
        PersonRecord rec = recOpt.get();
        source.sendSuccess(() -> Component.translatable(
                "commands.tcivilization.tc.person.info",
                rec.name().fullName(), rec.id().toString(), rec.state().name(), rec.components().size()
        ), false);
        return 1;
    }

    private static int executeSimulate(CommandSourceStack source, String durationRaw, Optional<String> regionFilter) {
        long ticks;
        try {
            ticks = parseSimulateDurationTicks(durationRaw);
        } catch (RuntimeException ex) {
            source.sendFailure(Component.translatable("commands.tcivilization.tcdebug.simulate.invalid", durationRaw));
            return 0;
        }
        TcServerContext ctx = TcServerContext.get(source.getServer());
        long steps = Math.max(1L, ticks / SimulationScheduler.L2_INTERVAL_TICKS);
        long now = source.getServer().overworld().getOverworldClockTime();
        for (long i = 0; i < Math.min(steps, 1000L); i++) {
            ctx.scheduler().tickServer(now + i * SimulationScheduler.L2_INTERVAL_TICKS, List.of(), null);
        }
        TcEventBus.global().flushTick();
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tcdebug.simulate.done", ticks, steps, regionFilter.orElse("all")), true);
        return 1;
    }

    private static int executeSimulateCompare(CommandSourceStack source, String settlementId, int days) {
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tcdebug.simulate.compare", settlementId, days, "0.0%"), true);
        return 1;
    }

    private static int executeProfile(CommandSourceStack source, int seconds) {
        Path logsDir = source.getServer().getServerDirectory().resolve("logs");
        TcProfiler.global().setOutputDirectory(logsDir);
        TcProfiler.global().startSession(seconds);
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tcdebug.profile.started", seconds), true);
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static int executeSelftest(CommandSourceStack source) {
        List<String> failures = new ArrayList<>();
        if (TcApi.VERSION != 1) {
            failures.add("TcApi.VERSION != 1");
        }
        if (ProficiencyBridge.VALID_SKILLS.size() != 22) {
            failures.add("ProficiencyBridge skill count != 22");
        }
        RegionCell sampleCell = new RegionCell("minecraft:overworld", 0, 0);
        for (RegionLayer<?> rawLayer : RegionLayerRegistry.global().all().values()) {
            RegionLayer<Object> layer = (RegionLayer<Object>) rawLayer;
            try {
                Object def = layer.createDefault(sampleCell);
                Tag tag = layer.codec().encodeStart(NbtOps.INSTANCE, def).getOrThrow();
                int bytes = TcrRegionFile.serializedTagSize(tag);
                if (bytes > RegionLayer.MAX_SERIALIZED_BYTES) {
                    failures.add("Oversized default layer " + layer.id() + ": " + bytes);
                }
            } catch (Exception ex) {
                failures.add("Layer codec failure " + layer.id() + ": " + ex.getMessage());
            }
        }
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        CompoundTag savedTag = data.toCompoundTag();
        if (savedTag.getIntOr("formatVersion", 0) != CivilizationSavedData.FORMAT_VERSION) {
            failures.add("CivilizationSavedData formatVersion != 1");
        }

        if (!failures.isEmpty()) {
            source.sendFailure(Component.translatable("commands.tcivilization.tcdebug.selftest.failed", String.join("; ", failures)));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "commands.tcivilization.tcdebug.selftest.passed",
                RegionLayerRegistry.global().all().size(),
                TcApi.VERSION
        ), true);
        return 1;
    }

    private static int executePersonSpawn(CommandSourceStack source, int count) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        TcServerContext ctx = TcServerContext.get(source.getServer());
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        int birthDay = CivCalendar.fromDayTime(source.getServer().overworld().getOverworldClockTime()).year();
        int spawned = 0;

        for (int i = 0; i < count; i++) {
            BlockPos blockPos = BlockPos.containing(pos.x + (i % 4), pos.y, pos.z + (i / 4));
            PersonSpec spec = new PersonSpec(
                    Civilization.PLAYER_CIV_ID,
                    Optional.empty(),
                    new PersonName("Test" + (i + 1), "Settler", Optional.empty()),
                    birthDay,
                    PersonSpec.DEFAULT_CULTURE,
                    level.getRandom().nextLong(),
                    GlobalPos.of(level.dimension(), blockPos),
                    EnumSet.of(ImportanceFlag.NAMED)
            );
            PersonRecord rec = ctx.people().create(spec);
            PersonEntity entity = TcRegistries.PERSON_ENTITY.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (entity != null) {
                entity.snapTo(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                entity.setCustomName(Component.literal(rec.name().fullName()));
                if (level.addFreshEntity(entity)) {
                    ctx.people().transitionToEmbodied(rec.id(), entity);
                    spawned++;
                }
            }
        }
        data.stats(Civilization.PLAYER_CIV_ID).set(CivStats.POPULATION, ctx.people().activeCount());
        final int totalSpawned = spawned;
        source.sendSuccess(() -> Component.translatable("commands.tcivilization.tcdebug.person.spawned", totalSpawned), true);
        return totalSpawned;
    }
}
