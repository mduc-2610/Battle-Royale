package com.battle_royale.game;

import com.battle_royale.model.Vector2D;
import java.io.Serializable;

public abstract class GameObject implements Serializable {
    protected Vector2D position;
    protected Vector2D velocity;

    public GameObject(Vector2D position, Vector2D velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    public abstract void update();

    public Vector2D getPosition() { return position; }
    public Vector2D getVelocity() { return velocity; }

    @Override
    public String toString() {
        return "GameObject{" +
                "position=" + position +
                ", velocity=" + velocity +
                '}';
    }
}
