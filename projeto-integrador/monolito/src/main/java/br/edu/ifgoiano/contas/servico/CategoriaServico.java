package br.edu.ifgoiano.contas.servico;

import br.edu.ifgoiano.contas.dto.CategoriaDto;
import br.edu.ifgoiano.contas.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.contas.excecao.RegraDeNegocioException;
import br.edu.ifgoiano.contas.modelo.Categoria;
import br.edu.ifgoiano.contas.repositorio.CategoriaRepositorio;
import br.edu.ifgoiano.contas.repositorio.ContaPagarRepositorio;
import br.edu.ifgoiano.contas.repositorio.ContaReceberRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaServico {

    private final CategoriaRepositorio categoriaRepositorio;
    private final ContaPagarRepositorio contaPagarRepositorio;
    private final ContaReceberRepositorio contaReceberRepositorio;

    public CategoriaServico(CategoriaRepositorio categoriaRepositorio,
                            ContaPagarRepositorio contaPagarRepositorio,
                            ContaReceberRepositorio contaReceberRepositorio) {
        this.categoriaRepositorio = categoriaRepositorio;
        this.contaPagarRepositorio = contaPagarRepositorio;
        this.contaReceberRepositorio = contaReceberRepositorio;
    }

    @Transactional
    public CategoriaDto.Resposta criar(CategoriaDto.Requisicao requisicao) {
        // IgnoreCase evita cadastrar "Aluguel" e "aluguel" como categorias distintas.
        if (categoriaRepositorio.existsByNomeIgnoreCase(requisicao.nome())) {
            throw new RegraDeNegocioException("Ja existe a categoria " + requisicao.nome() + ".");
        }

        Categoria categoria = new Categoria(requisicao.nome(), requisicao.descricao());
        return CategoriaDto.Resposta.de(categoriaRepositorio.save(categoria));
    }

    @Transactional(readOnly = true)
    public Page<CategoriaDto.Resposta> listar(Pageable paginacao) {
        return categoriaRepositorio.findAll(paginacao).map(CategoriaDto.Resposta::de);
    }

    @Transactional(readOnly = true)
    public CategoriaDto.Resposta buscarPorId(Long id) {
        return CategoriaDto.Resposta.de(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Categoria buscarEntidade(Long id) {
        return categoriaRepositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Categoria", id));
    }

    @Transactional
    public CategoriaDto.Resposta atualizar(Long id, CategoriaDto.Requisicao requisicao) {
        Categoria categoria = buscarEntidade(id);

        if (categoriaRepositorio.existsByNomeIgnoreCaseAndIdNot(requisicao.nome(), id)) {
            throw new RegraDeNegocioException("O nome " + requisicao.nome() + " ja pertence a outra categoria.");
        }

        categoria.setNome(requisicao.nome());
        categoria.setDescricao(requisicao.descricao());
        return CategoriaDto.Resposta.de(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidade(id);

        if (contaPagarRepositorio.existsByCategoriaId(id) || contaReceberRepositorio.existsByCategoriaId(id)) {
            throw new RegraDeNegocioException("A categoria possui contas vinculadas e nao pode ser excluida.");
        }

        categoriaRepositorio.delete(categoria);
    }
}
