package br.edu.ifgoiano.contas.dto;

import br.edu.ifgoiano.contas.modelo.ContaPagar;
import br.edu.ifgoiano.contas.modelo.Situacao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ContaPagarDto {

    private ContaPagarDto() {
    }

    /** Cadastro e alteracao de uma conta a pagar. */
    public record Requisicao(
            @NotBlank(message = "A descricao e obrigatoria")
            @Size(max = 200, message = "A descricao deve ter no maximo 200 caracteres")
            String descricao,

            @Size(max = 40, message = "O numero do documento deve ter no maximo 40 caracteres")
            String numeroDocumento,

            @NotNull(message = "Informe o fornecedor")
            Long fornecedorId,

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

        /**
         * Validacao que envolve dois campos ao mesmo tempo, o que nenhuma anotacao
         * isolada consegue fazer. O AssertTrue roda junto com as demais, entao o
         * erro sai no mesmo 400, na lista de campos, em vez de virar um erro a parte.
         *
         * Os testes de nulo evitam duplicar mensagem: se a data nao veio, quem
         * reclama e o NotNull do proprio campo.
         */
        @AssertTrue(message = "A data de vencimento nao pode ser anterior a data de emissao")
        public boolean isVencimentoPosteriorAEmissao() {
            if (dataEmissao == null || dataVencimento == null) {
                return true;
            }
            return !dataVencimento.isBefore(dataEmissao);
        }
    }

    /** Registro do pagamento, exigido pelo enunciado. */
    public record Pagamento(
            @NotNull(message = "A data do pagamento e obrigatoria")
            LocalDate dataPagamento,

            @NotNull(message = "O valor pago e obrigatorio")
            @DecimalMin(value = "0.01", message = "O valor pago deve ser maior que zero")
            BigDecimal valorPago,

            @NotNull(message = "Informe a forma de pagamento")
            Long formaPagamentoId) {
    }

    public record Resposta(
            Long id,
            String descricao,
            String numeroDocumento,
            ResumoDto fornecedor,
            ResumoDto categoria,
            BigDecimal valor,
            LocalDate dataEmissao,
            LocalDate dataVencimento,
            LocalDate dataPagamento,
            BigDecimal valorPago,
            ResumoDto formaPagamento,
            Situacao situacao,
            String observacao,
            LocalDateTime dataCadastro,
            LocalDateTime dataAtualizacao) {

        public static Resposta de(ContaPagar conta) {
            return new Resposta(
                    conta.getId(),
                    conta.getDescricao(),
                    conta.getNumeroDocumento(),
                    ResumoDto.de(conta.getFornecedor()),
                    ResumoDto.de(conta.getCategoria()),
                    conta.getValor(),
                    conta.getDataEmissao(),
                    conta.getDataVencimento(),
                    conta.getDataPagamento(),
                    conta.getValorPago(),
                    ResumoDto.de(conta.getFormaPagamento()),
                    // Situacao calculada, nao a gravada: uma conta aberta com
                    // vencimento no passado e devolvida como VENCIDA.
                    conta.getSituacaoAtual(),
                    conta.getObservacao(),
                    conta.getDataCadastro(),
                    conta.getDataAtualizacao());
        }
    }
}
