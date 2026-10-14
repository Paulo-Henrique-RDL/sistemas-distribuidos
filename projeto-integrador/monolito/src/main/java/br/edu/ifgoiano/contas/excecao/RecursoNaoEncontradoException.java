package br.edu.ifgoiano.contas.excecao;

/**
 * Lancada quando um id informado nao existe no banco. O TratadorDeErros
 * converte esta excecao em HTTP 404.
 *
 * Estende RuntimeException, e nao Exception, de proposito: nao faz sentido
 * obrigar cada servico e controlador a declarar throws para algo que nao tem
 * tratamento local possivel.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    /** Monta a mensagem no formato padrao: "Pessoa de id 7 nao encontrada." */
    public static RecursoNaoEncontradoException de(String recurso, Long id) {
        return new RecursoNaoEncontradoException(recurso + " de id " + id + " nao encontrada(o).");
    }
}
