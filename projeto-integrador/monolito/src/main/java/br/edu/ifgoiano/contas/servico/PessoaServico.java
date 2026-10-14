package br.edu.ifgoiano.contas.servico;

import br.edu.ifgoiano.contas.dto.PessoaDto;
import br.edu.ifgoiano.contas.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.contas.excecao.RegraDeNegocioException;
import br.edu.ifgoiano.contas.modelo.Pessoa;
import br.edu.ifgoiano.contas.repositorio.ContaPagarRepositorio;
import br.edu.ifgoiano.contas.repositorio.ContaReceberRepositorio;
import br.edu.ifgoiano.contas.repositorio.PessoaRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio de pessoa.
 *
 * E o servico, e nao o controlador, que concentra as regras. O controlador
 * apenas traduz HTTP; assim a mesma regra vale se amanha a operacao for
 * chamada por uma fila ou por outro servico.
 */
@Service
public class PessoaServico {

    private final PessoaRepositorio pessoaRepositorio;
    private final ContaPagarRepositorio contaPagarRepositorio;
    private final ContaReceberRepositorio contaReceberRepositorio;

    // Injecao por construtor, nao por @Autowired em campo: deixa as dependencias
    // explicitas e permite instanciar a classe em teste sem subir o Spring.
    public PessoaServico(PessoaRepositorio pessoaRepositorio,
                         ContaPagarRepositorio contaPagarRepositorio,
                         ContaReceberRepositorio contaReceberRepositorio) {
        this.pessoaRepositorio = pessoaRepositorio;
        this.contaPagarRepositorio = contaPagarRepositorio;
        this.contaReceberRepositorio = contaReceberRepositorio;
    }

    @Transactional
    public PessoaDto.Resposta criar(PessoaDto.Requisicao requisicao) {
        // O documento identifica a pessoa, entao duplicata e barrada antes de gravar.
        if (pessoaRepositorio.existsByDocumento(requisicao.documento())) {
            throw new RegraDeNegocioException("Ja existe pessoa cadastrada com o documento " + requisicao.documento() + ".");
        }

        Pessoa pessoa = new Pessoa(requisicao.nome(), requisicao.documento(), requisicao.tipo());
        pessoa.setEmail(requisicao.email());
        pessoa.setTelefone(requisicao.telefone());

        return PessoaDto.Resposta.de(pessoaRepositorio.save(pessoa));
    }

    // readOnly avisa o banco que nao havera escrita: dispensa o controle de
    // alteracoes do Hibernate e deixa a consulta mais leve.
    @Transactional(readOnly = true)
    public Page<PessoaDto.Resposta> listar(String nome, Pageable paginacao) {
        Page<Pessoa> pagina = (nome == null || nome.isBlank())
                ? pessoaRepositorio.findAll(paginacao)
                : pessoaRepositorio.findByNomeContainingIgnoreCase(nome, paginacao);

        return pagina.map(PessoaDto.Resposta::de);
    }

    @Transactional(readOnly = true)
    public PessoaDto.Resposta buscarPorId(Long id) {
        return PessoaDto.Resposta.de(buscarEntidade(id));
    }

    /** Usado pelos servicos de conta, que precisam da entidade e nao do DTO. */
    @Transactional(readOnly = true)
    public Pessoa buscarEntidade(Long id) {
        return pessoaRepositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Pessoa", id));
    }

    @Transactional
    public PessoaDto.Resposta atualizar(Long id, PessoaDto.Requisicao requisicao) {
        Pessoa pessoa = buscarEntidade(id);

        // A checagem ignora a propria pessoa: manter o documento ao editar e valido.
        if (pessoaRepositorio.existsByDocumentoAndIdNot(requisicao.documento(), id)) {
            throw new RegraDeNegocioException("O documento " + requisicao.documento() + " ja pertence a outra pessoa.");
        }

        pessoa.setNome(requisicao.nome());
        pessoa.setDocumento(requisicao.documento());
        pessoa.setTipo(requisicao.tipo());
        pessoa.setEmail(requisicao.email());
        pessoa.setTelefone(requisicao.telefone());

        // Sem save explicito: dentro da transacao o Hibernate detecta a alteracao
        // e grava na saida do metodo.
        return PessoaDto.Resposta.de(pessoa);
    }

    /**
     * Pessoa com conta vinculada nao e excluida: apagar destruiria o historico
     * financeiro que o enunciado exige poder acompanhar ao longo do tempo.
     */
    @Transactional
    public void excluir(Long id) {
        Pessoa pessoa = buscarEntidade(id);

        if (contaPagarRepositorio.existsByFornecedorId(id) || contaReceberRepositorio.existsByClienteId(id)) {
            throw new RegraDeNegocioException("A pessoa possui contas vinculadas e nao pode ser excluida.");
        }

        pessoaRepositorio.delete(pessoa);
    }
}
