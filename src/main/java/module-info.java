// Declara apenas os modulos usados em execucao e abre ao FXML os pacotes
// cujos controladores e campos sao acessados por reflexao.
module br.com.loja.pdv {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires jdk.charsets;

    opens br.com.loja.pdv to javafx.fxml;
    opens br.com.loja.pdv.controller to javafx.fxml;
    exports br.com.loja.pdv;
}
