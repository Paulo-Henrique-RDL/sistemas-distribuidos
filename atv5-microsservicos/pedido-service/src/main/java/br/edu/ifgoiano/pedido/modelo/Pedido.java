package br.edu.ifgoiano.pedido.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Pedido. Tabela cabecalho do relacionamento um-para-muitos com item_pedido.
 *
 * Repare no campo clienteId: e um Long solto, NAO uma chave estrangeira e nao
 * um relacionamento JPA. Nao poderia ser: o cliente mora no banco do
 * cliente-service, em outro arquivo, de outro processo. Um banco nao consegue
 * declarar integridade referencial para uma tabela que ele nao enxerga.
 *
 * Quem garante que esse id existe e a chamada HTTP feita antes de gravar.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao = Situacao.CRIADO;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "data_pedido", nullable = false, updatable = false)
    private LocalDateTime dataPedido;

    /**
     * O lado "muitos" do relacionamento.
     *
     * cascade e orphanRemoval fazem os itens nascerem e morrerem junto com o
     * pedido: item sem pedido nao significa nada no negocio.
     */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ItemPedido> itens = new ArrayList<>();

    public Pedido() {
    }

    /** Mantem os dois lados do relacionamento coerentes em memoria. */
    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        itens.add(item);
        valorTotal = valorTotal.add(item.getSubtotal());
    }

    public boolean estaCancelado() {
        return situacao == Situacao.CANCELADO;
    }

    public void cancelar() {
        this.situacao = Situacao.CANCELADO;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }
}
