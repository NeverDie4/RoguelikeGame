module com.roguelike.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.media;
    requires javafx.swing;
    requires java.desktop;
    requires java.xml;
    requires java.logging;
    requires com.google.gson;

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
    opens com.roguelike.map.config to com.google.gson;
    opens com.roguelike.network to javafx.fxml;
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
    exports com.roguelike.entities.config;
    exports com.roguelike.map to com.almasb.fxgl.all;
    exports com.roguelike.network;
    exports com.roguelike.physics;
    exports com.roguelike.ui;
    exports com.roguelike.utils;
}