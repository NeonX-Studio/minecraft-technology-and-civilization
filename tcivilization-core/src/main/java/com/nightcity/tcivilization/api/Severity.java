package com.nightcity.tcivilization.api;

/**
 * Notification and alert severity levels (03 §5.10.5, §11).
 */
public enum Severity {
    INFO(100),
    SUCCESS(120),
    WARNING(160),
    CRITICAL(300),
    CEREMONY(200);

    private final int defaultTtlTicks;

    Severity(int defaultTtlTicks) {
        this.defaultTtlTicks = defaultTtlTicks;
    }

    public int defaultTtlTicks() {
        return defaultTtlTicks;
    }
}
