package br.edu.ifgoiano.contas.servico;

import br.edu.ifgoiano.contas.dto.ContaReceberDto;
import br.edu.ifgoiano.contas.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.contas.excecao.RegraDeNegocioException;
import br.edu.ifgoiano.contas.modelo.ContaReceber;
import br.edu.ifgoiano.contas.modelo.FormaPagamento;
import br.edu.ifgoiano.contas.modelo.Situacao;
import br.edu.ifgoiano.contas.repositorio.ContaReceberRepositorio;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaReceberServico {

    private final ContaReceberRepositorio repositorio;
    private final PessoaServico pessoaServico;
    private final CategoriaServico categoriaServico;
    private final FormaPagamentoServico formaPagamentoServico;

    public ContaReceberServico(ContaReceberRepositorio repositorio,
                               PessoaServico pessoaServico,
                               CategoriaServico categoriaServico,
                               FormaPagamentoServico formaPagamentoServico) {
        this.repositorio = repositorio;
        this.pessoaServico = pessoaServico;
        this.categoriaServico = categoriaServico;
        this.formaPagamentoServico = formaPagamentoServico;
    }

    @Transactional
    public ContaReceberDto.Resposta criar(ContaReceberDto.Requisicao requisicao) {
        ContaReceber conta = new ContaReceber();
        aplicar(conta, requisicao);
        conta.setSituacao(Situacao.ABERTA);
        return ContaReceberDto.Resposta.de(repositorio.save(conta));
    }

    /** Mesma traducao de situacao da conta a pagar: VENCIDA e derivada, nao gravada. */
    @Transactional(readOnly = true)
    public Page<ContaReceberDto.Resposta> buscar(Situacao situacao, LocalDate de, LocalDate ate,
                                                 Long clienteId, Long categoriaId, Pageable paginacao) {
        LocalDate hoje = LocalDate.now();
        Situacao situacaoNoBanco = situacao;
        LocalDate deEfetivo = de;
        LocalDate ateEfetivo = ate;

        if (situacao == Situacao.VENCIDA) {
            situacaoNoBanco = Situacao.ABERTA;
            LocalDate ontem = hoje.minusDays(1);
            ateEfetivo = (ate == null || ate.isAfter(ontem)) ? ontem : ate;
        } else if (situacao == Situacao.ABERTA) {
            deEfetivo = (de == null || de.isBefore(hoje)) ? hoje : de;
        } else if (situacao == Situacao.PAGA) {
            throw new RegraDeNegocioException("A situacao PAGA nao se aplica a contas a receber. Use RECEBIDA.");
        }

        return repositorio.buscarComFiltros(situacaoNoBanco, deEfetivo, ateEfetivo,
                clienteId, categoriaId, paginacao).map(ContaReceberDto.Resposta::de);
    }

    @Transactional(readOnly = true)
    public ContaReceberDto.Resposta buscarPorId(Long id) {
        return ContaReceberDto.Resposta.de(buscarEntidade(id));
    }

    @Transactional
    public ContaReceberDto.Resposta atualizar(Long id, ContaReceberDto.Requisicao requisicao) {
        ContaReceber conta = buscarEntidade(id);

        if (conta.estaQuitada()) {
            throw new RegraDeNegocioException("A conta ja foi recebida e nao pode ser alterada.");
        }
        if (conta.estaCancelada()) {
            throw new RegraDeNegocioException("A conta esta cancelada e nao pode ser alterada.");
        }

        aplicar(conta, requisicao);
        return ContaReceberDto.Resposta.de(conta);
    }

    /** Registro do recebimento, um dos requisitos minimos do enunciado. */
    @Transactional
    public ContaReceberDto.Resposta receber(Long id, ContaReceberDto.Recebimento recebimento) {
        ContaReceber conta = buscarEntidade(id);

        if (conta.estaQuitada()) {
            throw new RegraDeNegocioException("A conta ja foi recebida em " + conta.getDataRecebimento() + ".");
        }
        if (conta.estaCancelada()) {
            throw new RegraDeNegocioException("A conta esta cancelada e nao pode ser recebida.");
        }
        if (recebimento.dataRecebimento().isBefore(conta.getDataEmissao())) {
            throw new RegraDeNegocioException("A data do recebimento nao pode ser anterior a emissao da conta.");
        }

        FormaPagamento forma = formaPagamentoServico.buscarEntidade(recebimento.formaRecebimentoId());
        conta.registrarRecebimento(recebimento.dataRecebimento(), recebimento.valorRecebido(), forma);
        return ContaReceberDto.Resposta.de(conta);
    }

    @Transactional
    public ContaReceberDto.Resposta cancelar(Long id) {
        ContaReceber conta = buscarEntidade(id);

        if (conta.estaQuitada()) {
            throw new RegraDeNegocioException("A conta ja foi recebida e nao pode ser cancelada.");
        }
        if (conta.estaCancelada()) {
            throw new RegraDeNegocioException("A conta ja esta cancelada.");
        }

        conta.cancelar();
        return ContaReceberDto.Resposta.de(conta);
    }

    @Transactional
    public void excluir(Long id) {
        ContaReceber conta = buscarEntidade(id);

        if (conta.estaQuitada()) {
            throw new RegraDeNegocioException("A conta ja foi recebida e nao pode ser excluida.");
        }

        repositorio.delete(conta);
    }

    private ContaReceber buscarEntidade(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Conta a receber", id));
    }

    private void aplicar(ContaReceber conta, ContaReceberDto.Requisicao requisicao) {
        conta.setDescricao(requisicao.descricao());
        conta.setNumeroDocumento(requisicao.numeroDocumento());
        conta.setCliente(pessoaServico.buscarEntidade(requisicao.clienteId()));
        conta.setCategoria(categoriaServico.buscarEntidade(requisicao.categoriaId()));
        conta.setValor(requisicao.valor());
        conta.setDataEmissao(requisicao.dataEmissao());
        conta.setDataVencimento(requisicao.dataVencimento());
        conta.setObservacao(requisicao.observacao());
    }
}
