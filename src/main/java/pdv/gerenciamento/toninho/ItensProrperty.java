package pdv.gerenciamento.toninho;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class ItensProrperty {
    private SimpleStringProperty produto;
    private SimpleDoubleProperty preco;

    public ItensProrperty(String produto, SimpleDoubleProperty preco) {
        this.produto = new SimpleStringProperty(produto);
        this.preco = new SimpleDoubleProperty(preco.doubleValue());
    }

    public String getProduto() {
        return produto.get();
    }
    public void setProduto(String produto) {
        this.produto.set(produto);
    }
    public double getPreco() {
        return preco.get();
    }
    public void setPreco(double preco) {
        this.preco.set(preco);
    }
}