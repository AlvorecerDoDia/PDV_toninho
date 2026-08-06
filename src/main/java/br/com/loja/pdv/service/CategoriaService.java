package br.com.loja.pdv.service;

import br.com.loja.pdv.domain.model.Categoria;
import br.com.loja.pdv.exception.EntityNotFoundException;
import br.com.loja.pdv.exception.ValidationException;
import br.com.loja.pdv.repository.CategoriaRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Permite cadastrar, renomear e listar categorias. */
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
        String normalizado = normalizar(nome);
        if (repository.buscarPorNome(normalizado).isPresent()) {
            throw new ValidationException("Já existe uma categoria com esse nome.");
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        Categoria categoria = new Categoria();
        categoria.setNome(normalizado);
        categoria.setAtiva(true);
        categoria.setCriadoEm(agora);
        categoria.setAtualizadoEm(agora);
        return repository.salvar(categoria);
    }

    public void atualizar(Categoria categoria) {
        if (categoria == null || categoria.getId() == null || categoria.getId() <= 0) {
            throw new ValidationException("Selecione uma categoria válida para atualizar.");
        }
        Categoria persistida = buscarPorId(categoria.getId());
        String normalizado = normalizar(categoria.getNome());
        repository.buscarPorNome(normalizado)
                .filter(encontrada -> !encontrada.getId().equals(categoria.getId()))
                .ifPresent(encontrada -> {
                    throw new ValidationException("Já existe uma categoria com esse nome.");
                });
        categoria.setNome(normalizado);
        categoria.setAtiva(true);
        categoria.setCriadoEm(persistida.getCriadoEm());
        categoria.setAtualizadoEm(LocalDateTime.now(clock));
        repository.atualizar(categoria);
    }

    public Categoria buscarPorId(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
    }

    public List<Categoria> listarTodas() {
        return repository.listarTodas();
    }

    private String normalizar(String nome) {
        String normalizado = nome == null ? "" : nome.strip().replaceAll("\\s+", " ");
        if (normalizado.isEmpty()) {
            throw new ValidationException("O nome da categoria é obrigatório.");
        }
        return normalizado;
    }
}
