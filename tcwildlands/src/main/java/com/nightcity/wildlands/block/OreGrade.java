package com.nightcity.wildlands.block;

import net.minecraft.util.StringRepresentable;

/**
 * Three-tier ore grade blockstate property (10 §2.3, §7.2): poor (1 drop), normal (2 drops), rich (3 drops).
 */
public enum OreGrade implements StringRepresentable {
    POOR("poor", 1),
    NORMAL("normal", 2),
    RICH("rich", 3);

    private final String serialized;
    private final int rawDropCount;

    OreGrade(String serialized, int rawDropCount) {
        this.serialized = serialized;
        this.rawDropCount = rawDropCount;
    }

    @Override
    public String getSerializedName() {
        return serialized;
    }

    public int rawDropCount() {
        return rawDropCount;
    }

    public static OreGrade fromGradeValue(float g) {
        if (g >= 0.66f) {
            return RICH;
        }
        if (g >= 0.33f) {
            return NORMAL;
        }
        return POOR;
    }
}
