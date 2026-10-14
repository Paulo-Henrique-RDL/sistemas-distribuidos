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
    AMBOS;

    public boolean podeSerCliente() {
        return this == CLIENTE || this == AMBOS;
    }

    public boolean podeSerFornecedor() {
        return this == FORNECEDOR || this == AMBOS;
    }
}
