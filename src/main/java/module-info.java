module com.roguelike.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires java.logging;

    requires com.almasb.fxgl.all;

    opens com.roguelike.core to javafx.fxml;
    opens com.roguelike.entities to javafx.fxml;
    opens com.roguelike.map to javafx.fxml;
    opens com.roguelike.ui to javafx.fxml;
    opens com.roguelike.utils to javafx.fxml;

    exports com.roguelike.core;
    exports com.roguelike.entities to com.almasb.fxgl.core;
}