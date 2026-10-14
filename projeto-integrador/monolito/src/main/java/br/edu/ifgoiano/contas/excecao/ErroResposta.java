package br.edu.ifgoiano.contas.excecao;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corpo unico de resposta para qualquer erro da API.
 *
 * E um record: imutavel e sem boilerplate de getters. Como toda falha responde
 * neste formato, quem consome a API trata erro em um lugar so, em vez de
 * adivinhar o formato de cada endpoint.
 *
 * O campo "campos" so aparece em erro de validacao: a configuracao
 * non_null do Jackson omite da resposta o que estiver nulo.
 */
public record ErroResposta(
        LocalDateTime momento,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos) {

    public ErroResposta(int status, String erro, String mensagem, String caminho) {
        this(LocalDateTime.now(), status, erro, mensagem, caminho, null);
    }

    public ErroResposta(int status, String erro, String mensagem, String caminho, List<CampoInvalido> campos) {
        this(LocalDateTime.now(), status, erro, mensagem, caminho, campos);
    }

    /** Um campo reprovado na validacao e o motivo. */
    public record CampoInvalido(String campo, String mensagem) {
    }
}
