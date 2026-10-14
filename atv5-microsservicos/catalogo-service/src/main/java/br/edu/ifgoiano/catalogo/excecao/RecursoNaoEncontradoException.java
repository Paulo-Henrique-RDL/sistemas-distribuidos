package br.edu.ifgoiano.catalogo.excecao;

/** Id que nao existe no banco deste microsservico. Vira 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException(recurso + " nao encontrado para o id " + id + ".");
    }
}
