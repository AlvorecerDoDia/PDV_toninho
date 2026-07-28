package br.com.loja.pdv.infrastructure.database;

public final class DatabaseInitializer {

    private final DatabaseMigrator migrator;

    public DatabaseInitializer(Database database) {
        this.migrator = new DatabaseMigrator(database);
    }

    public void initialize() {
        migrator.migrate();
    }
}
