package net.rtccloud.tutorial.model;

public enum Presence {
    OFFLINE, ONLINE, AWAY, DO_NOT_DISTURB;

    static Presence[] values = Presence.values();

    @Override
    public String toString() {
        return super.toString();
    }

    public static Presence fromOrdinal(int ordinal) {
        return values[ordinal % values.length];
    }
}
