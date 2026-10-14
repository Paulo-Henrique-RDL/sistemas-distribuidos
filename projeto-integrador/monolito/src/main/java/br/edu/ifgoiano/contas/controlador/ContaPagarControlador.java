package br.edu.ifgoiano.contas.controlador;

import br.edu.ifgoiano.contas.dto.ContaPagarDto;
import br.edu.ifgoiano.contas.modelo.Situacao;
import br.edu.ifgoiano.contas.servico.ContaPagarServico;
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
@RequestMapping("/api/contas-pagar")
public class ContaPagarControlador {

    private final ContaPagarServico servico;

    public ContaPagarControlador(ContaPagarServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<ContaPagarDto.Resposta> criar(@RequestBody @Valid ContaPagarDto.Requisicao requisicao) {
        ContaPagarDto.Resposta criada = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/contas-pagar/" + criada.id())).body(criada);
    }

    /**
     * Consulta por periodo e situacao. Todos os filtros sao opcionais e
     * combinaveis; sem nenhum, devolve todas as contas paginadas.
     *
     * O ISO.DATE aceita o formato 2026-10-06 na URL. Sem ele, o Spring usaria o
     * formato da maquina e a mesma URL se comportaria diferente em cada ambiente.
     */
    @GetMapping
    public ResponseEntity<Page<ContaPagarDto.Resposta>> buscar(
            @RequestParam(required = false) Situacao situacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 20, sort = "dataVencimento", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(servico.buscar(situacao, de, ate, fornecedorId, categoriaId, paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaPagarDto.Resposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContaPagarDto.Resposta> atualizar(@PathVariable Long id,
                                                            @RequestBody @Valid ContaPagarDto.Requisicao requisicao) {
        return ResponseEntity.ok(servico.atualizar(id, requisicao));
    }

    /** Registro do pagamento. */
    @PostMapping("/{id}/pagar")
    public ResponseEntity<ContaPagarDto.Resposta> pagar(@PathVariable Long id,
                                                        @RequestBody @Valid ContaPagarDto.Pagamento pagamento) {
        return ResponseEntity.ok(servico.pagar(id, pagamento));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ContaPagarDto.Resposta> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(servico.cancelar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
