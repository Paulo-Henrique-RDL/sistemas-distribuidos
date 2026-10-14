package br.edu.ifgoiano.cliente.repositorio;

import br.edu.ifgoiano.cliente.modelo.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

    boolean existsByDocumento(String documento);
}
