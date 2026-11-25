package com.nightcity.technology.api;

public enum TechState {
    HIDDEN("?", false),
    UNDISCOVERED("?", true),
    KNOWN("LOCK", true),
    RESEARCHABLE("OPEN", true),
    RESEARCHING("PROG", true),
    COMPLETED("DONE", true);

    private final String badge;
    private final boolean visibleOnTree;

    TechState(String badge, boolean visibleOnTree) {
        this.badge = badge;
        this.visibleOnTree = visibleOnTree;
    }

    public String badge() {
        return badge;
    }

    public boolean visibleOnTree() {
        return visibleOnTree;
    }
}
