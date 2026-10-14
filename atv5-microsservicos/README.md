# ATV5 — Microsserviços com base de dados própria

> **Enunciado:** tema livre, no mínimo 3 microsserviços. Cada microsserviço deve ter sua própria base de dados. Pelo menos um deve ter no mínimo duas tabelas se relacionando; os demais, pelo menos uma tabela cada. O GitHub deve possuir o código completo, inclusive com os arquivos para popular o banco de dados para teste, e todos os pacotes necessários para a execução, inclusive *handle* e *Exceptions*.

**Integrante:** Paulo Henrique Rodrigues de Lima

Uma loja decomposta em três microsserviços. Cada um tem **seu próprio banco**, e
nenhum enxerga o banco do outro: quando o pedido precisa de um dado que não é
dele, ele **pergunta por HTTP**.

| Serviço | Porta | Banco | Tabelas |
| --- | --- | --- | --- |
| `cliente-service` | 8100 | `cliente-service/dados/clientes` | `cliente` |
| `catalogo-service` | 8200 | `catalogo-service/dados/catalogo` | `produto` |
| `pedido-service` | 8300 | `pedido-service/dados/pedidos` | **`pedido` 1—N `item_pedido`** |

## Requisitos

**JDK 21 ou superior.** Nada além disso: o Maven vem embutido (Maven Wrapper) e
os bancos são criados e populados sozinhos na primeira execução, pelo Flyway.

A primeira execução baixa as dependências do Maven, então precisa de internet e
leva alguns minutos. Depois disso roda offline.

## Como executar

Três terminais, a partir desta pasta:

```sh
mvnw -f cliente-service  spring-boot:run     # terminal 1, porta 8100
mvnw -f catalogo-service spring-boot:run     # terminal 2, porta 8200
mvnw -f pedido-service   spring-boot:run     # terminal 3, porta 8300
```

No Linux ou macOS, `./mvnw`.

Suba `cliente-service` e `catalogo-service` antes do `pedido-service` — ele
depende dos dois para montar um pedido. Mas nenhum trava se o outro estiver
fora: a resposta é um erro `503` explicando qual serviço não respondeu.

## Os bancos

Não há script para rodar à mão. Cada serviço traz suas **migrations do Flyway**
em `src/main/resources/db/migration`, e elas rodam sozinhas ao subir:

| Arquivo | O que faz |
| --- | --- |
| `V1__criar_tabela_*.sql` | Cria a estrutura |
| `V2__popular_*.sql` | Insere os dados de teste |

O Flyway registra o que já aplicou na tabela `flyway_schema_history`, então
reiniciar o serviço **não duplica** os dados. Para começar do zero, apague a
pasta `dados/` do serviço.

O Hibernate está em `ddl-auto=validate`: ele **não cria nem altera** tabela,
apenas confere se as entidades batem com o que as migrations criaram. É assim
que se trabalha fora do ambiente de estudo — o esquema é versionado junto com o
código, não improvisado em tempo de execução.

Para olhar os dados: `http://localhost:8100/h2-console` (idem 8200 e 8300), com
a JDBC URL que está no `application.properties` de cada serviço, usuário `sa` e
senha em branco.

## Como testar

```sh
# os três serviços estão no ar?
curl http://localhost:8100/actuator/health
curl http://localhost:8200/actuator/health
curl http://localhost:8300/actuator/health

# dados que vieram das migrations
curl http://localhost:8100/clientes
curl http://localhost:8200/produtos

# um pedido existente, montado com dados dos três serviços
curl http://localhost:8300/pedidos/1

# abrir um pedido novo: informe só produto e quantidade.
# O preço vem do catálogo, não de quem chama.
curl -X POST http://localhost:8300/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId":1,"itens":[{"produtoId":1,"quantidade":2},{"produtoId":2,"quantidade":1}]}'
```

### O que vale demonstrar

| Teste | O que prova |
| --- | --- |
| `GET /pedidos/1` | O pedido tem itens do banco dele **e** o nome do cliente vindo do `cliente-service` |
| `POST /pedidos` com `clienteId: 999` | **404** — o `pedido-service` consultou o `cliente-service` antes de gravar |
| `POST /pedidos` com `produtoId: 5, quantidade: 10` | **409** — estoque insuficiente (a cadeira tem 2 em estoque) |
| Derrubar o `catalogo-service` e tentar criar pedido | **503**, dizendo qual serviço falhou |
| `POST /pedidos/2/cancelar` | **409** — já está cancelado |

As saídas estão em [`evidencias/`](evidencias/).

## Endpoints

| Serviço | Rotas |
| --- | --- |
| `cliente-service` | `GET /clientes`, `GET /clientes/{id}`, `POST /clientes` |
| `catalogo-service` | `GET /produtos`, `GET /produtos/{id}`, `POST /produtos` |
| `pedido-service` | `GET /pedidos`, `GET /pedidos?clienteId=`, `GET /pedidos/{id}`, `POST /pedidos`, `POST /pedidos/{id}/cancelar` |

## Estrutura

Os três serviços têm os mesmos pacotes, e o `pedido-service` tem um a mais:

```
<servico>/src/main/java/br/edu/ifgoiano/<servico>/
├── modelo/        entidades JPA
├── repositorio/   acesso a dados (Spring Data)
├── servico/       regras de negocio
├── controlador/   endpoints REST
├── dto/           contrato publico da API
├── excecao/       excecoes proprias e o tratador central
└── integracao/    (so no pedido-service) chamadas aos outros servicos
```

## Tratamento de erros

Cada serviço tem um `TratadorDeErros` com `@RestControllerAdvice`, e todos
respondem no mesmo formato, com o campo `servico` dizendo de onde veio a falha:

| Situação | Código |
| --- | --- |
| Campo faltando ou inválido | **400** |
| Id que não existe | **404** |
| Estoque insuficiente, pedido já cancelado | **409** |
| Um serviço de que dependemos não respondeu | **503** |
| Falha inesperada | **500** |

O **503** é o que a arquitetura distribuída acrescenta: no monolito, chamar
outro módulo era uma chamada de método que não falhava por rede.

## Tecnologias

Java 21, Spring Boot 3.5.3 (Web, Data JPA, Validation, Actuator), Flyway 11,
banco H2 em arquivo, `RestClient` para a comunicação entre serviços.
