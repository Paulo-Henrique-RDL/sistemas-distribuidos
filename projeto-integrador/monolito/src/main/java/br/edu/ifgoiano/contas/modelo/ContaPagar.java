package br.edu.ifgoiano.contas.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Conta a pagar: uma obrigacao com um fornecedor.
 *
 * Entidade separada da ContaReceber, e nao uma unica tabela com um campo de
 * tipo, porque a segunda etapa do projeto transforma cada uma em um
 * microsservico independente. Com tabelas separadas esse corte nao exige
 * dividir dados nem reescrever consultas.
 */
@Entity
@Table(name = "conta_pagar")
public class ContaPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(name = "numero_documento", length = 40)
    private String numeroDocumento;

    // LAZY para a listagem nao carregar fornecedor, categoria e forma de todas
    // as contas de uma vez. O servico carrega o que a resposta precisa.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Pessoa fornecedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // BigDecimal e nao double: dinheiro exige precisao exata, e double
    // acumula erro de arredondamento em somas de relatorio.
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "valor_pago", precision = 15, scale = 2)
    private BigDecimal valorPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forma_pagamento_id")
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao = Situacao.ABERTA;

    @Column(length = 500)
    private String observacao;

    // Rastro temporal exigido pelo enunciado: a conta precisa de informacoes
    // suficientes para ser acompanhada ao longo do tempo. O Hibernate preenche
    // os dois campos sozinho, sem o servico precisar lembrar disso.
    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @UpdateTimestamp
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    public ContaPagar() {
    }

    /**
     * Situacao mostrada ao usuario. VENCIDA nao fica gravada no banco: ela e
     * calculada aqui, comparando o vencimento com a data de hoje. Uma conta
     * aberta vira vencida sozinha quando o prazo passa, sem rotina agendada.
     */
    public Situacao getSituacaoAtual() {
        if (situacao == Situacao.ABERTA && dataVencimento != null && dataVencimento.isBefore(LocalDate.now())) {
            return Situacao.VENCIDA;
        }
        return situacao;
    }

    public boolean estaQuitada() {
        return situacao == Situacao.PAGA;
    }

    public boolean estaCancelada() {
        return situacao == Situacao.CANCELADA;
    }

    /** Registra a quitacao. As regras sao validadas no servico antes da chamada. */
    public void registrarPagamento(LocalDate data, BigDecimal valor, FormaPagamento forma) {
        this.dataPagamento = data;
        this.valorPago = valor;
        this.formaPagamento = forma;
        this.situacao = Situacao.PAGA;
    }

    public void cancelar() {
        this.situacao = Situacao.CANCELADA;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public Pessoa getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Pessoa fornecedor) {
        this.fornecedor = fornecedor;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public void setSituacao(Situacao situacao) {
        this.situacao = situacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
}
