package br.edu.ifgoiano.pedido.controlador;

import br.edu.ifgoiano.pedido.dto.PedidoDto;
import br.edu.ifgoiano.pedido.servico.PedidoServico;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoControlador {

    private final PedidoServico servico;

    public PedidoControlador(PedidoServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<PedidoDto.Resposta> criar(@RequestBody @Valid PedidoDto.Requisicao requisicao) {
        PedidoDto.Resposta criado = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/pedidos/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDto.Resposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<PedidoDto.Resposta>> listar(
            @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(servico.listar(clienteId));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PedidoDto.Resposta> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(servico.cancelar(id));
    }
}
