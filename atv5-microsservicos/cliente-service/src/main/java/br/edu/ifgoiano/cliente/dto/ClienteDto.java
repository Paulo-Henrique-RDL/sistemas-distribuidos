package br.edu.ifgoiano.cliente.dto;

import br.edu.ifgoiano.cliente.modelo.Cliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ClienteDto {

    private ClienteDto() {
    }

    /** Cadastro de cliente. */
    public record Requisicao(
            @NotBlank(message = "O nome e obrigatorio")
            @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
            String nome,

            @NotBlank(message = "O documento e obrigatorio")
            @Size(max = 20, message = "O documento deve ter no maximo 20 caracteres")
            String documento,

            @NotBlank(message = "O e-mail e obrigatorio")
            @Email(message = "E-mail invalido")
            String email,

            @Size(max = 80, message = "A cidade deve ter no maximo 80 caracteres")
            String cidade,

            @Size(min = 2, max = 2, message = "A UF deve ter 2 letras")
            String uf) {
    }

    /**
     * Resposta publica deste microsservico.
     *
     * E o contrato que o pedido-service consome. Ele nao conhece a entidade
     * Cliente nem o banco daqui: conhece este formato. Mudar uma coluna sem
     * mudar este record nao quebra ninguem.
     */
    public record Resposta(
            Long id,
            String nome,
            String documento,
            String email,
            String cidade,
            String uf) {

        public static Resposta de(Cliente cliente) {
            return new Resposta(cliente.getId(), cliente.getNome(), cliente.getDocumento(),
                    cliente.getEmail(), cliente.getCidade(), cliente.getUf());
        }
    }
}
