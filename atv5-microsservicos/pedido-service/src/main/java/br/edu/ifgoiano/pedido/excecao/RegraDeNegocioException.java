package br.edu.ifgoiano.pedido.excecao;

/**
 * A requisicao esta bem formada, mas o estado atual nao permite a operacao.
 * Estoque insuficiente, pedido ja cancelado. Vira 409.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
