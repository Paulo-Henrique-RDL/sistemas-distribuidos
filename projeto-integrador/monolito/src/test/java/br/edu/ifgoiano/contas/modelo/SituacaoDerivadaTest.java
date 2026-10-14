package br.edu.ifgoiano.contas.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da regra de derivacao da situacao VENCIDA.
 *
 * Sao testes de unidade puros: nao sobem o Spring nem tocam no banco, porque a
 * regra vive inteira dentro da entidade. Rodam em milissegundos.
 */
class SituacaoDerivadaTest {

    @Test
    @DisplayName("Conta a pagar aberta com vencimento no passado e devolvida como VENCIDA")
    void contaPagarAbertaVencidaViraVencida() {
        // Cenario: conta aberta, vencimento ontem.
        ContaPagar conta = new ContaPagar();
        conta.setSituacao(Situacao.ABERTA);
        conta.setDataVencimento(LocalDate.now().minusDays(1));

        // Resultado esperado: situacao derivada VENCIDA, mas a gravada continua ABERTA.
        assertThat(conta.getSituacaoAtual()).isEqualTo(Situacao.VENCIDA);
        assertThat(conta.getSituacao()).isEqualTo(Situacao.ABERTA);
    }

    @Test
    @DisplayName("Conta a pagar que vence hoje ainda e ABERTA")
    void contaQueVenceHojeContinuaAberta() {
        // O vencimento e no fim do dia: so vence a partir do dia seguinte.
        ContaPagar conta = new ContaPagar();
        conta.setSituacao(Situacao.ABERTA);
        conta.setDataVencimento(LocalDate.now());

        assertThat(conta.getSituacaoAtual()).isEqualTo(Situacao.ABERTA);
    }

    @Test
    @DisplayName("Conta paga com vencimento no passado permanece PAGA, nao vira VENCIDA")
    void contaPagaNaoViraVencida() {
        ContaPagar conta = new ContaPagar();
        conta.setSituacao(Situacao.PAGA);
        conta.setDataVencimento(LocalDate.now().minusMonths(2));

        assertThat(conta.getSituacaoAtual()).isEqualTo(Situacao.PAGA);
    }

    @Test
    @DisplayName("Conta cancelada com vencimento no passado permanece CANCELADA")
    void contaCanceladaNaoViraVencida() {
        ContaReceber conta = new ContaReceber();
        conta.setSituacao(Situacao.CANCELADA);
        conta.setDataVencimento(LocalDate.now().minusDays(30));

        assertThat(conta.getSituacaoAtual()).isEqualTo(Situacao.CANCELADA);
    }

    @Test
    @DisplayName("Conta a receber aberta e vencida tambem e derivada como VENCIDA")
    void contaReceberAbertaVencida() {
        ContaReceber conta = new ContaReceber();
        conta.setSituacao(Situacao.ABERTA);
        conta.setDataVencimento(LocalDate.now().minusDays(5));

        assertThat(conta.getSituacaoAtual()).isEqualTo(Situacao.VENCIDA);
    }
}
