package br.edu.ifgoiano.contas.repositorio;

import br.edu.ifgoiano.contas.modelo.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormaPagamentoRepositorio extends JpaRepository<FormaPagamento, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
