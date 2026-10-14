package br.edu.ifgoiano.pedido.repositorio;

import br.edu.ifgoiano.pedido.modelo.Pedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepositorio extends JpaRepository<Pedido, Long> {

    /**
     * O EntityGraph traz os itens na mesma consulta.
     *
     * Sem ele, listar 10 pedidos dispararia 1 consulta para os pedidos e mais
     * 10 para os itens de cada um: o problema N+1.
     */
    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findWithItensById(Long id);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findByClienteIdOrderByIdDesc(Long clienteId);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findAllByOrderByIdDesc();
}
