package com.battle_royale.model;

import com.battle_royale.utils.Constants;

import java.io.Serializable;

public class Projectile implements Serializable {
    private static final long serialVersionUID = 1L;
    private Vector2D position;
    private Vector2D velocity;
    private Player player;
    private static final int SIZE = 5;
    private static final int SPEED = 10;


    public Projectile(Player player, Vector2D startPosition, Vector2D direction) {
        this.player = player;
        this.position = startPosition;
        this.velocity = direction;
    }

    public Player getPlayer() {
        return player;
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public void update() {
        position.add(velocity);
    }

    public boolean isOutOfBounds() {
        return position.x < 0 || position.x > Constants.GAME_WIDTH ||
                position.y < 0 || position.y > Constants.GAME_HEIGHT;
    }

    public boolean collidesWith(Player player) {
        return position.x < player.getPosition().x + Constants.PLAYER_SIZE &&
                position.x + SIZE > player.getPosition().x &&
                position.y < player.getPosition().y + Constants.PLAYER_SIZE &&
                position.y + SIZE > player.getPosition().y;
    }

    public Vector2D getPosition() {
        return position;
    }
}
