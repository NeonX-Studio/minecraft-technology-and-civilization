package com.nightcity.skills.api;

public record GainResult(Status status, int requested, int clamped, int credited, int levelAfter) {
    public enum Status {
        CREDITED,
        ACCUMULATED,
        CANCELED,
        DROPPED_INVALID,
        DROPPED_EXCLUDED,
        DROPPED_IDLE,
        DEDUPED,
        QUEUED_OFFLINE,
        UNAVAILABLE
    }

    public static final GainResult UNAVAILABLE_RESULT = new GainResult(Status.UNAVAILABLE, 0, 0, 0, 0);
}
