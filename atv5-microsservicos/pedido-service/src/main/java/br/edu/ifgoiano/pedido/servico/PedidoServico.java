package br.edu.ifgoiano.pedido.servico;

import br.edu.ifgoiano.pedido.dto.PedidoDto;
import br.edu.ifgoiano.pedido.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.pedido.excecao.RegraDeNegocioException;
import br.edu.ifgoiano.pedido.integracao.CatalogoClient;
import br.edu.ifgoiano.pedido.integracao.ClienteClient;
import br.edu.ifgoiano.pedido.modelo.ItemPedido;
import br.edu.ifgoiano.pedido.modelo.Pedido;
import br.edu.ifgoiano.pedido.repositorio.PedidoRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o pedido.
 *
 * E o unico servico dos tres que depende dos outros: para montar um pedido ele
 * precisa saber se o cliente existe e quanto custa cada produto, e essas duas
 * informacoes moram em bancos que ele nao acessa.
 */
@Service
public class PedidoServico {

    private final PedidoRepositorio repositorio;
    private final ClienteClient clienteClient;
    private final CatalogoClient catalogoClient;

    public PedidoServico(PedidoRepositorio repositorio, ClienteClient clienteClient,
                         CatalogoClient catalogoClient) {
        this.repositorio = repositorio;
        this.clienteClient = clienteClient;
        this.catalogoClient = catalogoClient;
    }

    /**
     * Abre um pedido.
     *
     * Repare que este metodo NAO tem @Transactional, e isso e proposital.
     * Abrir a transacao antes das chamadas HTTP prenderia uma conexao do banco
     * durante toda a conversa com os outros servicos; se um deles demorasse,
     * as conexoes acabariam. A regra pratica e: resolva o que depende de rede
     * primeiro, abra a transacao por ultimo e o mais curta possivel.
     *
     * Aqui, a unica escrita e o save() no fim, que ja roda na propria transacao.
     */
    public PedidoDto.Resposta criar(PedidoDto.Requisicao requisicao) {
        // 1. O cliente existe? Quem responde e o cliente-service.
        ClienteClient.ClienteResumo cliente = clienteClient.buscarPorId(requisicao.clienteId());

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.id());

        // 2. Para cada item, o catalogo-service diz o preco e o estoque do momento.
        for (PedidoDto.ItemRequisicao item : requisicao.itens()) {
            CatalogoClient.ProdutoResumo produto = catalogoClient.buscarPorId(item.produtoId());

            if (produto.estoque() < item.quantidade()) {
                throw new RegraDeNegocioException(String.format(
                        "Estoque insuficiente para o produto %s (%s): disponivel %d, pedido %d.",
                        produto.sku(), produto.nome(), produto.estoque(), item.quantidade()));
            }

            pedido.adicionarItem(new ItemPedido(produto.id(), produto.sku(), produto.nome(),
                    item.quantidade(), produto.preco()));
        }

        // 3. So agora grava, com o total ja calculado a partir dos precos reais.
        Pedido salvo = repositorio.save(pedido);
        return PedidoDto.Resposta.de(salvo, converter(cliente));
    }

    public PedidoDto.Resposta buscarPorId(Long id) {
        Pedido pedido = repositorio.findWithItensById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Pedido", id));

        // Os itens guardam o nome do produto no momento da compra, entao a
        // leitura nao precisa consultar o catalogo. So o cliente e buscado.
        return PedidoDto.Resposta.de(pedido, converter(clienteClient.buscarPorId(pedido.getClienteId())));
    }

    public List<PedidoDto.Resposta> listar(Long clienteId) {
        List<Pedido> pedidos = clienteId == null
                ? repositorio.findAllByOrderByIdDesc()
                : repositorio.findByClienteIdOrderByIdDesc(clienteId);

        return pedidos.stream()
                .map(pedido -> PedidoDto.Resposta.de(pedido,
                        converter(clienteClient.buscarPorId(pedido.getClienteId()))))
                .toList();
    }

    @Transactional
    public PedidoDto.Resposta cancelar(Long id) {
        Pedido pedido = repositorio.findWithItensById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Pedido", id));

        if (pedido.estaCancelado()) {
            throw new RegraDeNegocioException("O pedido " + id + " ja esta cancelado.");
        }

        pedido.cancelar();
        return PedidoDto.Resposta.de(pedido, converter(clienteClient.buscarPorId(pedido.getClienteId())));
    }

    private PedidoDto.ClienteResposta converter(ClienteClient.ClienteResumo cliente) {
        return new PedidoDto.ClienteResposta(cliente.id(), cliente.nome(), cliente.documento(),
                cliente.email());
    }
}
