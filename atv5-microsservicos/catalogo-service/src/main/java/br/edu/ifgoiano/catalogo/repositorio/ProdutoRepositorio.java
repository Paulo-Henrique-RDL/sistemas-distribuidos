package br.edu.ifgoiano.catalogo.repositorio;

import br.edu.ifgoiano.catalogo.modelo.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepositorio extends JpaRepository<Produto, Long> {
}
