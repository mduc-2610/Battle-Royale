package com.battle_royale.utils;

import com.battle_royale.model.Player;
import com.battle_royale.model.Vector2D;

public class CollisionUtils {
    public static boolean checkPlayerCollision(Player player1, Player player2) {
        if (player1 == null || player2 == null ||
                player1.getPosition() == null || player2.getPosition() == null) {
            return false;
        }

        Vector2D pos1 = player1.getPosition();
        Vector2D pos2 = player2.getPosition();

        return pos1.x < pos2.x + Constants.PLAYER_SIZE &&
                pos1.x + Constants.PLAYER_SIZE > pos2.x &&
                pos1.y < pos2.y + Constants.PLAYER_SIZE &&
                pos1.y + Constants.PLAYER_SIZE > pos2.y;
    }


}