package br.edu.ifgoiano.pedido.integracao;

import br.edu.ifgoiano.pedido.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.pedido.excecao.ServicoIndisponivelException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Porta de saida para o cliente-service.
 *
 * Isolar a chamada HTTP numa classe so tem um motivo pratico: o servico de
 * pedidos passa a depender de um metodo Java, nao de uma URL. Trocar o
 * endereco, o protocolo ou a forma de descobrir o servico mexe aqui e em
 * nenhum outro lugar.
 *
 * Usamos RestClient, e nao RestTemplate: desde o Spring 6.1 o RestTemplate
 * esta em modo de manutencao e a documentacao recomenda o RestClient para
 * codigo novo. A ideia e a mesma, a API e mais direta.
 */
@Component
public class ClienteClient {

    private final RestClient restClient;

    public ClienteClient(RestClient.Builder builder,
                         @Value("${servicos.cliente.url}") String url,
                         @Value("${servicos.timeout-segundos}") int timeoutSegundos) {
        // Timeout explicito: sem ele, um servico travado prenderia a thread do
        // pedido indefinidamente, e a lentidao de um derrubaria o outro.
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(timeoutSegundos));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));

        this.restClient = builder.baseUrl(url).requestFactory(fabrica).build();
    }

    public ClienteResumo buscarPorId(Long id) {
        try {
            return restClient.get()
                    .uri("/clientes/{id}", id)
                    .retrieve()
                    // 404 la vira 404 aqui, com a mensagem certa. Sem isso, a
                    // falta de um cliente apareceria como erro 500 generico.
                    .onStatus(status -> status.value() == 404, (requisicao, resposta) -> {
                        throw RecursoNaoEncontradoException.de("Cliente", id);
                    })
                    .body(ClienteResumo.class);

        } catch (RestClientException e) {
            // Conexao recusada, timeout ou 5xx do outro lado.
            throw new ServicoIndisponivelException("cliente-service", e);
        }
    }

    /**
     * So os campos que o pedido usa.
     *
     * Nao copiamos o DTO inteiro do cliente-service de proposito: quanto menos
     * campos conhecemos, menos chance de quebrar quando o outro time mudar algo
     * que nao nos interessa.
     */
    public record ClienteResumo(Long id, String nome, String documento, String email) {
    }
}
