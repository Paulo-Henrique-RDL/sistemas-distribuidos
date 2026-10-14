package br.edu.ifgoiano.contas.excecao;

/**
 * Lancada quando a operacao e valida na forma, mas proibida pelo estado atual
 * do sistema: quitar uma conta ja paga, excluir pessoa com contas vinculadas,
 * usar uma categoria inativa.
 *
 * Vira HTTP 409 (conflito), e nao 400. A diferenca importa: 400 significa que o
 * cliente mandou algo malformado e deve corrigir o dado; 409 significa que o
 * dado esta correto, mas conflita com a situacao atual do recurso.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
