package br.edu.ifgoiano.catalogo.controlador;

import br.edu.ifgoiano.catalogo.dto.ProdutoDto;
import br.edu.ifgoiano.catalogo.servico.ProdutoServico;
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
@RequestMapping("/produtos")
public class ProdutoControlador {

    private final ProdutoServico servico;

    public ProdutoControlador(ProdutoServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoDto.Resposta>> listar() {
        return ResponseEntity.ok(servico.listar());
    }

    /** Endpoint consumido pelo pedido-service: devolve preco e estoque atuais. */
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto.Resposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProdutoDto.Resposta> criar(@RequestBody @Valid ProdutoDto.Requisicao requisicao) {
        ProdutoDto.Resposta criado = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/produtos/" + criado.id())).body(criado);
    }
}
