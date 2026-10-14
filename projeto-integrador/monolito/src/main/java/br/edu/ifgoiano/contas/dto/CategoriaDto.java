package br.edu.ifgoiano.contas.dto;

import br.edu.ifgoiano.contas.modelo.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoriaDto {

    private CategoriaDto() {
    }

    public record Requisicao(
            @NotBlank(message = "O nome da categoria e obrigatorio")
            @Size(max = 80, message = "O nome deve ter no maximo 80 caracteres")
            String nome,

            @Size(max = 200, message = "A descricao deve ter no maximo 200 caracteres")
            String descricao) {
    }

    public record Resposta(Long id, String nome, String descricao) {

        public static Resposta de(Categoria categoria) {
            return new Resposta(categoria.getId(), categoria.getNome(), categoria.getDescricao());
        }
    }
}
