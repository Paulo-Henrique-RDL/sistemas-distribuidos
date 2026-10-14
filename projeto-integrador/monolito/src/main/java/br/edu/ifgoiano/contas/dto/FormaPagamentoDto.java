package br.edu.ifgoiano.contas.dto;

import br.edu.ifgoiano.contas.modelo.FormaPagamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FormaPagamentoDto {

    private FormaPagamentoDto() {
    }

    public record Requisicao(
            @NotBlank(message = "O nome da forma de pagamento e obrigatorio")
            @Size(max = 60, message = "O nome deve ter no maximo 60 caracteres")
            String nome) {
    }

    public record Resposta(Long id, String nome) {

        public static Resposta de(FormaPagamento forma) {
            return new Resposta(forma.getId(), forma.getNome());
        }
    }
}
