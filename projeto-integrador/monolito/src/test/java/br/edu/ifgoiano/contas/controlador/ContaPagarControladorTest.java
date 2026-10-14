package br.edu.ifgoiano.contas.controlador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import br.edu.ifgoiano.contas.modelo.Categoria;
import br.edu.ifgoiano.contas.modelo.FormaPagamento;
import br.edu.ifgoiano.contas.modelo.Pessoa;
import br.edu.ifgoiano.contas.modelo.TipoPessoa;
import br.edu.ifgoiano.contas.repositorio.CategoriaRepositorio;
import br.edu.ifgoiano.contas.repositorio.ContaPagarRepositorio;
import br.edu.ifgoiano.contas.repositorio.FormaPagamentoRepositorio;
import br.edu.ifgoiano.contas.repositorio.PessoaRepositorio;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes dos endpoints de conta a pagar.
 *
 * Sobe a aplicacao inteira e chama os endpoints pelo MockMvc, cobrindo de uma
 * vez tres exigencias do enunciado: teste de controlador, teste de integracao
 * com o banco e teste das regras de negocio como o cliente da API as enxerga.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContaPagarControladorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepositorio pessoaRepositorio;
    @Autowired
    private CategoriaRepositorio categoriaRepositorio;
    @Autowired
    private FormaPagamentoRepositorio formaPagamentoRepositorio;
    @Autowired
    private ContaPagarRepositorio contaPagarRepositorio;
    @Autowired
    private br.edu.ifgoiano.contas.repositorio.ContaReceberRepositorio contaReceberRepositorio;

    private Long fornecedorId;
    private Long categoriaId;
    private Long formaId;

    @BeforeEach
    void prepararDados() {
        // Limpa antes de cada teste: nenhum teste pode depender do que outro deixou.
        // As contas saem antes das pessoas por causa da chave estrangeira, e as duas
        // tabelas de conta sao limpas porque o banco em memoria e compartilhado
        // entre as classes de teste que rodam na mesma JVM.
        contaPagarRepositorio.deleteAll();
        contaReceberRepositorio.deleteAll();
        pessoaRepositorio.deleteAll();
        categoriaRepositorio.deleteAll();
        formaPagamentoRepositorio.deleteAll();

        fornecedorId = pessoaRepositorio.save(
                new Pessoa("Fornecedor Teste", "11.111.111/0001-11", TipoPessoa.FORNECEDOR)).getId();
        categoriaId = categoriaRepositorio.save(new Categoria("Despesas gerais", null)).getId();
        formaId = formaPagamentoRepositorio.save(new FormaPagamento("Boleto")).getId();
    }

    private String contaValida() {
        return """
                {"descricao":"Compra de material","fornecedorId":%d,"categoriaId":%d,
                 "valor":500.00,"dataEmissao":"2026-10-01","dataVencimento":"2026-11-01"}
                """.formatted(fornecedorId, categoriaId);
    }

    private Long criarConta() throws Exception {
        String corpo = mockMvc.perform(post("/api/contas-pagar")
                        .contentType(MediaType.APPLICATION_JSON).content(contaValida()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    @Test
    @DisplayName("Cenario: criar conta valida | Esperado: 201 e situacao ABERTA")
    void criarContaValida() throws Exception {
        mockMvc.perform(post("/api/contas-pagar")
                        .contentType(MediaType.APPLICATION_JSON).content(contaValida()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.situacao").value("ABERTA"))
                .andExpect(jsonPath("$.fornecedor.nome").value("Fornecedor Teste"));
    }

    @Test
    @DisplayName("Cenario: valor zero e descricao vazia | Esperado: 400 com a lista de campos")
    void entradaInvalidaRetorna400() throws Exception {
        String invalida = """
                {"descricao":"","fornecedorId":%d,"categoriaId":%d,
                 "valor":0,"dataEmissao":"2026-10-01","dataVencimento":"2026-11-01"}
                """.formatted(fornecedorId, categoriaId);

        mockMvc.perform(post("/api/contas-pagar")
                        .contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    @DisplayName("Cenario: vencimento anterior a emissao | Esperado: 400")
    void vencimentoAnteriorAEmissaoRetorna400() throws Exception {
        String invalida = """
                {"descricao":"Conta torta","fornecedorId":%d,"categoriaId":%d,
                 "valor":100.00,"dataEmissao":"2026-10-10","dataVencimento":"2026-10-01"}
                """.formatted(fornecedorId, categoriaId);

        mockMvc.perform(post("/api/contas-pagar")
                        .contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cenario: fornecedor inexistente | Esperado: 404")
    void fornecedorInexistenteRetorna404() throws Exception {
        String corpo = """
                {"descricao":"Teste","fornecedorId":99999,"categoriaId":%d,
                 "valor":100.00,"dataEmissao":"2026-10-01","dataVencimento":"2026-11-01"}
                """.formatted(categoriaId);

        mockMvc.perform(post("/api/contas-pagar")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Cenario: buscar id inexistente | Esperado: 404")
    void buscarInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/contas-pagar/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cenario: registrar pagamento | Esperado: 200 e situacao PAGA")
    void registrarPagamento() throws Exception {
        Long id = criarConta();
        String pagamento = """
                {"dataPagamento":"2026-10-15","valorPago":500.00,"formaPagamentoId":%d}
                """.formatted(formaId);

        mockMvc.perform(post("/api/contas-pagar/" + id + "/pagar")
                        .contentType(MediaType.APPLICATION_JSON).content(pagamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PAGA"))
                .andExpect(jsonPath("$.valorPago").value(500.00))
                .andExpect(jsonPath("$.formaPagamento.nome").value("Boleto"));
    }

    @Test
    @DisplayName("Cenario: OPERACAO DUPLICADA, pagar a mesma conta duas vezes | Esperado: 409")
    void pagarDuasVezesRetorna409() throws Exception {
        Long id = criarConta();
        String pagamento = """
                {"dataPagamento":"2026-10-15","valorPago":500.00,"formaPagamentoId":%d}
                """.formatted(formaId);

        mockMvc.perform(post("/api/contas-pagar/" + id + "/pagar")
                .contentType(MediaType.APPLICATION_JSON).content(pagamento));

        mockMvc.perform(post("/api/contas-pagar/" + id + "/pagar")
                        .contentType(MediaType.APPLICATION_JSON).content(pagamento))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Cenario: alterar conta ja paga | Esperado: 409")
    void alterarContaPagaRetorna409() throws Exception {
        Long id = criarConta();
        mockMvc.perform(post("/api/contas-pagar/" + id + "/pagar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"dataPagamento":"2026-10-15","valorPago":500.00,"formaPagamentoId":%d}
                        """.formatted(formaId)));

        mockMvc.perform(put("/api/contas-pagar/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(contaValida()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cenario: excluir conta ja paga | Esperado: 409 e a conta permanece")
    void excluirContaPagaRetorna409() throws Exception {
        Long id = criarConta();
        mockMvc.perform(post("/api/contas-pagar/" + id + "/pagar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"dataPagamento":"2026-10-15","valorPago":500.00,"formaPagamentoId":%d}
                        """.formatted(formaId)));

        mockMvc.perform(delete("/api/contas-pagar/" + id))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/contas-pagar/" + id)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cenario: excluir conta aberta | Esperado: 204 e depois 404")
    void excluirContaAberta() throws Exception {
        Long id = criarConta();

        mockMvc.perform(delete("/api/contas-pagar/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/contas-pagar/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cenario: cancelar conta e tentar cancelar de novo | Esperado: 200 e depois 409")
    void cancelarDuasVezes() throws Exception {
        Long id = criarConta();

        mockMvc.perform(post("/api/contas-pagar/" + id + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADA"));

        mockMvc.perform(post("/api/contas-pagar/" + id + "/cancelar"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cenario: consulta por situacao PAGA | Esperado: 200 e so a conta paga")
    void consultaPorSituacao() throws Exception {
        Long paga = criarConta();
        criarConta();

        mockMvc.perform(post("/api/contas-pagar/" + paga + "/pagar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"dataPagamento":"2026-10-15","valorPago":500.00,"formaPagamentoId":%d}
                        """.formatted(formaId)));

        mockMvc.perform(get("/api/contas-pagar").param("situacao", "PAGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].situacao").value("PAGA"));
    }

    @Test
    @DisplayName("Cenario: consulta por periodo de vencimento | Esperado: so as contas do intervalo")
    void consultaPorPeriodo() throws Exception {
        criarConta();

        mockMvc.perform(get("/api/contas-pagar")
                        .param("de", "2026-10-25").param("ate", "2026-11-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/contas-pagar")
                        .param("de", "2026-01-01").param("ate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Cenario: parametro de data invalido na URL | Esperado: 400")
    void dataInvalidaNaUrlRetorna400() throws Exception {
        mockMvc.perform(get("/api/contas-pagar").param("de", "ontem"))
                .andExpect(status().isBadRequest());
    }
}
