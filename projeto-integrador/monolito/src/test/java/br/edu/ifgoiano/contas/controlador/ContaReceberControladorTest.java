package br.edu.ifgoiano.contas.controlador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import br.edu.ifgoiano.contas.modelo.Categoria;
import br.edu.ifgoiano.contas.modelo.FormaPagamento;
import br.edu.ifgoiano.contas.modelo.Pessoa;
import br.edu.ifgoiano.contas.modelo.TipoPessoa;
import br.edu.ifgoiano.contas.repositorio.CategoriaRepositorio;
import br.edu.ifgoiano.contas.repositorio.ContaReceberRepositorio;
import br.edu.ifgoiano.contas.repositorio.FormaPagamentoRepositorio;
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
class ContaReceberControladorTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PessoaRepositorio pessoaRepositorio;
    @Autowired
    private CategoriaRepositorio categoriaRepositorio;
    @Autowired
    private FormaPagamentoRepositorio formaPagamentoRepositorio;
    @Autowired
    private ContaReceberRepositorio contaReceberRepositorio;
    @Autowired
    private br.edu.ifgoiano.contas.repositorio.ContaPagarRepositorio contaPagarRepositorio;

    private Long clienteId;
    private Long categoriaId;
    private Long formaId;

    @BeforeEach
    void prepararDados() {
        contaReceberRepositorio.deleteAll();
        contaPagarRepositorio.deleteAll();
        pessoaRepositorio.deleteAll();
        categoriaRepositorio.deleteAll();
        formaPagamentoRepositorio.deleteAll();

        clienteId = pessoaRepositorio.save(
                new Pessoa("Cliente Teste", "22.222.222/0001-22", TipoPessoa.CLIENTE)).getId();
        categoriaId = categoriaRepositorio.save(new Categoria("Vendas", null)).getId();
        formaId = formaPagamentoRepositorio.save(new FormaPagamento("Transferencia")).getId();
    }

    private Long criarConta() throws Exception {
        String corpo = """
                {"descricao":"Venda de produtos","clienteId":%d,"categoriaId":%d,
                 "valor":1200.00,"dataEmissao":"2026-10-01","dataVencimento":"2026-11-01"}
                """.formatted(clienteId, categoriaId);

        String resposta = mockMvc.perform(post("/api/contas-receber")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    @Test
    @DisplayName("Cenario: criar conta a receber | Esperado: 201 e situacao ABERTA")
    void criarContaReceber() throws Exception {
        String corpo = """
                {"descricao":"Venda de produtos","clienteId":%d,"categoriaId":%d,
                 "valor":1200.00,"dataEmissao":"2026-10-01","dataVencimento":"2026-11-01"}
                """.formatted(clienteId, categoriaId);

        mockMvc.perform(post("/api/contas-receber")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("ABERTA"))
                .andExpect(jsonPath("$.cliente.nome").value("Cliente Teste"));
    }

    @Test
    @DisplayName("Cenario: registrar recebimento | Esperado: 200 e situacao RECEBIDA")
    void registrarRecebimento() throws Exception {
        Long id = criarConta();
        String recebimento = """
                {"dataRecebimento":"2026-10-20","valorRecebido":1200.00,"formaRecebimentoId":%d}
                """.formatted(formaId);

        mockMvc.perform(post("/api/contas-receber/" + id + "/receber")
                        .contentType(MediaType.APPLICATION_JSON).content(recebimento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("RECEBIDA"))
                .andExpect(jsonPath("$.valorRecebido").value(1200.00));
    }

    @Test
    @DisplayName("Cenario: OPERACAO DUPLICADA, receber duas vezes | Esperado: 409")
    void receberDuasVezesRetorna409() throws Exception {
        Long id = criarConta();
        String recebimento = """
                {"dataRecebimento":"2026-10-20","valorRecebido":1200.00,"formaRecebimentoId":%d}
                """.formatted(formaId);

        mockMvc.perform(post("/api/contas-receber/" + id + "/receber")
                .contentType(MediaType.APPLICATION_JSON).content(recebimento));

        mockMvc.perform(post("/api/contas-receber/" + id + "/receber")
                        .contentType(MediaType.APPLICATION_JSON).content(recebimento))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cenario: recebimento anterior a emissao | Esperado: 409")
    void recebimentoAnteriorAEmissaoRetorna409() throws Exception {
        Long id = criarConta();
        String recebimento = """
                {"dataRecebimento":"2026-09-01","valorRecebido":1200.00,"formaRecebimentoId":%d}
                """.formatted(formaId);

        mockMvc.perform(post("/api/contas-receber/" + id + "/receber")
                        .contentType(MediaType.APPLICATION_JSON).content(recebimento))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cenario: filtrar conta a receber por situacao PAGA | Esperado: 409, situacao de outro recurso")
    void situacaoPagaNaoSeAplicaRetorna409() throws Exception {
        mockMvc.perform(get("/api/contas-receber").param("situacao", "PAGA"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cenario: consulta por situacao RECEBIDA | Esperado: so a conta recebida")
    void consultaPorSituacaoRecebida() throws Exception {
        Long recebida = criarConta();
        criarConta();

        mockMvc.perform(post("/api/contas-receber/" + recebida + "/receber")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"dataRecebimento":"2026-10-20","valorRecebido":1200.00,"formaRecebimentoId":%d}
                        """.formatted(formaId)));

        mockMvc.perform(get("/api/contas-receber").param("situacao", "RECEBIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
