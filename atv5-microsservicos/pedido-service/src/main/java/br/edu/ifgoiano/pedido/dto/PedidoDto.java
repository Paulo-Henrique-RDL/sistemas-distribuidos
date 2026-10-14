package br.edu.ifgoiano.pedido.dto;

import br.edu.ifgoiano.pedido.modelo.ItemPedido;
import br.edu.ifgoiano.pedido.modelo.Pedido;
import br.edu.ifgoiano.pedido.modelo.Situacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PedidoDto {

    private PedidoDto() {
    }

    /** Abertura de pedido: o cliente e a lista de itens desejados. */
    public record Requisicao(
            @NotNull(message = "Informe o cliente")
            Long clienteId,

            @NotEmpty(message = "O pedido precisa de pelo menos um item")
            @Valid
            List<ItemRequisicao> itens) {
    }

    /**
     * Note que o item pede so produto e quantidade.
     *
     * O preco nao vem do cliente da API: ele e buscado no catalogo-service no
     * momento da compra. Aceitar preco de fora seria deixar quem chama definir
     * quanto quer pagar.
     */
    public record ItemRequisicao(
            @NotNull(message = "Informe o produto")
            Long produtoId,

            @NotNull(message = "Informe a quantidade")
            @Positive(message = "A quantidade deve ser maior que zero")
            Integer quantidade) {
    }

    public record Resposta(
            Long id,
            ClienteResposta cliente,
            Situacao situacao,
            BigDecimal valorTotal,
            LocalDateTime dataPedido,
            List<ItemResposta> itens) {

        public static Resposta de(Pedido pedido, ClienteResposta cliente) {
            return new Resposta(
                    pedido.getId(),
                    cliente,
                    pedido.getSituacao(),
                    pedido.getValorTotal(),
                    pedido.getDataPedido(),
                    pedido.getItens().stream().map(ItemResposta::de).toList());
        }
    }

    /** Dados do cliente trazidos do cliente-service na hora de responder. */
    public record ClienteResposta(Long id, String nome, String documento, String email) {
    }

    public record ItemResposta(
            Long id,
            Long produtoId,
            String sku,
            String nomeProduto,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal) {

        public static ItemResposta de(ItemPedido item) {
            return new ItemResposta(item.getId(), item.getProdutoId(), item.getSku(),
                    item.getNomeProduto(), item.getQuantidade(), item.getPrecoUnitario(),
                    item.getSubtotal());
        }
    }
}
