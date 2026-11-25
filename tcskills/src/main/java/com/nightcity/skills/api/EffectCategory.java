package com.nightcity.skills.api;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EffectCategory {
    SPEED("speed", 0.006, 0.50, 0.20, true),
    YIELD("yield", 0.003, 0.25, 0.10, true),
    COST("cost", 0.003, 0.25, 0.10, false),
    WEAR("wear", 0.005, 0.40, 0.15, false),
    QUALITY("quality", 0.004, 0.30, 0.10, true),
    PRECISION("precision", 0.004, 0.30, 0.10, true),
    RANGE("range", 0.005, 0.40, 0.15, true),
    PRICE("price", 0.002, 0.15, 0.05, true),
    INFLUENCE("influence", 0.004, 0.30, 0.10, true),
    CAPACITY("capacity", 0.006, 0.50, 0.20, true),
    LEARNING("learning", 0.000, 0.50, 0.50, true);

    private static final Map<String, EffectCategory> BY_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(EffectCategory::key, Function.identity()));

    private final String key;
    private final double coeffPerLevel;
    private final double maxBonusCap;
    private final double talentHeadroom;
    private final boolean positive;

    EffectCategory(String key, double coeffPerLevel, double maxBonusCap, double talentHeadroom, boolean positive) {
        this.key = key;
        this.coeffPerLevel = coeffPerLevel;
        this.maxBonusCap = maxBonusCap;
        this.talentHeadroom = talentHeadroom;
        this.positive = positive;
    }

    public String key() {
        return key;
    }

    public double coeffPerLevel() {
        return coeffPerLevel;
    }

    public double maxBonusCap() {
        return maxBonusCap;
    }

    public double talentHeadroom() {
        return talentHeadroom;
    }

    public boolean positive() {
        return positive;
    }

    public static String normalizeEffectPath(String rawEffect) {
        if (rawEffect == null) {
            return "";
        }
        if (rawEffect.startsWith("tcskills:")) {
            return rawEffect.substring("tcskills:".length());
        }
        return switch (rawEffect) {
            case "tcatlas:pace_error" -> "precision/survey/pace";
            case "tcatlas:instrument_error" -> "precision/survey/instrument";
            case "tcatlas:sketch_radius" -> "range/survey/sketch";
            case "tcatlas:memory_half_life" -> "range/reveal/memory";
            default -> rawEffect.contains(":") ? rawEffect.substring(rawEffect.indexOf(':') + 1) : rawEffect;
        };
    }

    public static Optional<EffectCategory> fromEffectPath(String effectPath) {
        String norm = normalizeEffectPath(effectPath);
        int slash = norm.indexOf('/');
        String cat = slash >= 0 ? norm.substring(0, slash) : norm;
        return Optional.ofNullable(BY_KEY.get(cat));
    }

    public static boolean pathMatches(String prefix, String queried) {
        String p = normalizeEffectPath(prefix);
        String q = normalizeEffectPath(queried);
        return q.equals(p) || q.startsWith(p + "/");
    }
}
