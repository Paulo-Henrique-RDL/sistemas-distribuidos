package br.edu.ifgoiano.contas.modelo;

/**
 * Situacoes possiveis de uma conta, conforme o enunciado do projeto.
 *
 * PAGA vale apenas para contas a pagar e RECEBIDA apenas para contas a receber.
 *
 * VENCIDA nunca e gravada no banco: a conta permanece ABERTA e a situacao e
 * derivada na leitura, comparando a data de vencimento com a data atual. Assim
 * o sistema nao depende de uma rotina agendada para manter o banco correto.
 */
public enum Situacao {
    ABERTA,
    PAGA,
    RECEBIDA,
    VENCIDA,
    CANCELADA
}
