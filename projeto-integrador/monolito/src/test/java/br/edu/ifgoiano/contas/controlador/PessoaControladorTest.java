package br.edu.ifgoiano.contas.controlador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import br.edu.ifgoiano.contas.modelo.Categoria;
import br.edu.ifgoiano.contas.modelo.Pessoa;
import br.edu.ifgoiano.contas.modelo.TipoPessoa;
import br.edu.ifgoiano.contas.repositorio.CategoriaRepositorio;
import br.edu.ifgoiano.contas.repositorio.ContaPagarRepositorio;
import br.edu.ifgoiano.contas.repositorio.PessoaRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PessoaControladorTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PessoaRepositorio pessoaRepositorio;
    @Autowired
    private CategoriaRepositorio categoriaRepositorio;
    @Autowired
    private ContaPagarRepositorio contaPagarRepositorio;
    @Autowired
    private br.edu.ifgoiano.contas.repositorio.ContaReceberRepositorio contaReceberRepositorio;

    private static final String PESSOA_VALIDA = """
            {"nome":"Empresa Exemplo","documento":"33.333.333/0001-33","tipo":"FORNECEDOR",
             "email":"contato@exemplo.com.br","telefone":"64 3333-0000"}
            """;

    @BeforeEach
    void limpar() {
        // As duas tabelas de conta saem antes das pessoas: a chave estrangeira
        // impede apagar uma pessoa que ainda tenha conta apontando para ela.
        contaPagarRepositorio.deleteAll();
        contaReceberRepositorio.deleteAll();
        pessoaRepositorio.deleteAll();
        categoriaRepositorio.deleteAll();
    }

    @Test
    @DisplayName("Cenario: cadastrar pessoa valida | Esperado: 201 com id gerado")
    void cadastrarPessoa() throws Exception {
        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON).content(PESSOA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Empresa Exemplo"));
    }

    @Test
    @DisplayName("Cenario: OPERACAO DUPLICADA, mesmo documento duas vezes | Esperado: 409")
    void documentoDuplicadoRetorna409() throws Exception {
        mockMvc.perform(post("/api/pessoas")
                .contentType(MediaType.APPLICATION_JSON).content(PESSOA_VALIDA));

        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON).content(PESSOA_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Cenario: nome vazio e e-mail invalido | Esperado: 400 com os dois campos")
    void entradaInvalidaRetorna400() throws Exception {
        String invalida = """
                {"nome":"","documento":"123","tipo":"CLIENTE","email":"nao-e-email"}
                """;

        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
    }

    @Test
    @DisplayName("Cenario: tipo inexistente no enum | Esperado: 400")
    void tipoInvalidoRetorna400() throws Exception {
        String invalida = """
                {"nome":"Teste","documento":"44.444.444/0001-44","tipo":"SOCIO"}
                """;

        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cenario: buscar, alterar e excluir pessoa sem vinculo | Esperado: 200, 200 e 204")
    void cicloCompleto() throws Exception {
        String criada = mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON).content(PESSOA_VALIDA))
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(criada, "$.id")).longValue();

        mockMvc.perform(get("/api/pessoas/" + id)).andExpect(status().isOk());

        String alterada = """
                {"nome":"Empresa Exemplo LTDA","documento":"33.333.333/0001-33","tipo":"AMBOS"}
                """;
        mockMvc.perform(put("/api/pessoas/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(alterada))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("AMBOS"));

        mockMvc.perform(delete("/api/pessoas/" + id)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Cenario: excluir pessoa com conta vinculada | Esperado: 409, integridade preservada")
    void excluirPessoaComContaRetorna409() throws Exception {
        Pessoa fornecedor = pessoaRepositorio.save(
                new Pessoa("Com Vinculo", "55.555.555/0001-55", TipoPessoa.FORNECEDOR));
        Categoria categoria = categoriaRepositorio.save(new Categoria("Servicos", null));

        String conta = """
                {"descricao":"Servico contratado","fornecedorId":%d,"categoriaId":%d,
                 "valor":300.00,"dataEmissao":"2026-10-01","dataVencimento":"2026-11-01"}
                """.formatted(fornecedor.getId(), categoria.getId());

        mockMvc.perform(post("/api/contas-pagar")
                .contentType(MediaType.APPLICATION_JSON).content(conta));

        mockMvc.perform(delete("/api/pessoas/" + fornecedor.getId()))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/pessoas/" + fornecedor.getId())).andExpect(status().isOk());
    }
}
