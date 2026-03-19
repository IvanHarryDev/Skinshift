package com.harry.wildcraft.entity.skinwalker;

public enum SkinwalkerMode {
    PASSIVE(0),
    THREATENING(1),
    AGGRESSIVE(2);

    public final int id;
    SkinwalkerMode(int id) { this.id = id; }

    public static SkinwalkerMode fromId(int id) {
        return values()[Math.max(0, Math.min(id, values().length - 1))];
    }
}