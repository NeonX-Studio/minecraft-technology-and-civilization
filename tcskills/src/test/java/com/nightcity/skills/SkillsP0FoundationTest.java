package com.nightcity.skills;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.nightcity.skills.api.GainFlag;
import com.nightcity.skills.api.Skill;
import com.nightcity.skills.data.SkillCurves;
import com.nightcity.skills.data.TalentDef;
import com.nightcity.skills.data.TalentLoader;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SkillsP0FoundationTest {
    @Test
    void standard22SkillsAndCurvesMatchSpec() {
        assertEquals(22, Skill.values().length);
        long activeWithoutBeyond = Stream.of(Skill.values()).filter(s -> s.isActive(m -> false)).count();
        long activeWithBeyond = Stream.of(Skill.values()).filter(s -> s.isActive("tcbeyond"::equals)).count();
        assertEquals(21L, activeWithoutBeyond);
        assertEquals(22L, activeWithBeyond);

        assertEquals(15, SkillCurves.xpToNext(0));
        assertEquals(62_635L, SkillCurves.cumulativeSkillXp(40));
        assertEquals(108_755L, SkillCurves.cumulativeSkillXp(50));
        assertEquals(0L, SkillCurves.cumulativeGrowthXp(1));
        assertEquals(544_500L, SkillCurves.cumulativeGrowthXp(100));
        assertEquals(0, SkillCurves.unlockedSpecialtySlots(9));
        assertEquals(1, SkillCurves.unlockedSpecialtySlots(10));
        assertEquals(2, SkillCurves.unlockedSpecialtySlots(30));
        assertEquals(3, SkillCurves.unlockedSpecialtySlots(60));
    }

    @Test
    void all154TalentJsonsLoadValidateAndRespectHeadroom() throws IOException {
        Path talentRoot = Path.of("src", "main", "resources", "data", "tcskills", "tc_talent");
        assertTrue(Files.isDirectory(talentRoot), "Missing tc_talent directory: " + talentRoot.toAbsolutePath());

        Map<Identifier, JsonElement> raw = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(talentRoot)) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".json")).toList()) {
                String rel = talentRoot.relativize(p).toString().replace('\\', '/');
                String idPath = rel.substring(0, rel.length() - 5);
                raw.put(Identifier.fromNamespaceAndPath("tcskills", idPath),
                        JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)));
            }
        }
        assertEquals(154, raw.size(), "Expected 22 skills x 7 slots = 154 talent JSON files");

        TalentLoader.TalentSnapshot snapshot = TalentLoader.compile(raw);
        assertTrue(snapshot.errors().isEmpty(), () -> "Talent validation errors: " + snapshot.errors());
        assertTrue(snapshot.warnings().isEmpty(), () -> "Talent headroom warnings: " + snapshot.warnings());
        assertEquals(154, snapshot.byId().size());
        for (Skill skill : Skill.values()) {
            Map<TalentDef.Slot, TalentDef> slots = snapshot.bySkillAndSlot().get(skill);
            assertNotNull(slots, "Missing talent tree for " + skill);
            assertEquals(7, slots.size(), "Expected 7 slots for " + skill);
        }
    }

    @Test
    void multipliersTalentCommitFallbackAndSavedDataRoundTrip() throws IOException {
        Path talentRoot = Path.of("src", "main", "resources", "data", "tcskills", "tc_talent");
        Map<Identifier, JsonElement> raw = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(talentRoot)) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".json")).toList()) {
                String rel = talentRoot.relativize(p).toString().replace('\\', '/');
                raw.put(Identifier.fromNamespaceAndPath("tcskills", rel.substring(0, rel.length() - 5)),
                        JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)));
            }
        }
        TalentLoader.TalentSnapshot snap = TalentLoader.compile(raw);

        UUID pid = UUID.randomUUID();
        PlayerSkillsProfile profile = PlayerSkillsProfile.empty(pid);
        for (int i = 0; i < 220; i++) {
            profile = profile.gainProficiency("mining", 500, Set.of(GainFlag.ADMIN), snap, m -> false).profile();
        }
        assertEquals(40, profile.skillLevel(Skill.MINING), "Non-specialty cap should be 40");
        assertTrue(profile.reserveXp(Skill.MINING) > 0, "Reserve XP above Lv.40 should be retained");

        profile = profile.setSpecialty(Skill.MINING);
        assertEquals(50, profile.skillLevel(Skill.MINING), "Selecting specialty should unlock reserve XP to Lv.50");

        double pickSpeed = profile.multiplier(Skill.MINING, "speed/pickaxe", snap, m -> false);
        double pickWear = profile.multiplier(Skill.MINING, "wear/pickaxe", snap, m -> false);
        assertEquals(1.30, pickSpeed, 1e-6);
        assertEquals(0.875, pickWear, 1e-6);

        // Commit T1 -> T2 -> T3a -> T4 -> T5 -> C, verify T3b is rejected due to exclusivity
        Map<TalentDef.Slot, TalentDef> miningTree = snap.bySkillAndSlot().get(Skill.MINING);
        List<Identifier> chain = List.of(
                miningTree.get(TalentDef.Slot.T1).id(),
                miningTree.get(TalentDef.Slot.T2).id(),
                miningTree.get(TalentDef.Slot.T3A).id(),
                miningTree.get(TalentDef.Slot.T4).id(),
                miningTree.get(TalentDef.Slot.T5).id(),
                miningTree.get(TalentDef.Slot.C).id()
        );
        var committedOpt = profile.commitTalents(chain, snap);
        assertTrue(committedOpt.isPresent(), "Valid specialty talent chain should commit");
        PlayerSkillsProfile committed = committedOpt.get();
        assertTrue(committed.commitTalents(List.of(miningTree.get(TalentDef.Slot.T3B).id()), snap).isEmpty(),
                "Exclusive sibling T3B must be rejected once T3A is learned");

        double cappedSpeed = committed.multiplier(Skill.MINING, "speed/pickaxe", snap, m -> false);
        assertTrue(cappedSpeed <= 1.500001 && cappedSpeed > 1.30, "DR-25 speed cap must clamp <= 1.50");

        // SavedData Codec formatVersion = 1 check
        SkillsSavedData saved = new SkillsSavedData();
        saved.putProfile(committed);
        JsonElement encoded = SkillsSavedData.CODEC.encodeStart(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(1, encoded.getAsJsonObject().get("formatVersion").getAsInt());
        SkillsSavedData decoded = SkillsSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(50, decoded.profile(pid).skillLevel(Skill.MINING));
        assertEquals(6, decoded.profile(pid).talents().size());
    }
}
