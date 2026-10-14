package br.edu.ifgoiano.contas.repositorio;

import br.edu.ifgoiano.contas.modelo.ContaPagar;
import br.edu.ifgoiano.contas.modelo.Situacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContaPagarRepositorio extends JpaRepository<ContaPagar, Long> {

    /**
     * Consulta com filtros opcionais, como pede o enunciado.
     *
     * O padrao ":campo is null or ..." deixa cada filtro independente: quem nao
     * informa o parametro nao restringe o resultado. Evita escrever uma consulta
     * diferente para cada combinacao de filtros.
     *
     * O EntityGraph traz fornecedor, categoria e forma na mesma consulta. Sem
     * ele, cada linha da pagina dispararia consultas extras ao montar a resposta
     * (o problema conhecido como N+1).
     */
    @EntityGraph(attributePaths = {"fornecedor", "categoria", "formaPagamento"})
    @Query("""
            select c from ContaPagar c
            where (:situacao is null or c.situacao = :situacao)
              and (:de is null or c.dataVencimento >= :de)
              and (:ate is null or c.dataVencimento <= :ate)
              and (:fornecedorId is null or c.fornecedor.id = :fornecedorId)
              and (:categoriaId is null or c.categoria.id = :categoriaId)
            """)
    Page<ContaPagar> buscarComFiltros(@Param("situacao") Situacao situacao,
                                      @Param("de") LocalDate de,
                                      @Param("ate") LocalDate ate,
                                      @Param("fornecedorId") Long fornecedorId,
                                      @Param("categoriaId") Long categoriaId,
                                      Pageable paginacao);

    @EntityGraph(attributePaths = {"fornecedor", "categoria", "formaPagamento"})
    List<ContaPagar> findBySituacaoAndDataVencimentoBeforeOrderByDataVencimento(Situacao situacao, LocalDate data);

    /** Soma usada no resumo financeiro. O coalesce evita retorno nulo quando nao ha contas. */
    @Query("""
            select coalesce(sum(c.valor), 0) from ContaPagar c
            where c.situacao = :situacao
              and c.dataVencimento between :de and :ate
            """)
    BigDecimal somarPorSituacaoEPeriodo(@Param("situacao") Situacao situacao,
                                        @Param("de") LocalDate de,
                                        @Param("ate") LocalDate ate);

    @Query("""
            select coalesce(sum(c.valor), 0) from ContaPagar c
            where c.situacao = br.edu.ifgoiano.contas.modelo.Situacao.ABERTA
              and c.dataVencimento < :hoje
            """)
    BigDecimal somarVencidas(@Param("hoje") LocalDate hoje);

    /** Impede excluir pessoa, categoria ou forma que ja tenha conta vinculada. */
    boolean existsByFornecedorId(Long fornecedorId);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByFormaPagamentoId(Long formaPagamentoId);
}
