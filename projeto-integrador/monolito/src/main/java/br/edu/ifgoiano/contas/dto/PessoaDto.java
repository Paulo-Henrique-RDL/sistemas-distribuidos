package br.edu.ifgoiano.contas.dto;

import br.edu.ifgoiano.contas.modelo.Pessoa;
import br.edu.ifgoiano.contas.modelo.TipoPessoa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTOs de pessoa, agrupados na mesma classe por serem o contrato de um unico
 * recurso.
 *
 * Por que nao expor a entidade direto no controlador: a entidade e o modelo do
 * banco, nao o contrato da API. Separando, o cliente nao ve campos internos,
 * uma mudanca de coluna nao quebra quem consome a API, e a validacao fica na
 * entrada, onde ela pertence.
 */
public final class PessoaDto {

    private PessoaDto() {
    }

    /** O que o cliente envia em POST e PUT. */
    public record Requisicao(
            @NotBlank(message = "O nome e obrigatorio")
            @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
            String nome,

            @NotBlank(message = "O documento (CPF ou CNPJ) e obrigatorio")
            @Size(max = 20, message = "O documento deve ter no maximo 20 caracteres")
            String documento,

            @NotNull(message = "Informe o tipo: CLIENTE, FORNECEDOR ou AMBOS")
            TipoPessoa tipo,

            @Email(message = "E-mail invalido")
            @Size(max = 120, message = "O e-mail deve ter no maximo 120 caracteres")
            String email,

            @Size(max = 20, message = "O telefone deve ter no maximo 20 caracteres")
            String telefone) {
    }

    /** O que a API devolve. Inclui o id, que o cliente nao envia. */
    public record Resposta(
            Long id,
            String nome,
            String documento,
            TipoPessoa tipo,
            String email,
            String telefone) {

        public static Resposta de(Pessoa pessoa) {
            return new Resposta(pessoa.getId(), pessoa.getNome(), pessoa.getDocumento(),
                    pessoa.getTipo(), pessoa.getEmail(), pessoa.getTelefone());
        }
    }
}
