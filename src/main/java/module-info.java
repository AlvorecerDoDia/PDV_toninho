module br.com.loja.pdv {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.kordamp.bootstrapfx.core;

    opens br.com.loja.pdv to javafx.fxml;
    exports br.com.loja.pdv;
}
