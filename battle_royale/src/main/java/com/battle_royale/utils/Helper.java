package com.battle_royale.utils;

import javafx.scene.paint.Color;

public class Helper {
    public static Color getRandomColor() {
        double red = Math.random();
        double green = Math.random();
        double blue = Math.random();
        return Color.color(red, green, blue);
    }
}
