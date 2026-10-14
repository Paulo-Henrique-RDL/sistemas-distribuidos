package br.edu.ifgoiano.contas.dto;

import br.edu.ifgoiano.contas.modelo.ContaReceber;
import br.edu.ifgoiano.contas.modelo.Situacao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ContaReceberDto {

    private ContaReceberDto() {
    }

    public record Requisicao(
            @NotBlank(message = "A descricao e obrigatoria")
            @Size(max = 200, message = "A descricao deve ter no maximo 200 caracteres")
            String descricao,

            @Size(max = 40, message = "O numero do documento deve ter no maximo 40 caracteres")
            String numeroDocumento,

            @NotNull(message = "Informe o cliente")
            Long clienteId,

            @NotNull(message = "Informe a categoria")
            Long categoriaId,

            @NotNull(message = "O valor e obrigatorio")
            @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
            BigDecimal valor,

            @NotNull(message = "A data de emissao e obrigatoria")
            LocalDate dataEmissao,

            @NotNull(message = "A data de vencimento e obrigatoria")
            LocalDate dataVencimento,

            @Size(max = 500, message = "A observacao deve ter no maximo 500 caracteres")
            String observacao) {

        @AssertTrue(message = "A data de vencimento nao pode ser anterior a data de emissao")
        public boolean isVencimentoPosteriorAEmissao() {
            if (dataEmissao == null || dataVencimento == null) {
                return true;
            }
            return !dataVencimento.isBefore(dataEmissao);
        }
    }

    /** Registro do recebimento, exigido pelo enunciado. */
    public record Recebimento(
            @NotNull(message = "A data do recebimento e obrigatoria")
            LocalDate dataRecebimento,

            @NotNull(message = "O valor recebido e obrigatorio")
            @DecimalMin(value = "0.01", message = "O valor recebido deve ser maior que zero")
            BigDecimal valorRecebido,

            @NotNull(message = "Informe a forma de recebimento")
            Long formaRecebimentoId) {
    }

    public record Resposta(
            Long id,
            String descricao,
            String numeroDocumento,
            ResumoDto cliente,
            ResumoDto categoria,
            BigDecimal valor,
            LocalDate dataEmissao,
            LocalDate dataVencimento,
            LocalDate dataRecebimento,
            BigDecimal valorRecebido,
            ResumoDto formaRecebimento,
            Situacao situacao,
            String observacao,
            LocalDateTime dataCadastro,
            LocalDateTime dataAtualizacao) {

        public static Resposta de(ContaReceber conta) {
            return new Resposta(
                    conta.getId(),
                    conta.getDescricao(),
                    conta.getNumeroDocumento(),
                    ResumoDto.de(conta.getCliente()),
                    ResumoDto.de(conta.getCategoria()),
                    conta.getValor(),
                    conta.getDataEmissao(),
                    conta.getDataVencimento(),
                    conta.getDataRecebimento(),
                    conta.getValorRecebido(),
                    ResumoDto.de(conta.getFormaRecebimento()),
                    conta.getSituacaoAtual(),
                    conta.getObservacao(),
                    conta.getDataCadastro(),
                    conta.getDataAtualizacao());
        }
    }
}
