package br.edu.ifgoiano.pedido.integracao;

import br.edu.ifgoiano.pedido.excecao.RecursoNaoEncontradoException;
import br.edu.ifgoiano.pedido.excecao.ServicoIndisponivelException;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Porta de saida para o catalogo-service. Mesma estrutura do ClienteClient. */
@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(RestClient.Builder builder,
                          @Value("${servicos.catalogo.url}") String url,
                          @Value("${servicos.timeout-segundos}") int timeoutSegundos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(timeoutSegundos));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));

        this.restClient = builder.baseUrl(url).requestFactory(fabrica).build();
    }

    public ProdutoResumo buscarPorId(Long id) {
        try {
            return restClient.get()
                    .uri("/produtos/{id}", id)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (requisicao, resposta) -> {
                        throw RecursoNaoEncontradoException.de("Produto", id);
                    })
                    .body(ProdutoResumo.class);

        } catch (RestClientException e) {
            throw new ServicoIndisponivelException("catalogo-service", e);
        }
    }

    public record ProdutoResumo(Long id, String sku, String nome, BigDecimal preco, Integer estoque) {
    }
}
