package br.edu.ifgoiano.contas.controlador;

import br.edu.ifgoiano.contas.dto.CategoriaDto;
import br.edu.ifgoiano.contas.servico.CategoriaServico;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaControlador {

    private final CategoriaServico servico;

    public CategoriaControlador(CategoriaServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<CategoriaDto.Resposta> criar(@RequestBody @Valid CategoriaDto.Requisicao requisicao) {
        CategoriaDto.Resposta criada = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/categorias/" + criada.id())).body(criada);
    }

    @GetMapping
    public ResponseEntity<Page<CategoriaDto.Resposta>> listar(
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(servico.listar(paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDto.Resposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDto.Resposta> atualizar(@PathVariable Long id,
                                                           @RequestBody @Valid CategoriaDto.Requisicao requisicao) {
        return ResponseEntity.ok(servico.atualizar(id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
