package br.edu.ifgoiano.catalogo.servico;

import br.edu.ifgoiano.catalogo.dto.ProdutoDto;
import br.edu.ifgoiano.catalogo.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.catalogo.modelo.Produto;
import br.edu.ifgoiano.catalogo.repositorio.ProdutoRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoServico {

    private final ProdutoRepositorio repositorio;

    public ProdutoServico(ProdutoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<ProdutoDto.Resposta> listar() {
        return repositorio.findAll().stream().map(ProdutoDto.Resposta::de).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoDto.Resposta buscarPorId(Long id) {
        return ProdutoDto.Resposta.de(repositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto", id)));
    }

    @Transactional
    public ProdutoDto.Resposta criar(ProdutoDto.Requisicao requisicao) {
        Produto produto = new Produto();
        produto.setSku(requisicao.sku());
        produto.setNome(requisicao.nome());
        produto.setDescricao(requisicao.descricao());
        produto.setPreco(requisicao.preco());
        produto.setEstoque(requisicao.estoque());
        return ProdutoDto.Resposta.de(repositorio.save(produto));
    }
}
