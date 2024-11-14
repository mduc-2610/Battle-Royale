module com.battle_royale {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.battle_royale to javafx.fxml;
//    exports com.battle_royale.game;
    exports com.battle_royale.model;
    opens com.battle_royale.model to javafx.fxml;
    exports com.battle_royale.game;
    opens com.battle_royale.game to javafx.fxml;
}