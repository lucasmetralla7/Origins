package com.pvpup.models;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private int level;
    private int kills;

    public PlayerData(UUID uuid, int level, int kills) {
        this.uuid = uuid;
        this.level = level;
        this.kills = kills;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }
}
