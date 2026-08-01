package br.com.loja.pdv.infrastructure.database;

/** Fachada curta usada pela aplicacao e pelos testes para aplicar migracoes. */
public final class DatabaseInitializer {

    private final DatabaseMigrator migrator;

    /** Cria o migrador associado ao banco informado. */
    public DatabaseInitializer(Database database) {
        this.migrator = new DatabaseMigrator(database);
    }

    /** Executa todas as migracoes pendentes do banco. */
    public void initialize() {
        migrator.migrate();
    }
}
