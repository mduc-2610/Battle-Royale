package com.battle_royale.model;

import java.io.Serializable;

public class Obstacle implements Serializable {
    private static final long serialVersionUID = 1L;
    private Vector2D position;
    private double width;
    private double height;

    public Obstacle(Vector2D position, double width, double height) {
        this.position = position;
        this.width = width;
        this.height = height;
    }

    public boolean collidesWith(Vector2D point, double objectWidth, double objectHeight) {
        return point.x < position.x + width &&
                point.x + objectWidth > position.x &&
                point.y < position.y + height &&
                point.y + objectHeight > position.y;
    }

    public Vector2D getPosition() { return position; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}

