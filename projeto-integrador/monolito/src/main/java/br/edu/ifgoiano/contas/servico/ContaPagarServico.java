package br.edu.ifgoiano.contas.servico;

import br.edu.ifgoiano.contas.dto.ContaPagarDto;
import br.edu.ifgoiano.contas.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.contas.excecao.RegraDeNegocioException;
import br.edu.ifgoiano.contas.modelo.ContaPagar;
import br.edu.ifgoiano.contas.modelo.FormaPagamento;
import br.edu.ifgoiano.contas.modelo.Situacao;
import br.edu.ifgoiano.contas.repositorio.ContaPagarRepositorio;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaPagarServico {

    private final ContaPagarRepositorio repositorio;
    private final PessoaServico pessoaServico;
    private final CategoriaServico categoriaServico;
    private final FormaPagamentoServico formaPagamentoServico;

    public ContaPagarServico(ContaPagarRepositorio repositorio,
                             PessoaServico pessoaServico,
                             CategoriaServico categoriaServico,
                             FormaPagamentoServico formaPagamentoServico) {
        this.repositorio = repositorio;
        this.pessoaServico = pessoaServico;
        this.categoriaServico = categoriaServico;
        this.formaPagamentoServico = formaPagamentoServico;
    }

    @Transactional
    public ContaPagarDto.Resposta criar(ContaPagarDto.Requisicao requisicao) {
        ContaPagar conta = new ContaPagar();
        aplicar(conta, requisicao);
        // Nasce sempre aberta: a quitacao e uma operacao separada e explicita.
        conta.setSituacao(Situacao.ABERTA);
        return ContaPagarDto.Resposta.de(repositorio.save(conta));
    }

    /**
     * Consulta por periodo e situacao, exigida pelo enunciado.
     *
     * A situacao VENCIDA merece atencao: ela nao existe no banco, e derivada.
     * Por isso o filtro e traduzido antes de chegar ao repositorio:
     *   VENCIDA -> situacao ABERTA com vencimento anterior a hoje
     *   ABERTA  -> situacao ABERTA com vencimento de hoje em diante
     * Assim as duas ficam disjuntas e a soma delas bate com o total de abertas.
     */
    @Transactional(readOnly = true)
    public Page<ContaPagarDto.Resposta> buscar(Situacao situacao, LocalDate de, LocalDate ate,
                                               Long fornecedorId, Long categoriaId, Pageable paginacao) {
        LocalDate hoje = LocalDate.now();
        Situacao situacaoNoBanco = situacao;
        LocalDate deEfetivo = de;
        LocalDate ateEfetivo = ate;

        if (situacao == Situacao.VENCIDA) {
            situacaoNoBanco = Situacao.ABERTA;
            LocalDate ontem = hoje.minusDays(1);
            // Intersecao com o periodo informado: o filtro do usuario continua valendo.
            ateEfetivo = (ate == null || ate.isAfter(ontem)) ? ontem : ate;
        } else if (situacao == Situacao.ABERTA) {
            deEfetivo = (de == null || de.isBefore(hoje)) ? hoje : de;
        } else if (situacao == Situacao.RECEBIDA) {
            // RECEBIDA pertence a conta a receber; aqui nunca ha resultado.
            throw new RegraDeNegocioException("A situacao RECEBIDA nao se aplica a contas a pagar.");
        }

        return repositorio.buscarComFiltros(situacaoNoBanco, deEfetivo, ateEfetivo,
                fornecedorId, categoriaId, paginacao).map(ContaPagarDto.Resposta::de);
    }

    @Transactional(readOnly = true)
    public ContaPagarDto.Resposta buscarPorId(Long id) {
        return ContaPagarDto.Resposta.de(buscarEntidade(id));
    }

    @Transactional
    public ContaPagarDto.Resposta atualizar(Long id, ContaPagarDto.Requisicao requisicao) {
        ContaPagar conta = buscarEntidade(id);

        // Conta ja quitada ou cancelada nao e alterada: mudar o valor de algo que
        // ja foi pago reescreveria o historico financeiro.
        if (conta.estaQuitada()) {
            throw new RegraDeNegocioException("A conta ja foi paga e nao pode ser alterada.");
        }
        if (conta.estaCancelada()) {
            throw new RegraDeNegocioException("A conta esta cancelada e nao pode ser alterada.");
        }

        aplicar(conta, requisicao);
        return ContaPagarDto.Resposta.de(conta);
    }

    /** Registro do pagamento, um dos requisitos minimos do enunciado. */
    @Transactional
    public ContaPagarDto.Resposta pagar(Long id, ContaPagarDto.Pagamento pagamento) {
        ContaPagar conta = buscarEntidade(id);

        // Operacao repetida: quitar duas vezes criaria lancamento em duplicidade.
        if (conta.estaQuitada()) {
            throw new RegraDeNegocioException("A conta ja foi paga em " + conta.getDataPagamento() + ".");
        }
        if (conta.estaCancelada()) {
            throw new RegraDeNegocioException("A conta esta cancelada e nao pode ser paga.");
        }
        if (pagamento.dataPagamento().isBefore(conta.getDataEmissao())) {
            throw new RegraDeNegocioException("A data do pagamento nao pode ser anterior a emissao da conta.");
        }

        FormaPagamento forma = formaPagamentoServico.buscarEntidade(pagamento.formaPagamentoId());
        conta.registrarPagamento(pagamento.dataPagamento(), pagamento.valorPago(), forma);
        return ContaPagarDto.Resposta.de(conta);
    }

    @Transactional
    public ContaPagarDto.Resposta cancelar(Long id) {
        ContaPagar conta = buscarEntidade(id);

        if (conta.estaQuitada()) {
            throw new RegraDeNegocioException("A conta ja foi paga e nao pode ser cancelada.");
        }
        if (conta.estaCancelada()) {
            throw new RegraDeNegocioException("A conta ja esta cancelada.");
        }

        conta.cancelar();
        return ContaPagarDto.Resposta.de(conta);
    }

    @Transactional
    public void excluir(Long id) {
        ContaPagar conta = buscarEntidade(id);

        // Conta paga permanece no sistema: o enunciado exige poder acompanhar a
        // conta ao longo do tempo, e apagar destruiria esse historico.
        if (conta.estaQuitada()) {
            throw new RegraDeNegocioException("A conta ja foi paga e nao pode ser excluida.");
        }

        repositorio.delete(conta);
    }

    private ContaPagar buscarEntidade(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Conta a pagar", id));
    }

    /**
     * Copia os dados da requisicao para a entidade.
     *
     * Buscar fornecedor e categoria pelos servicos, e nao pelos repositorios,
     * garante o 404 com a mensagem certa quando o id nao existe.
     */
    private void aplicar(ContaPagar conta, ContaPagarDto.Requisicao requisicao) {
        conta.setDescricao(requisicao.descricao());
        conta.setNumeroDocumento(requisicao.numeroDocumento());
        conta.setFornecedor(pessoaServico.buscarEntidade(requisicao.fornecedorId()));
        conta.setCategoria(categoriaServico.buscarEntidade(requisicao.categoriaId()));
        conta.setValor(requisicao.valor());
        conta.setDataEmissao(requisicao.dataEmissao());
        conta.setDataVencimento(requisicao.dataVencimento());
        conta.setObservacao(requisicao.observacao());
    }
}
