package com.nightcity.tcivilization.client.api;

public record HudRect(int x, int y, int width, int height) {
    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }
}
