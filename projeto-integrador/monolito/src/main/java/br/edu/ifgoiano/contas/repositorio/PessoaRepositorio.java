package br.edu.ifgoiano.contas.repositorio;

import br.edu.ifgoiano.contas.modelo.Pessoa;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * O Spring Data gera a implementacao a partir da assinatura dos metodos:
 * nao existe classe PessoaRepositorioImpl escrita a mao.
 */
public interface PessoaRepositorio extends JpaRepository<Pessoa, Long> {

    Optional<Pessoa> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    /** Usado na validacao de documento duplicado durante a alteracao. */
    boolean existsByDocumentoAndIdNot(String documento, Long id);

    Page<Pessoa> findByNomeContainingIgnoreCase(String nome, Pageable paginacao);
}
