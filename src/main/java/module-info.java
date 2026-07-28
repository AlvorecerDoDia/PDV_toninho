module br.com.loja.pdv {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens br.com.loja.pdv to javafx.fxml;
    opens br.com.loja.pdv.controller to javafx.fxml;
    exports br.com.loja.pdv;
}
