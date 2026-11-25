package com.nightcity.tcivilization;

import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.CivStats;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.EraWriteToken;
import com.nightcity.tcivilization.api.MemoryEntry;
import com.nightcity.tcivilization.api.MemoryQuery;
import com.nightcity.tcivilization.api.MilestoneRecord;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.SettlementRecord;
import com.nightcity.tcivilization.api.TcMigrator;
import com.nightcity.tcivilization.api.TcMigrators;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CivilizationSavedDataAndMemoryTest {
    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyV0PrototypeAndRoundTripsV1SavedData() {
        CompoundTag legacyV0 = new CompoundTag();
        legacyV0.putLong("world_ticks", 4_800L);
        legacyV0.putInt("login_events", 7);
        legacyV0.putInt("peak_players", 4);

        CivilizationSavedData migrated = CivilizationSavedData.fromCompoundTag(legacyV0);
        assertEquals(4_800L, migrated.worldTicks());
        assertEquals(7, migrated.loginEvents());
        assertEquals(4, migrated.peakPlayers());

        com.nightcity.tcivilization.api.TcThreads.bindMainThread(Thread.currentThread());
        try {
            UUID founder = UUID.randomUUID();
            migrated.joinPlayerCiv(founder, 1_200L);
            migrated.renameCivilization(Civilization.PLAYER_CIV_ID, "Dawn Builders");
            EraWriteToken.resetForTests();
            EraWriteToken token = EraWriteToken.acquire("tctechnology");
            migrated.setEra(Civilization.PLAYER_CIV_ID, 2, token);

            assertTrue(migrated.recordRegionVisit(Civilization.PLAYER_CIV_ID, new RegionCell("minecraft:overworld", 3, -2)));
            assertFalse(migrated.recordRegionVisit(Civilization.PLAYER_CIV_ID, new RegionCell("minecraft:overworld", 3, -2)));
            assertEquals(1L, migrated.stats(Civilization.PLAYER_CIV_ID).get(CivStats.REGIONS_VISITED));

            SettlementRecord settlement = migrated.settlements().found(
                    Civilization.PLAYER_CIV_ID,
                    "Riverford",
                    net.minecraft.core.GlobalPos.of(net.minecraft.world.level.Level.OVERWORLD, new BlockPos(100, 64, -200))
            );
            migrated.recordMilestone(new MilestoneRecord(
                    Identifier.fromNamespaceAndPath("tctechnology", "first_fire"),
                    Civilization.PLAYER_CIV_ID,
                    2_400L,
                    List.of(founder)
            ));

            CompoundTag serialized = migrated.toCompoundTag();
            assertEquals(CivilizationSavedData.FORMAT_VERSION, serialized.getIntOr("formatVersion", 0));

            CivilizationSavedData restored = CivilizationSavedData.fromCompoundTag(serialized);
            assertEquals("Dawn Builders", restored.playerCiv().name());
            assertEquals(2, restored.era(Civilization.PLAYER_CIV_ID).orElse(-1));
            assertTrue(restored.playerCiv().members().containsKey(founder));
            assertEquals(1, restored.settlements().all().size());
            assertEquals(settlement.id(), restored.settlements().all().getFirst().id());
            assertEquals(1, restored.milestones(Civilization.PLAYER_CIV_ID).size());
        } finally {
            com.nightcity.tcivilization.api.TcThreads.bindMainThread(null);
        }
    }

    @Test
    void executesMultiStepMigratorChainInOrder() {
        Identifier stream = Identifier.fromNamespaceAndPath("test", "chain");
        TcMigrators.clearForTests(stream);
        TcMigrators.register(stream, 1, (ver, input) -> {
            CompoundTag out = input.copy();
            out.putString("step1", "ok");
            return out;
        });
        TcMigrators.register(stream, 2, (ver, input) -> {
            CompoundTag out = input.copy();
            out.putInt("value", out.getIntOr("value", 0) * 10);
            return out;
        });

        CompoundTag in = new CompoundTag();
        in.putInt(TcMigrators.FORMAT_VERSION_KEY, 1);
        in.putInt("value", 5);
        CompoundTag out = TcMigrators.migrate(stream, 3, in);
        assertEquals("ok", out.getStringOr("step1", ""));
        assertEquals(50, out.getIntOr("value", 0));
    }

    @Test
    void compactsOldTrivialMemoriesPreservesMajorFeatsAndWritesTcmLog() throws IOException {
        com.nightcity.tcivilization.api.TcThreads.bindMainThread(Thread.currentThread());
        try {
            java.util.concurrent.atomic.AtomicLong now = new java.util.concurrent.atomic.AtomicLong(0L);
            MemoryStore store = new MemoryStore(new com.nightcity.tcivilization.sim.TcEventBus(e -> {}), now::get);
            store.setMemoryCap(10);
            UUID civ = Civilization.PLAYER_CIV_ID;
            Identifier trivialType = Identifier.fromNamespaceAndPath("tcivilization", "daily_note");
            Identifier featType = Identifier.fromNamespaceAndPath("tcivilization", "first_smelting");

            for (int i = 0; i < 4; i++) {
                now.set(i * 1_000L);
                store.record(civ, trivialType, 1, null, List.of(), new CompoundTag());
            }
            now.set(5_000L);
            store.record(civ, featType, 5, null, List.of(), new CompoundTag());

            long year4Tick = 4L * CivCalendar.YEAR_DAYS * CivCalendar.DAY_TICKS;
            now.set(year4Tick);
            store.compactAnnual(year4Tick);

            assertEquals(2, store.count(civ));
            assertEquals(1L, store.query(civ, new MemoryQuery(5, Optional.<Identifier>empty(), java.util.OptionalLong.empty(), 0, 10)).count());

            for (int i = 0; i < 12; i++) {
                now.set(year4Tick + i * 100L);
                store.record(civ, trivialType, 2, null, List.of(), new CompoundTag());
            }
            assertTrue(store.count(civ) <= 10);
            assertEquals(1L, store.query(civ, new MemoryQuery(5, Optional.<Identifier>empty(), java.util.OptionalLong.empty(), 0, 10)).count());

            store.saveToDirectory(tempDir);
            MemoryStore reloaded = new MemoryStore(new com.nightcity.tcivilization.sim.TcEventBus(e -> {}), now::get);
            reloaded.loadCivFromDirectory(tempDir, civ);
            assertEquals(store.count(civ), reloaded.count(civ));
        } finally {
            com.nightcity.tcivilization.api.TcThreads.bindMainThread(null);
        }
    }
}
