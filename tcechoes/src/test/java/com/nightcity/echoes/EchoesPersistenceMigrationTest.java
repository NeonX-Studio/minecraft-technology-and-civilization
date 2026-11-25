package com.nightcity.echoes;

import com.nightcity.echoes.api.EventType;
import com.nightcity.echoes.api.HeritageEntry;
import com.nightcity.echoes.api.LegendView;
import com.nightcity.echoes.api.LiveHistoryEntry;
import com.nightcity.echoes.data.EchoesSavedData;
import com.nightcity.echoes.sim.HistoryBinIo;
import com.nightcity.echoes.sim.HistoryTerrainInput;
import com.nightcity.echoes.sim.WorldHistoryData;
import com.nightcity.echoes.sim.WorldHistorySimulator;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.TcMigrators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class EchoesPersistenceMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void v03HistoryBinRoundTripCrcValidationAndTcMigratorsForwardChain() throws IOException {
        Path migrationLog = tempDir.resolve("logs").resolve("tc-migration.log");
        TcMigrators.setMigrationLogPath(migrationLog);

        long seed = 88888888L;
        WorldHistoryData original = WorldHistorySimulator.simulate(
                HistoryTerrainInput.standaloneSynthetic(seed, 0, 0, 16),
                new WorldHistorySimulator.Config(seed, 10, 4_000, 6, 1, false)
        );

        Path historyFile = HistoryBinIo.resolvePath(tempDir.resolve("save_v03"));
        HistoryBinIo.writeAtomic(historyFile, original);
        assertTrue(Files.isRegularFile(historyFile));

        WorldHistoryData loaded = HistoryBinIo.read(historyFile);
        assertEquals(HistoryBinIo.FORMAT_VERSION, loaded.formatVersion());
        assertEquals(original.historySeed(), loaded.historySeed());
        assertEquals(original.cultures(), loaded.cultures());
        assertEquals(original.polities(), loaded.polities());
        assertEquals(original.persons(), loaded.persons());
        assertEquals(original.events(), loaded.events());
        assertEquals(original.edges(), loaded.edges());
        assertEquals(original.sites(), loaded.sites());
        assertEquals(original.routes(), loaded.routes());
        assertEquals(original.mines(), loaded.mines());
        assertEquals(original.legendSeeds(), loaded.legendSeeds());

        LiveHistoryEntry live = new LiveHistoryEntry(
                1, EventType.STELE_RAISED, new BlockPos(100, 70, -50),
                List.of(Civilization.PLAYER_CIV_ID), Identifier.fromNamespaceAndPath("tcechoes", "stele"), 45
        );
        WorldHistoryData withLive = loaded.withAppendedLiveEntry(live);
        HistoryBinIo.writeAtomic(historyFile, withLive);
        WorldHistoryData reloadedLive = HistoryBinIo.read(historyFile);
        assertEquals(1, reloadedLive.liveLog().size());
        assertEquals(live, reloadedLive.liveLog().getFirst());

        byte[] validBytes = Files.readAllBytes(historyFile);
        byte[] corruptPayload = validBytes.clone();
        corruptPayload[corruptPayload.length - 5] ^= 0x5A;
        assertThrows(IOException.class, () -> HistoryBinIo.decodeFromBytes(corruptPayload));

        byte[] corruptHeader = validBytes.clone();
        corruptHeader[10] ^= 0x0F;
        assertThrows(IOException.class, () -> HistoryBinIo.decodeFromBytes(corruptHeader));

        CompoundTag v0Raw = HistoryBinIo.toCompoundTag(original);
        v0Raw.remove(TcMigrators.FORMAT_VERSION_KEY);
        v0Raw.remove("mode");
        v0Raw.putString("legacy_mode", "standalone");
        byte[] legacyV0Bytes = HistoryBinIo.encodeLegacyV0Container(v0Raw);
        WorldHistoryData migratedFromV0 = HistoryBinIo.decodeFromBytes(legacyV0Bytes, 1);
        assertEquals(1, migratedFromV0.formatVersion());
        assertEquals(original.sites().size(), migratedFromV0.sites().size());

        TcMigrators.register(HistoryBinIo.FILE_ID, 1, (fromVer, tag) -> {
            CompoundTag next = tag.copy();
            next.putInt("endYear", 0);
            return next;
        });
        try {
            WorldHistoryData migratedToV2 = HistoryBinIo.read(historyFile, 2);
            assertEquals(2, migratedToV2.formatVersion());
            assertEquals(0, migratedToV2.endYear());
            assertArrayEquals(validBytes, Files.readAllBytes(historyFile));

            CompoundTag futureProbe = new CompoundTag();
            futureProbe.putInt(TcMigrators.FORMAT_VERSION_KEY, 99);
            assertThrows(IllegalStateException.class,
                    () -> TcMigrators.migrate(HistoryBinIo.FILE_ID, 1, futureProbe));
            assertTrue(Files.isRegularFile(migrationLog));
        } finally {
            TcMigrators.clearForTests(HistoryBinIo.FILE_ID);
            HistoryBinIo.registerDefaultMigrators();
        }
    }

    @Test
    void v03EchoesSavedDataRoundTripOptimisticConcurrencyAndForwardMigration() throws IOException {
        EchoesSavedData saved = new EchoesSavedData();
        saved.markHistoryInitialized(123456789L, 1, com.nightcity.echoes.api.SimulationMode.STANDALONE);
        saved.setRegionsPopulated(true);
        UUID sampleHistoricCiv = UUID.randomUUID();
        saved.putHistoricCivId(0, sampleHistoricCiv);
        saved.addOrUpdateLegend(new LegendView(
                0, Civilization.PLAYER_CIV_ID, LegendView.State.RUMOR,
                new BlockPos(120, 68, -80), 192,
                Identifier.fromNamespaceAndPath("tcechoes", "campfire"), 600L
        ));
        saved.setScriptMastery(new EchoesSavedData.ScriptMasteryEntry(
                Civilization.PLAYER_CIV_ID, 0, 45, List.of(101, 102)
        ));
        saved.addClue(new EchoesSavedData.EchoClueEntry(
                Civilization.PLAYER_CIV_ID, Identifier.fromNamespaceAndPath("tctechnology", "iron/aqueducts"), 0.25F, 0
        ));

        HeritageEntry h1 = new HeritageEntry(0, Civilization.PLAYER_CIV_ID, HeritageEntry.Level.LISTED, 1_000L, 420, 1L);
        assertTrue(saved.putHeritageOptimistic(h1, 0L));
        assertFalse(saved.putHeritageOptimistic(h1.bump(HeritageEntry.Level.PROTECTED), 0L));
        assertTrue(saved.putHeritageOptimistic(h1.bump(HeritageEntry.Level.PROTECTED), 1L));
        assertEquals(2L, saved.heritage(0).orElseThrow().version());

        Path datFile = EchoesSavedData.resolveDatPath(tempDir.resolve("save_v03"));
        saved.saveToDatFile(datFile);
        assertTrue(Files.isRegularFile(datFile));

        EchoesSavedData restoredSaved = EchoesSavedData.loadFromDatFile(datFile);
        assertEquals(EchoesSavedData.FORMAT_VERSION, restoredSaved.formatVersion());
        assertTrue(restoredSaved.isHistoryInitialized());
        assertTrue(restoredSaved.isRegionsPopulated());
        assertEquals(Optional.of(sampleHistoricCiv), restoredSaved.historicCivId(0));
        assertEquals(1, restoredSaved.legendsOf(Civilization.PLAYER_CIV_ID).size());
        assertEquals(45, restoredSaved.scriptMastery(Civilization.PLAYER_CIV_ID, 0));
        assertEquals(0.25F, restoredSaved.clueDiscount(Civilization.PLAYER_CIV_ID,
                Identifier.fromNamespaceAndPath("tctechnology", "iron/aqueducts")), 1e-5F);
        assertEquals(HeritageEntry.Level.PROTECTED, restoredSaved.heritage(0).orElseThrow().level());

        CompoundTag legacyDat = new CompoundTag();
        legacyDat.putBoolean("initialized", true);
        EchoesSavedData fromLegacy = EchoesSavedData.fromCompoundTag(legacyDat, 1);
        assertEquals(1, fromLegacy.formatVersion());
        assertTrue(fromLegacy.isHistoryInitialized());

        TcMigrators.register(EchoesSavedData.FILE_ID, 1, (fromVer, tag) -> {
            CompoundTag upgraded = tag.copy();
            upgraded.putInt("generatorVersion", 2);
            return upgraded;
        });
        try {
            EchoesSavedData upgradedSaved = EchoesSavedData.loadFromDatFile(datFile, 2);
            assertEquals(2, upgradedSaved.formatVersion());
            assertEquals(2, upgradedSaved.generatorVersion());
        } finally {
            TcMigrators.clearForTests(EchoesSavedData.FILE_ID);
            EchoesSavedData.registerDefaultMigrators();
        }
    }
}
