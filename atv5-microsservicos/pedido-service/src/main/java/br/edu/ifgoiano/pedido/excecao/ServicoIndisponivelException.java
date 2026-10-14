package br.edu.ifgoiano.pedido.excecao;

/**
 * Um microsservico de que dependemos nao respondeu.
 *
 * Esta excecao so existe porque o sistema e distribuido: no monolito, chamar
 * outro modulo era uma chamada de metodo que nao falhava por rede. Aqui, cada
 * dependencia e um processo separado que pode estar fora do ar, lento ou
 * inacessivel.
 *
 * Vira 503, e nao 500, porque a causa nao e um defeito no nosso codigo: e
 * indisponibilidade temporaria de terceiro, e faz sentido tentar de novo.
 */
public class ServicoIndisponivelException extends RuntimeException {

    private final String servico;

    public ServicoIndisponivelException(String servico, Throwable causa) {
        super("O servico " + servico + " nao respondeu. Tente novamente em instantes.", causa);
        this.servico = servico;
    }

    public String getServico() {
        return servico;
    }
}
