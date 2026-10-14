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
 * Conta a receber: um direito sobre um cliente.
 *
 * Espelha a ContaPagar, trocando fornecedor por cliente e pagamento por
 * recebimento. A duplicacao e intencional: em dezembro cada uma vira um
 * microsservico com banco proprio.
 */
@Entity
@Table(name = "conta_receber")
public class ContaReceber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(name = "numero_documento", length = 40)
    private String numeroDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Pessoa cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_recebimento")
    private LocalDate dataRecebimento;

    @Column(name = "valor_recebido", precision = 15, scale = 2)
    private BigDecimal valorRecebido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forma_recebimento_id")
    private FormaPagamento formaRecebimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao = Situacao.ABERTA;

    @Column(length = 500)
    private String observacao;

    // Mesmo rastro temporal da ContaPagar, preenchido pelo Hibernate.
    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @UpdateTimestamp
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    public ContaReceber() {
    }

    /** Mesma derivacao da ContaPagar: aberta com vencimento no passado vira VENCIDA. */
    public Situacao getSituacaoAtual() {
        if (situacao == Situacao.ABERTA && dataVencimento != null && dataVencimento.isBefore(LocalDate.now())) {
            return Situacao.VENCIDA;
        }
        return situacao;
    }

    public boolean estaQuitada() {
        return situacao == Situacao.RECEBIDA;
    }

    public boolean estaCancelada() {
        return situacao == Situacao.CANCELADA;
    }

    public void registrarRecebimento(LocalDate data, BigDecimal valor, FormaPagamento forma) {
        this.dataRecebimento = data;
        this.valorRecebido = valor;
        this.formaRecebimento = forma;
        this.situacao = Situacao.RECEBIDA;
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

    public Pessoa getCliente() {
        return cliente;
    }

    public void setCliente(Pessoa cliente) {
        this.cliente = cliente;
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

    public LocalDate getDataRecebimento() {
        return dataRecebimento;
    }

    public void setDataRecebimento(LocalDate dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
    }

    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }

    public void setValorRecebido(BigDecimal valorRecebido) {
        this.valorRecebido = valorRecebido;
    }

    public FormaPagamento getFormaRecebimento() {
        return formaRecebimento;
    }

    public void setFormaRecebimento(FormaPagamento formaRecebimento) {
        this.formaRecebimento = formaRecebimento;
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
