package br.edu.ifgoiano.contas.controlador;

import br.edu.ifgoiano.contas.dto.ContaReceberDto;
import br.edu.ifgoiano.contas.modelo.Situacao;
import br.edu.ifgoiano.contas.servico.ContaReceberServico;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contas-receber")
public class ContaReceberControlador {

    private final ContaReceberServico servico;

    public ContaReceberControlador(ContaReceberServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<ContaReceberDto.Resposta> criar(@RequestBody @Valid ContaReceberDto.Requisicao requisicao) {
        ContaReceberDto.Resposta criada = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/contas-receber/" + criada.id())).body(criada);
    }

    @GetMapping
    public ResponseEntity<Page<ContaReceberDto.Resposta>> buscar(
            @RequestParam(required = false) Situacao situacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 20, sort = "dataVencimento", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(servico.buscar(situacao, de, ate, clienteId, categoriaId, paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaReceberDto.Resposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContaReceberDto.Resposta> atualizar(
            @PathVariable Long id, @RequestBody @Valid ContaReceberDto.Requisicao requisicao) {
        return ResponseEntity.ok(servico.atualizar(id, requisicao));
    }

    /** Registro do recebimento. */
    @PostMapping("/{id}/receber")
    public ResponseEntity<ContaReceberDto.Resposta> receber(
            @PathVariable Long id, @RequestBody @Valid ContaReceberDto.Recebimento recebimento) {
        return ResponseEntity.ok(servico.receber(id, recebimento));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ContaReceberDto.Resposta> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(servico.cancelar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
