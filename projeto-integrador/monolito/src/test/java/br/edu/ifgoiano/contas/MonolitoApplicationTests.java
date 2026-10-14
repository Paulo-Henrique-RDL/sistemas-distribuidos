package br.edu.ifgoiano.contas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Cenario: subir a aplicacao inteira.
 * Dados: nenhum.
 * Resultado esperado: o contexto do Spring carrega sem erro, o que prova que
 * todas as camadas estao ligadas corretamente e o mapeamento JPA e valido.
 */
@SpringBootTest
@ActiveProfiles("test")
class MonolitoApplicationTests {

    @Test
    @DisplayName("O contexto da aplicacao carrega")
    void contextLoads() {
    }
}
