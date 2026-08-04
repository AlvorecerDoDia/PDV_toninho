package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Categoria;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CategoriaRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Centraliza cadastro, consulta e situacao das categorias de produto. */
public final class CategoriaService {
    private final CategoriaRepository repository;
    private final Clock clock;

    public CategoriaService(CategoriaRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    CategoriaService(CategoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Categoria cadastrar(String nome) {
        String normalized = normalize(nome);
        if (repository.buscarPorNome(normalized).isPresent()) {
            throw new ValidationException("Já existe uma categoria com esse nome.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Categoria categoria = new Categoria();
        categoria.setNome(normalized);
        categoria.setAtiva(true);
        categoria.setCriadoEm(now);
        categoria.setAtualizadoEm(now);
        return repository.salvar(categoria);
    }

    public void atualizar(Categoria categoria) {
        if (categoria == null || categoria.getId() == null || categoria.getId() <= 0) {
            throw new ValidationException("Selecione uma categoria válida para atualizar.");
        }
        Categoria persisted = buscarPorId(categoria.getId());
        String normalized = normalize(categoria.getNome());
        repository.buscarPorNome(normalized)
                .filter(found -> !found.getId().equals(categoria.getId()))
                .ifPresent(found -> {
                    throw new ValidationException("Já existe uma categoria com esse nome.");
                });
        categoria.setNome(normalized);
        categoria.setAtiva(persisted.isAtiva());
        categoria.setCriadoEm(persisted.getCriadoEm());
        categoria.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(categoria);
    }

    public Categoria buscarPorId(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
    }

    public List<Categoria> listarAtivas() {
        return repository.listarAtivas();
    }

    public List<Categoria> listarTodas() {
        return repository.listarTodas();
    }

    public void desativar(long id) {
        buscarPorId(id);
        repository.desativar(id);
    }

    public void reativar(long id) {
        buscarPorId(id);
        repository.reativar(id);
    }

    private String normalize(String name) {
        String normalized = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new ValidationException("O nome da categoria é obrigatório.");
        }
        return normalized;
    }
}
