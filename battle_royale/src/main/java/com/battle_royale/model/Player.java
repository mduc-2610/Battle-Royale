package com.battle_royale.model;

import com.battle_royale.utils.CollisionUtils;
import com.battle_royale.utils.Constants;
import javafx.scene.paint.Color;

import java.io.Serializable;
import java.util.Map;

public class Player implements Serializable {
    private Vector2D position;
    private Vector2D velocity;
    private final int id;
    private boolean isAlive;
    private int health;
    private Direction lastDirection;
    private double redObj, greenObj, blueObj;
    private double redBarrel, greenBarrel, blueBarrel;
    private Map<Integer, Player> relatedPlayers;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT;

        public static Direction fromInput(Input input) {
            if (input.up) return UP;
            if (input.down) return DOWN;
            if (input.left) return LEFT;
            if (input.right) return RIGHT;
            return null;
        }
    }

    public static class Input implements Serializable {
        public boolean up, down, left, right, shooting;

        public boolean isMoving() {
            return up || down || left || right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Input other = (Input) o;
            return up == other.up && down == other.down &&
                    left == other.left && right == other.right &&
                    shooting == other.shooting;
        }
    }

    public Player(int id, Map<Integer, Player> relatedPlayers) {
        this.id = id;
        this.position = new Vector2D(Math.random() * 800, Math.random() * 600);
        this.velocity = new Vector2D(0, 0);
        this.isAlive = true;
        this.health = 100;
        this.lastDirection = Direction.RIGHT;
        this.relatedPlayers = relatedPlayers;
        this.redObj = Math.random();
        this.greenObj = Math.random();
        this.blueObj = Math.random();

        this.redBarrel = Math.random();
        this.greenBarrel = Math.random();
        this.blueBarrel = Math.random();

    }

    public void update(Input input) {
        if (!isAlive) return;

        Vector2D newVelocity = new Vector2D(0, 0);
        if (input.up) {
            newVelocity.y -= 5;
            lastDirection = Direction.UP;
        }
        if (input.down) {
            newVelocity.y += 5;
            lastDirection = Direction.DOWN;
        }
        if (input.left) {
            newVelocity.x -= 5;
            lastDirection = Direction.LEFT;
        }
        if (input.right) {
            newVelocity.x += 5;
            lastDirection = Direction.RIGHT;
        }

        Vector2D originalPosition = new Vector2D(position.x, position.y);

        Vector2D newPosition = new Vector2D(
                position.x + newVelocity.x,
                position.y + newVelocity.y
        );

        position.x = newPosition.x;
        position.y = newPosition.y;

        if (checkCollision()) {
            position.x = originalPosition.x;
            position.y = originalPosition.y;

            if (newVelocity.x != 0) {
                position.x += newVelocity.x;
                if (checkCollision()) {
                    position.x = originalPosition.x;
                }
            }

            if (newVelocity.y != 0) {
                position.y += newVelocity.y;
                if (checkCollision()) {
                    position.y = originalPosition.y;
                }
            }
        }

        position.x = Math.max(0, Math.min(position.x, 800 - Constants.PLAYER_SIZE));
        position.y = Math.max(0, Math.min(position.y, 600 - Constants.PLAYER_SIZE));

        velocity.x = position.x - originalPosition.x;
        velocity.y = position.y - originalPosition.y;
    }

    public Color getObjectColor() {
        return Color.color(redObj, greenObj, blueObj);
    }

    public Color getBarrelColor() {
        return Color.color(redBarrel, greenBarrel, blueBarrel);
    }

    public Direction getLastDirection() {
        return lastDirection;
    }


    public void damage(int amount) {
        health -= amount;
        if (health <= 0) {
            isAlive = false;
        }
    }

    private boolean checkCollision() {
        for(Map.Entry<Integer, Player> _player : relatedPlayers.entrySet()) {
            if(_player.getValue() != this) {
                boolean check = CollisionUtils.checkPlayerCollision(this, _player.getValue());
                if(check) return true;
            }
        }
        return false;
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public int getHealth() {
        return health;
    }

    public Vector2D getPosition() { return position; }
    public boolean isAlive() { return isAlive; }
    public int getId() { return id; }

    public void setVelocity(Vector2D velocity) {
        this.velocity = velocity;
    }
    public void setPosition(Vector2D position) { this.position = position; }

    @Override
    public String toString() {
        return "Player{" +
                "position=" + position +
                ", velocity=" + velocity +
                ", id=" + id +
                ", isAlive=" + isAlive +
                ", health=" + health +
                ", lastDirection=" + lastDirection +
                '}';
    }
}