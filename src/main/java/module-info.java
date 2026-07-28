module br.com.loja.pdv {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens br.com.loja.pdv to javafx.fxml;
    exports br.com.loja.pdv;
}
