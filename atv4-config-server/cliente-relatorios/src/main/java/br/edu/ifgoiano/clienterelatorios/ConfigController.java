package br.edu.ifgoiano.clienterelatorios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {

    // O valor depois dos dois-pontos e o padrao: so aparece se a propriedade
    // nao chegar do servidor de configuracao.
    @Value("${mensagem.boasvindas:Configuracao nao carregada}")
    private String mensagem;

    @GetMapping("/mensagem")
    public String mensagem() {
        return this.mensagem;
    }
}
