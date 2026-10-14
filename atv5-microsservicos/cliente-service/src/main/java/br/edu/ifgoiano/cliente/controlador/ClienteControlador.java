package br.edu.ifgoiano.cliente.controlador;

import br.edu.ifgoiano.cliente.dto.ClienteDto;
import br.edu.ifgoiano.cliente.servico.ClienteServico;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
public class ClienteControlador {

    private final ClienteServico servico;

    public ClienteControlador(ClienteServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public ResponseEntity<List<ClienteDto.Resposta>> listar() {
        return ResponseEntity.ok(servico.listar());
    }

    /** Endpoint consumido pelo pedido-service para validar e enriquecer o pedido. */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteDto.Resposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ClienteDto.Resposta> criar(@RequestBody @Valid ClienteDto.Requisicao requisicao) {
        ClienteDto.Resposta criado = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/clientes/" + criado.id())).body(criado);
    }
}
