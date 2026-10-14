package br.edu.ifgoiano.contas.dto;

import br.edu.ifgoiano.contas.modelo.Categoria;
import br.edu.ifgoiano.contas.modelo.FormaPagamento;
import br.edu.ifgoiano.contas.modelo.Pessoa;

/**
 * Representacao enxuta de um relacionamento dentro da resposta de uma conta.
 *
 * Ao devolver uma conta, o cliente precisa saber quem e o fornecedor e qual a
 * categoria, mas nao precisa do cadastro inteiro. Mandar so id e nome evita
 * resposta inchada e impede vazar dados que pertencem a outro recurso.
 */
public record ResumoDto(Long id, String nome) {

    public static ResumoDto de(Pessoa pessoa) {
        return pessoa == null ? null : new ResumoDto(pessoa.getId(), pessoa.getNome());
    }

    public static ResumoDto de(Categoria categoria) {
        return categoria == null ? null : new ResumoDto(categoria.getId(), categoria.getNome());
    }

    public static ResumoDto de(FormaPagamento forma) {
        return forma == null ? null : new ResumoDto(forma.getId(), forma.getNome());
    }
}
