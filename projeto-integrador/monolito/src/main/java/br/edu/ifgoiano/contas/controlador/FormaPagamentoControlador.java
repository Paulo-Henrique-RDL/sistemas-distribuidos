package br.edu.ifgoiano.contas.controlador;

import br.edu.ifgoiano.contas.dto.FormaPagamentoDto;
import br.edu.ifgoiano.contas.servico.FormaPagamentoServico;
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
@RequestMapping("/api/formas-pagamento")
public class FormaPagamentoControlador {

    private final FormaPagamentoServico servico;

    public FormaPagamentoControlador(FormaPagamentoServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<FormaPagamentoDto.Resposta> criar(
            @RequestBody @Valid FormaPagamentoDto.Requisicao requisicao) {
        FormaPagamentoDto.Resposta criada = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/formas-pagamento/" + criada.id())).body(criada);
    }

    @GetMapping
    public ResponseEntity<Page<FormaPagamentoDto.Resposta>> listar(
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(servico.listar(paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormaPagamentoDto.Resposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormaPagamentoDto.Resposta> atualizar(
            @PathVariable Long id, @RequestBody @Valid FormaPagamentoDto.Requisicao requisicao) {
        return ResponseEntity.ok(servico.atualizar(id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
