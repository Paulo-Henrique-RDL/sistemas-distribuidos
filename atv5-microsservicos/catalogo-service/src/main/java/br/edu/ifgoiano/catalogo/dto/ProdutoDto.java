package br.edu.ifgoiano.catalogo.dto;

import br.edu.ifgoiano.catalogo.modelo.Produto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class ProdutoDto {

    private ProdutoDto() {
    }

    public record Requisicao(
            @NotBlank(message = "O SKU e obrigatorio")
            @Size(max = 30, message = "O SKU deve ter no maximo 30 caracteres")
            String sku,

            @NotBlank(message = "O nome e obrigatorio")
            @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
            String nome,

            @Size(max = 400, message = "A descricao deve ter no maximo 400 caracteres")
            String descricao,

            @NotNull(message = "O preco e obrigatorio")
            @DecimalMin(value = "0.01", message = "O preco deve ser maior que zero")
            BigDecimal preco,

            @NotNull(message = "O estoque e obrigatorio")
            @PositiveOrZero(message = "O estoque nao pode ser negativo")
            Integer estoque) {
    }

    /** Contrato publico consumido pelo pedido-service. */
    public record Resposta(
            Long id,
            String sku,
            String nome,
            String descricao,
            BigDecimal preco,
            Integer estoque) {

        public static Resposta de(Produto produto) {
            return new Resposta(produto.getId(), produto.getSku(), produto.getNome(),
                    produto.getDescricao(), produto.getPreco(), produto.getEstoque());
        }
    }
}
