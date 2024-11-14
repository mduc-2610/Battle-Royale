package com.battle_royale.network;

import java.io.Serializable;

public class GamePacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public final PacketType type;
    public final Object data;

    public GamePacket(PacketType type, Object data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public String toString() {
        return "GamePacket{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}