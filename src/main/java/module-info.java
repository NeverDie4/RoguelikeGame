module com.roguelike.main {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;

    opens com.roguelike.main to javafx.fxml;
    exports com.roguelike.main;
}