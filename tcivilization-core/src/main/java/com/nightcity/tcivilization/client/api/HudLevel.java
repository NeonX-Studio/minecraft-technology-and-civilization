package com.nightcity.tcivilization.client.api;

/**
 * HUD verbosity tier cycled by H (03 §5.10.2, DR-28).
 */
public enum HudLevel {
    MINIMAL,
    STANDARD,
    EXPANDED;

    public HudLevel next() {
        HudLevel[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }

    public boolean includes(HudLevel widgetMinLevel) {
        return this.ordinal() >= widgetMinLevel.ordinal();
    }
}
