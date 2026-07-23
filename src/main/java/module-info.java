module org.example.pdv_toninho {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens org.example.pdv_toninho to javafx.fxml;
    opens pdv.gerenciamento.toninho to javafx.fxml;

    exports org.example.pdv_toninho;
    exports pdv.gerenciamento.toninho;

}