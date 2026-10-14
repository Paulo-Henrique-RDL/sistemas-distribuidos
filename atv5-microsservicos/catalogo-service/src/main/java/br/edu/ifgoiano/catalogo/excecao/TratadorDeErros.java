package br.edu.ifgoiano.catalogo.excecao;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Tratamento centralizado de erros deste microsservico.
 *
 * O RestControllerAdvice intercepta as excecoes de qualquer controlador, entao
 * nenhum controlador precisa de try/catch e toda falha sai no mesmo formato.
 *
 * O campo "servico" na resposta existe por causa da arquitetura distribuida:
 * quando o pedido-service devolve um erro, quem le precisa saber se o problema
 * nasceu nele ou em um servico que ele chamou.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);
    private static final String SERVICO = "catalogo-service";

    /** Id inexistente: 404. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(RecursoNaoEncontradoException e,
                                                            HttpServletRequest requisicao) {
        return construir(HttpStatus.NOT_FOUND, e.getMessage(), requisicao);
    }

    /** Falha de validacao do @Valid: 400 com a lista de campos reprovados. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException e,
                                                        HttpServletRequest requisicao) {
        List<ErroResposta.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResposta.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();

        ErroResposta corpo = new ErroResposta(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Dados invalidos. Verifique os campos informados.",
                requisicao.getRequestURI(), SERVICO, campos);

        return ResponseEntity.badRequest().body(corpo);
    }

    /** JSON malformado ou valor de enum inexistente: 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoIlegivel(HttpMessageNotReadableException e,
                                                            HttpServletRequest requisicao) {
        return construir(HttpStatus.BAD_REQUEST,
                "Corpo da requisicao invalido. Confira o formato do JSON.", requisicao);
    }

    /** Parametro de tipo errado na URL: 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(MethodArgumentTypeMismatchException e,
                                                           HttpServletRequest requisicao) {
        return construir(HttpStatus.BAD_REQUEST,
                "O parametro '" + e.getName() + "' recebeu um valor invalido.", requisicao);
    }

    /** Unicidade ou chave estrangeira violada no banco: 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarIntegridade(DataIntegrityViolationException e,
                                                          HttpServletRequest requisicao) {
        log.warn("Violacao de integridade em {}: {}", requisicao.getRequestURI(),
                e.getMostSpecificCause().getMessage());
        return construir(HttpStatus.CONFLICT,
                "A operacao viola uma restricao do banco. Verifique duplicidade ou vinculos existentes.",
                requisicao);
    }

    /** Qualquer falha nao prevista: 500 com mensagem generica; o detalhe vai para o log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroInesperado(Exception e, HttpServletRequest requisicao) {
        log.error("Erro nao tratado em {}", requisicao.getRequestURI(), e);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno no servidor. Consulte o log da aplicacao.", requisicao);
    }

    private ResponseEntity<ErroResposta> construir(HttpStatus status, String mensagem,
                                                   HttpServletRequest requisicao) {
        ErroResposta corpo = new ErroResposta(status.value(), status.getReasonPhrase(),
                mensagem, requisicao.getRequestURI(), SERVICO);
        return ResponseEntity.status(status).body(corpo);
    }
}
