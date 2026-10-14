package br.edu.ifgoiano.catalogo.excecao;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corpo unico de erro, igual nos tres microsservicos.
 *
 * Padronizar o formato entre servicos e o que permite a quem consome tratar
 * falha em um lugar so. Num sistema distribuido isso vale mais ainda: o
 * pedido-service recebe erro de dois servicos diferentes e precisa entender
 * os dois.
 *
 * O campo "campos" so aparece em erro de validacao; a configuracao non_null
 * do Jackson omite o que estiver nulo.
 */
public record ErroResposta(
        LocalDateTime momento,
        int status,
        String erro,
        String mensagem,
        String caminho,
        String servico,
        List<CampoInvalido> campos) {

    public ErroResposta(int status, String erro, String mensagem, String caminho, String servico) {
        this(LocalDateTime.now(), status, erro, mensagem, caminho, servico, null);
    }

    public ErroResposta(int status, String erro, String mensagem, String caminho, String servico,
                        List<CampoInvalido> campos) {
        this(LocalDateTime.now(), status, erro, mensagem, caminho, servico, campos);
    }

    /** Um campo reprovado na validacao e o motivo. */
    public record CampoInvalido(String campo, String mensagem) {
    }
}
