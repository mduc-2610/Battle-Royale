package com.battle_royale.model;

import java.io.Serializable;

public class Vector2D implements Serializable {
    public double x;
    public double y;

    public Vector2D() {}

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(Vector2D v) {
        this.x = v.x;
        this.y = v.y;
    }

    public void add(Vector2D other) {
        this.x += other.x;
        this.y += other.y;
    }

    public boolean isZero() {
        return x == 0 && y == 0;
    }
    public void add(double x, double y) {
        this.x += x;
        this.y += y;
    }

    public double distance(Vector2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void normalize() {
        double length = Math.sqrt(x * x + y * y);
        if (length != 0) {
            x /= length;
            y /= length;
        }
    }

    public Vector2D multiply(double scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector2D vector2D = (Vector2D) obj;
        return Double.compare(vector2D.x, x) == 0 &&
                Double.compare(vector2D.y, y) == 0;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Vector2D{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
