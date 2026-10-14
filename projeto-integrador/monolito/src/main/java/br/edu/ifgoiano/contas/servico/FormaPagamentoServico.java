package br.edu.ifgoiano.contas.servico;

import br.edu.ifgoiano.contas.dto.FormaPagamentoDto;
import br.edu.ifgoiano.contas.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.contas.excecao.RegraDeNegocioException;
import br.edu.ifgoiano.contas.modelo.FormaPagamento;
import br.edu.ifgoiano.contas.repositorio.ContaPagarRepositorio;
import br.edu.ifgoiano.contas.repositorio.ContaReceberRepositorio;
import br.edu.ifgoiano.contas.repositorio.FormaPagamentoRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormaPagamentoServico {

    private final FormaPagamentoRepositorio formaPagamentoRepositorio;
    private final ContaPagarRepositorio contaPagarRepositorio;
    private final ContaReceberRepositorio contaReceberRepositorio;

    public FormaPagamentoServico(FormaPagamentoRepositorio formaPagamentoRepositorio,
                                 ContaPagarRepositorio contaPagarRepositorio,
                                 ContaReceberRepositorio contaReceberRepositorio) {
        this.formaPagamentoRepositorio = formaPagamentoRepositorio;
        this.contaPagarRepositorio = contaPagarRepositorio;
        this.contaReceberRepositorio = contaReceberRepositorio;
    }

    @Transactional
    public FormaPagamentoDto.Resposta criar(FormaPagamentoDto.Requisicao requisicao) {
        if (formaPagamentoRepositorio.existsByNomeIgnoreCase(requisicao.nome())) {
            throw new RegraDeNegocioException("Ja existe a forma de pagamento " + requisicao.nome() + ".");
        }

        return FormaPagamentoDto.Resposta.de(formaPagamentoRepositorio.save(new FormaPagamento(requisicao.nome())));
    }

    @Transactional(readOnly = true)
    public Page<FormaPagamentoDto.Resposta> listar(Pageable paginacao) {
        return formaPagamentoRepositorio.findAll(paginacao).map(FormaPagamentoDto.Resposta::de);
    }

    @Transactional(readOnly = true)
    public FormaPagamentoDto.Resposta buscarPorId(Long id) {
        return FormaPagamentoDto.Resposta.de(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public FormaPagamento buscarEntidade(Long id) {
        return formaPagamentoRepositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Forma de pagamento", id));
    }

    @Transactional
    public FormaPagamentoDto.Resposta atualizar(Long id, FormaPagamentoDto.Requisicao requisicao) {
        FormaPagamento forma = buscarEntidade(id);

        if (formaPagamentoRepositorio.existsByNomeIgnoreCaseAndIdNot(requisicao.nome(), id)) {
            throw new RegraDeNegocioException("O nome " + requisicao.nome() + " ja pertence a outra forma de pagamento.");
        }

        forma.setNome(requisicao.nome());
        return FormaPagamentoDto.Resposta.de(forma);
    }

    @Transactional
    public void excluir(Long id) {
        FormaPagamento forma = buscarEntidade(id);

        if (contaPagarRepositorio.existsByFormaPagamentoId(id)
                || contaReceberRepositorio.existsByFormaRecebimentoId(id)) {
            throw new RegraDeNegocioException(
                    "A forma de pagamento ja foi usada em quitacoes e nao pode ser excluida.");
        }

        formaPagamentoRepositorio.delete(forma);
    }
}
