package com.nightcity.technology.api;

import com.nightcity.technology.TTechnology;

import java.util.Locale;

public interface DifficultyApi {
    record Difficulty(
            String preset,
            double techCost,
            double knowledgeGain,
            double insightBonus,
            double populationGrowth,
            int aiAggression,
            double disasterRate
    ) {
        public static Difficulty fromPreset(String presetName) {
            return switch (presetName == null ? "standard" : presetName.toLowerCase(Locale.ROOT)) {
                case "sandbox" -> new Difficulty("sandbox", 0.5, 1.25, 0.15, 1.5, 0, 0.5);
                case "historical" -> new Difficulty("historical", 1.5, 0.9, 0.10, 0.8, 2, 1.3);
                default -> new Difficulty("standard", 1.0, 1.0, 0.10, 1.0, 1, 1.0);
            };
        }
    }

    static DifficultyApi get() {
        return TTechnology.difficulty();
    }

    Difficulty current();
}
