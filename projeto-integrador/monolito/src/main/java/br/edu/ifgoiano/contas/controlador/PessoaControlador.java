package br.edu.ifgoiano.contas.controlador;

import br.edu.ifgoiano.contas.dto.PessoaDto;
import br.edu.ifgoiano.contas.servico.PessoaServico;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de pessoa.
 *
 * O controlador nao tem regra de negocio: ele recebe HTTP, delega ao servico e
 * traduz o retorno em status e corpo. Toda decisao de negocio esta no servico.
 */
@RestController
@RequestMapping("/api/pessoas")
public class PessoaControlador {

    private final PessoaServico servico;

    public PessoaControlador(PessoaServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<PessoaDto.Resposta> criar(@RequestBody @Valid PessoaDto.Requisicao requisicao) {
        PessoaDto.Resposta criada = servico.criar(requisicao);

        // 201 com o cabecalho Location apontando para o recurso criado: e o que a
        // convencao REST espera de um POST bem-sucedido.
        URI endereco = URI.create("/api/pessoas/" + criada.id());
        return ResponseEntity.created(endereco).body(criada);
    }

    @GetMapping
    public ResponseEntity<Page<PessoaDto.Resposta>> listar(
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(servico.listar(nome, paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaDto.Resposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PessoaDto.Resposta> atualizar(@PathVariable Long id,
                                                        @RequestBody @Valid PessoaDto.Requisicao requisicao) {
        return ResponseEntity.ok(servico.atualizar(id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        // 204: sucesso sem corpo de resposta.
        return ResponseEntity.noContent().build();
    }
}
