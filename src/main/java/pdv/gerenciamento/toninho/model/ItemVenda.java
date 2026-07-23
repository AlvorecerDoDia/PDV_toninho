package pdv.gerenciamento.toninho.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class ItemVenda {

    private Produto produto;
    private SimpleStringProperty nomeProduto;
    private SimpleIntegerProperty qtdProduto;
    private SimpleDoubleProperty subtotal;

    public ItemVenda(Produto produto, int qtdProduto) {
        this.produto = produto;
        this.nomeProduto = new SimpleStringProperty(produto.getNome());
        this.qtdProduto = new SimpleIntegerProperty(qtdProduto);
        this.subtotal = new SimpleDoubleProperty(produto.getPreco() * qtdProduto);
    }

    public String getNomeProduto() { return nomeProduto.get(); }
    public SimpleStringProperty nomeProdutoProperty() { return nomeProduto; }

    public int getQtdProduto() { return qtdProduto.get(); }
    public SimpleIntegerProperty qtdProdutoProperty() { return qtdProduto; }

    public double getSubtotal() { return subtotal.get(); }
    public SimpleDoubleProperty subtotalProperty() { return subtotal; }

    public Produto getProduto() { return produto; }
}