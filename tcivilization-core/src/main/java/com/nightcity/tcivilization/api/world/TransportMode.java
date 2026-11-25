package com.nightcity.tcivilization.api.world;

/**
 * Infrastructure route transport mode with grade and earthwork parameters (10 §5.18.4, §12.1).
 */
public enum TransportMode {
    ROAD(0.12f, 6.0f, 1.5f),
    RAIL(0.025f, 8.0f, 0.5f),
    CANAL(0.0f, 12.0f, 0.25f),
    PIPELINE(0.50f, 3.0f, 2.0f);

    private final float maxGrade;
    private final float corridorWidthBlocks;
    private final float gradeToleranceBlocks;

    TransportMode(float maxGrade, float corridorWidthBlocks, float gradeToleranceBlocks) {
        this.maxGrade = maxGrade;
        this.corridorWidthBlocks = corridorWidthBlocks;
        this.gradeToleranceBlocks = gradeToleranceBlocks;
    }

    public float maxGrade() {
        return maxGrade;
    }

    public float corridorWidthBlocks() {
        return corridorWidthBlocks;
    }

    public float gradeToleranceBlocks() {
        return gradeToleranceBlocks;
    }
}
