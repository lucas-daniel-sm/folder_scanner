module folder.scanner.main {
    requires javafx.controls;

    opens org.example to javafx.fxml;
    exports org.example to javafx.graphics;
}