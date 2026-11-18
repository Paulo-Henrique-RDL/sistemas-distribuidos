package br.edu.ifgoiano.contas.repositorio;

import br.edu.ifgoiano.contas.modelo.ContaReceber;
import br.edu.ifgoiano.contas.modelo.Situacao;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContaReceberRepositorio extends JpaRepository<ContaReceber, Long> {

    @EntityGraph(attributePaths = {"cliente", "categoria", "formaRecebimento"})
    @Query("""
            select c from ContaReceber c
            where (:situacao is null or c.situacao = :situacao)
              and (:de is null or c.dataVencimento >= :de)
              and (:ate is null or c.dataVencimento <= :ate)
              and (:clienteId is null or c.cliente.id = :clienteId)
              and (:categoriaId is null or c.categoria.id = :categoriaId)
            """)
    Page<ContaReceber> buscarComFiltros(@Param("situacao") Situacao situacao,
                                        @Param("de") LocalDate de,
                                        @Param("ate") LocalDate ate,
                                        @Param("clienteId") Long clienteId,
                                        @Param("categoriaId") Long categoriaId,
                                        Pageable paginacao);

    boolean existsByClienteId(Long clienteId);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByFormaRecebimentoId(Long formaRecebimentoId);
}
