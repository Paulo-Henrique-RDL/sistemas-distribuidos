package br.edu.ifgoiano.contas.modelo;

/**
 * Papel da pessoa nas transacoes financeiras.
 *
 * AMBOS existe porque uma mesma empresa pode ser fornecedora em uma conta a
 * pagar e cliente em uma conta a receber.
 */
public enum TipoPessoa {
    CLIENTE,
    FORNECEDOR,
    AMBOS
}
