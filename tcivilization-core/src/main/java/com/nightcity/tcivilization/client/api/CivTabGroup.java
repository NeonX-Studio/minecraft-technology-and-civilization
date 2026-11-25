package com.nightcity.tcivilization.client.api;

/**
 * Five ordered tab groups in the J Civilization Panel (03 §5.10.1, §12.2).
 */
public enum CivTabGroup {
    PERSONAL("civtab.group.tcivilization.personal"),
    CIVILIZATION("civtab.group.tcivilization.civilization"),
    ECONOMY("civtab.group.tcivilization.economy"),
    WORLD("civtab.group.tcivilization.world"),
    KNOWLEDGE("civtab.group.tcivilization.knowledge");

    private final String translationKey;

    CivTabGroup(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
