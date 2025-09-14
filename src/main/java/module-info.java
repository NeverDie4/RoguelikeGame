module com.roguelike.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.xml;
    requires java.logging;

    requires transitive javafx.graphics;
    requires transitive com.almasb.fxgl.all;

    opens com.roguelike.core to javafx.fxml;
    opens com.roguelike.entities to javafx.fxml;
    opens com.roguelike.entities.bullets to javafx.fxml;
    opens com.roguelike.entities.attacks to javafx.fxml;
    opens com.roguelike.entities.configs to javafx.fxml;
    opens com.roguelike.entities.factory to javafx.fxml;
    opens com.roguelike.entities.weapons to javafx.fxml;
    opens com.roguelike.entities.components to javafx.fxml;
    opens com.roguelike.map to javafx.fxml;
    opens com.roguelike.physics to javafx.fxml;
    opens com.roguelike.ui to javafx.fxml;
    opens com.roguelike.utils to javafx.fxml;

    exports com.roguelike.core;
    exports com.roguelike.entities to com.almasb.fxgl.core;
    exports com.roguelike.entities.bullets;
    exports com.roguelike.entities.attacks;
    exports com.roguelike.entities.configs;
    exports com.roguelike.entities.factory;
    exports com.roguelike.entities.components;
    exports com.roguelike.entities.weapons;
    exports com.roguelike.map;
    exports com.roguelike.physics;
}