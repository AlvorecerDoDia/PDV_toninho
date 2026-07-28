package br.com.loja.pdv.infrastructure.database;

import br.com.loja.pdv.infrastructure.database.Database;

import java.sql.SQLException;

public class Teste  {
    public static void main(String[] args) {
        try (var connection = Database.getConnection()) {
            System.out.println("Banco conectado com sucesso.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}