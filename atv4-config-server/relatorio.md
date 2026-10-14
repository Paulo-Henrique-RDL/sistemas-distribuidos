# Relatório — Configuração Centralizada com Spring Cloud Config

**Disciplina:** Sistemas Distribuídos · **Integrante:** Paulo Henrique Rodrigues de Lima

Quatro aplicações Spring Boot: um **Config Server** na porta 8888 e três clientes
— `cliente-vendas`, `cliente-estoque` e `cliente-relatorios`. As configurações
não moram em nenhuma das quatro: ficam no repositório Git
[`atv4-repo-configs`](https://github.com/Paulo-Henrique-RDL/atv4-repo-configs),
que o servidor clona e serve.

## 1. Como cada serviço se conecta ao servidor

O cliente não tem código de conexão. Ele declara a dependência
`spring-cloud-starter-config` e uma propriedade:

```properties
spring.config.import=optional:configserver:http://localhost:8888
```

Isso basta porque `spring.config.import` é um ponto de extensão do próprio
Spring Boot: ao encontrar o prefixo `configserver:`, ele aciona o
`ConfigServerConfigDataLoader`, que faz uma requisição HTTP ao endereço
indicado **antes** de o contexto da aplicação existir. O resultado entra na lista
de fontes de propriedades com prioridade acima do `application.properties` local.

Qual arquivo o servidor deve devolver é decidido por duas outras propriedades do
cliente: `spring.application.name` e `spring.profiles.active`. O servidor as
recebe na URL (`/{nome}/{perfil}`) e as usa para localizar
`{nome}-{perfil}.properties` no repositório Git.

Do lado do servidor, a conexão é só a anotação `@EnableConfigServer`, que publica
os endpoints REST de configuração, e a URL do repositório no
`application.properties`.

## 2. Função de cada propriedade

### Config Server

| Propriedade | Função |
| --- | --- |
| `spring.application.name=config-server` | Nome da aplicação nos logs e no Actuator |
| `server.port=8888` | Porta fixa; é o endereço que os clientes conhecem |
| `spring.cloud.config.server.git.uri=...` | Repositório Git de onde saem as configurações |
| `spring.cloud.config.server.git.clone-on-start=true` | Clona na inicialização, e não na primeira requisição: uma falha de acesso aparece ao subir, não depois |
| `management.endpoints.web.exposure.include=*` | Expõe os endpoints do Actuator, entre eles `/actuator/health` |

### Clientes

| Propriedade | Função |
| --- | --- |
| `spring.application.name=cliente-vendas` | Primeira parte do nome do arquivo pedido ao servidor |
| `spring.profiles.active=dev` | Segunda parte — junto com a anterior, resolve `cliente-vendas-dev.properties` |
| `spring.config.import=optional:configserver:http://localhost:8888` | Onde buscar. O prefixo `optional:` evita que a aplicação morra se o servidor estiver fora do ar |
| `management.endpoints.web.exposure.include=*` | Expõe os endpoints do Actuator |

Nenhum cliente declara `server.port` nem `mensagem.boasvindas`: as duas vêm do
repositório central. É justamente por isso que a porta serve de prova — se o
cliente subiu na 8081, a configuração remota foi aplicada.

## 3. Fluxo de inicialização

1. O **Config Server** sobe e clona o repositório Git (`clone-on-start=true`).
2. Um **cliente** inicia e lê o seu `application.properties` local, descobrindo
   seu nome (`cliente-vendas`), seu perfil (`dev`) e o endereço do servidor.
3. Ainda na fase de carga da configuração, o cliente chama
   `GET http://localhost:8888/cliente-vendas/dev`.
4. O servidor atualiza seu clone do repositório, localiza
   `cliente-vendas-dev.properties` e devolve o conteúdo em JSON.
5. O cliente insere essas propriedades no `Environment` **acima** das locais.
6. Só então o Spring Boot cria o contexto: o Tomcat sobe na porta 8081, porque
   `server.port` já estava resolvido, e `@Value("${mensagem.boasvindas}")`
   injeta o texto no `ConfigController`.

A ordem importa: o servidor precisa estar no ar antes dos clientes, senão o
passo 3 falha e o `optional:` faz cada cliente cair nos valores padrão.

## 4. Testes realizados

**Servidor** — `http://localhost:8888/actuator/health` respondeu
`{"status":"UP"}`, e as três consultas de configuração devolveram o JSON
esperado, com o SHA do commit do repositório no campo `version`:

| URL | Retorno |
| --- | --- |
| `/cliente-vendas/dev` | `mensagem.boasvindas` e `server.port=8081` |
| `/cliente-estoque/dev` | `mensagem.boasvindas` e `server.port=8082` |
| `/cliente-relatorios/dev` | `mensagem.boasvindas` e `server.port=8083` |

**Clientes** — cada um subiu na porta vinda do repositório e devolveu seu texto:

| URL | Resposta |
| --- | --- |
| `http://localhost:8081/mensagem` | `Bem-vindo ao sistema de Vendas - Ambiente DEV` |
| `http://localhost:8082/mensagem` | `Serviço de Estoque inicializado - Ambiente DEV` |
| `http://localhost:8083/mensagem` | `Serviço de Relatórios em execução - Ambiente DEV` |

**Teste negativo** — com o Config Server desligado, `cliente-vendas` subiu mesmo
assim, mas na porta padrão 8080, e `/mensagem` respondeu
`Configuracao nao carregada`. É a confirmação de que os valores anteriores vinham
mesmo do servidor, e não de algum arquivo local.

**Versionamento** — um commit no repositório de configurações alterando uma
mensagem passou a ser refletido na consulta ao servidor sem reiniciá-lo: ele
consulta o Git a cada requisição. Os clientes, porém, leem a configuração apenas
na inicialização, então precisam ser reiniciados para receber o novo valor.

As saídas completas estão em `evidencias/`.

## 5. Dificuldade encontrada

As mensagens de estoque e relatórios têm acento. Gravadas em UTF-8, chegavam
corrompidas ao cliente (`ServiÃ§o`), porque arquivos `.properties` são lidos em
ISO-8859-1. Foram reescritas com escapes Unicode — `Servi\u00e7o` — que é a forma
canônica em Java e independe da codificação do arquivo.
