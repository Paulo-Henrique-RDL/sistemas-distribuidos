package br.edu.ifgoiano.cliente.servico;

import br.edu.ifgoiano.cliente.dto.ClienteDto;
import br.edu.ifgoiano.cliente.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.cliente.modelo.Cliente;
import br.edu.ifgoiano.cliente.repositorio.ClienteRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServico {

    private final ClienteRepositorio repositorio;

    public ClienteServico(ClienteRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<ClienteDto.Resposta> listar() {
        return repositorio.findAll().stream().map(ClienteDto.Resposta::de).toList();
    }

    @Transactional(readOnly = true)
    public ClienteDto.Resposta buscarPorId(Long id) {
        return ClienteDto.Resposta.de(repositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Cliente", id)));
    }

    @Transactional
    public ClienteDto.Resposta criar(ClienteDto.Requisicao requisicao) {
        Cliente cliente = new Cliente();
        cliente.setNome(requisicao.nome());
        cliente.setDocumento(requisicao.documento());
        cliente.setEmail(requisicao.email());
        cliente.setCidade(requisicao.cidade());
        cliente.setUf(requisicao.uf());
        return ClienteDto.Resposta.de(repositorio.save(cliente));
    }
}
