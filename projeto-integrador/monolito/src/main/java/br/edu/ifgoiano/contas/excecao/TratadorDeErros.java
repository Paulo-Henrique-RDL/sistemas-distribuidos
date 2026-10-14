package br.edu.ifgoiano.contas.excecao;

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
 * Centraliza o tratamento de erros de toda a aplicacao.
 *
 * A anotacao RestControllerAdvice faz esta classe interceptar as excecoes de
 * qualquer controlador. Sem ela, cada metodo precisaria do proprio try/catch e
 * as respostas de erro sairiam em formatos diferentes.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    /** Id inexistente: 404. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(RecursoNaoEncontradoException e,
                                                            HttpServletRequest requisicao) {
        return construir(HttpStatus.NOT_FOUND, e.getMessage(), requisicao);
    }

    /** Operacao proibida pelo estado atual do recurso: 409. */
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResposta> tratarRegraDeNegocio(RegraDeNegocioException e,
                                                             HttpServletRequest requisicao) {
        return construir(HttpStatus.CONFLICT, e.getMessage(), requisicao);
    }

    /**
     * Falha de validacao do @Valid: 400 com a lista de campos reprovados.
     * Devolver campo a campo evita o vai e vem de corrigir um erro por vez.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException e,
                                                        HttpServletRequest requisicao) {
        List<ErroResposta.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResposta.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();

        ErroResposta corpo = new ErroResposta(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Dados invalidos. Verifique os campos informados.",
                requisicao.getRequestURI(),
                campos);

        return ResponseEntity.badRequest().body(corpo);
    }

    /** JSON malformado ou enum com valor inexistente: 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoIlegivel(HttpMessageNotReadableException e,
                                                            HttpServletRequest requisicao) {
        return construir(HttpStatus.BAD_REQUEST,
                "Corpo da requisicao invalido. Confira o formato do JSON e os valores de data e situacao.",
                requisicao);
    }

    /** Parametro de tipo errado na URL, como ?de=ontem no lugar de uma data: 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(MethodArgumentTypeMismatchException e,
                                                           HttpServletRequest requisicao) {
        return construir(HttpStatus.BAD_REQUEST,
                "O parametro '" + e.getName() + "' recebeu um valor invalido.",
                requisicao);
    }

    /**
     * Rede de seguranca do banco: unicidade ou chave estrangeira violada.
     * O servico valida antes, mas em acesso concorrente duas requisicoes podem
     * passar pela validacao ao mesmo tempo e so o banco barrar a segunda.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarIntegridade(DataIntegrityViolationException e,
                                                          HttpServletRequest requisicao) {
        log.warn("Violacao de integridade em {}: {}", requisicao.getRequestURI(), e.getMostSpecificCause().getMessage());
        return construir(HttpStatus.CONFLICT,
                "A operacao viola uma restricao do banco de dados. Verifique duplicidade ou vinculos existentes.",
                requisicao);
    }

    /**
     * Qualquer falha nao prevista: 500 com mensagem generica.
     * A mensagem real vai para o log, nao para a resposta: detalhe interno
     * exposto ao cliente e risco de seguranca e nao ajuda quem consome a API.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroInesperado(Exception e, HttpServletRequest requisicao) {
        log.error("Erro nao tratado em {}", requisicao.getRequestURI(), e);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno no servidor. Consulte o log da aplicacao.",
                requisicao);
    }

    private ResponseEntity<ErroResposta> construir(HttpStatus status, String mensagem, HttpServletRequest requisicao) {
        ErroResposta corpo = new ErroResposta(status.value(), status.getReasonPhrase(),
                mensagem, requisicao.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
