# Relatório — Microsserviços com base de dados própria

**Disciplina:** Sistemas Distribuídos · **Integrante:** Paulo Henrique Rodrigues de Lima

## 1. O que foi construído

Uma loja decomposta em três microsserviços Spring Boot independentes, cada um
com **seu próprio banco de dados**:

| Serviço | Porta | Banco | Tabelas |
| --- | --- | --- | --- |
| `cliente-service` | 8100 | `clientes` | `cliente` |
| `catalogo-service` | 8200 | `catalogo` | `produto` |
| `pedido-service` | 8300 | `pedidos` | `pedido` **1—N** `item_pedido` |

O `pedido-service` atende ao requisito das duas tabelas se relacionando, e é
também o único que depende dos outros dois: para montar um pedido ele precisa
saber se o cliente existe e quanto custa cada produto — informações que não
estão no banco dele.

## 2. Como os serviços se comunicam

Por **HTTP**, nunca pelo banco. Esta é a regra que sustenta a arquitetura: se o
`pedido-service` lesse a tabela `produto` direto, os dois serviços passariam a
compartilhar um esquema e deixariam de poder evoluir separados.

Ao receber `POST /pedidos`, o `pedido-service`:

1. chama `GET /clientes/{id}` no `cliente-service` para confirmar que o cliente existe;
2. para cada item, chama `GET /produtos/{id}` no `catalogo-service` e obtém **preço e estoque do momento**;
3. recusa o pedido se o estoque não cobrir a quantidade;
4. grava o pedido e os itens no banco dele, com o preço praticado copiado para cada item.

As chamadas ficam isoladas em `ClienteClient` e `CatalogoClient`, no pacote
`integracao`. Foi usado o **`RestClient`** do Spring 6.1, e não o `RestTemplate`:
este último está em modo de manutenção e a própria documentação recomenda o
`RestClient` para código novo. Os dois clientes têm **timeout de 3 segundos** —
sem ele, um serviço travado prenderia as threads do `pedido-service` e a
lentidão de um derrubaria o outro.

## 3. Banco de dados

Cada serviço tem um banco **H2 em arquivo**, criado e populado pelo **Flyway**
na primeira execução. O requisito de "arquivos para popular o banco para teste"
é atendido pelas migrations:

| Arquivo | Função |
| --- | --- |
| `V1__criar_tabela_*.sql` | Cria a estrutura |
| `V2__popular_*.sql` | Insere os dados de teste |

O Flyway registra o que aplicou em `flyway_schema_history`, então reiniciar o
serviço não duplica dados.

Duas decisões merecem destaque:

**`ddl-auto=validate`.** O Hibernate não cria nem altera tabela: apenas confere
se as entidades batem com o que as migrations criaram. O esquema é versionado
junto com o código, que é como se trabalha fora do ambiente de estudo. Isso foi
testado na prática — a primeira versão da migration declarou `uf` como `CHAR(2)`
enquanto a entidade esperava `VARCHAR(2)`, e o serviço **se recusou a subir**,
apontando a coluna divergente. Com `ddl-auto=update` o erro passaria despercebido.

**Nada de chave estrangeira entre serviços.** Em `pedido`, as colunas
`cliente_id` e `produto_id` são apenas identificadores: não existe
`FOREIGN KEY` para eles, porque as tabelas referenciadas estão em outros bancos.
Quem valida esses ids é a chamada HTTP feita antes de gravar. Já entre `pedido`
e `item_pedido` há chave estrangeira de verdade — as duas estão no mesmo banco.

## 4. Tratamento de erros

Cada serviço tem um `TratadorDeErros` anotado com `@RestControllerAdvice`, e
todos respondem no mesmo formato, com o campo `servico` indicando a origem da
falha.

| Situação | Código | Exceção |
| --- | --- | --- |
| Campo faltando ou inválido | 400 | `MethodArgumentNotValidException` |
| Id inexistente | 404 | `RecursoNaoEncontradoException` |
| Estoque insuficiente, pedido já cancelado | 409 | `RegraDeNegocioException` |
| Serviço dependente não respondeu | 503 | `ServicoIndisponivelException` |
| Falha inesperada | 500 | `Exception` |

O **503** é o que a arquitetura distribuída acrescenta. No monolito, chamar
outro módulo era uma chamada de método que não falhava por rede; aqui cada
dependência é um processo separado que pode estar fora do ar. Retornar 503, e
não 500, informa que a causa é temporária e de terceiro, e que vale tentar de
novo.

Um detalhe do `ClienteClient`: um **404 do cliente-service vira 404 aqui**, com
a mensagem correta. Sem esse tratamento, a ausência de um cliente apareceria
como erro 500 genérico, e quem consome a API não saberia o que corrigir.

## 5. Testes realizados

| Teste | Resultado |
| --- | --- |
| `GET /pedidos/1` | 200 — itens do banco local e nome do cliente vindo do `cliente-service` |
| `POST /pedidos` com cliente 3 e dois produtos | 201 — total calculado com os preços do catálogo |
| `POST /pedidos` com `clienteId: 999` | **404** — "Cliente nao encontrado para o id 999" |
| `POST /pedidos` com 10 unidades de um produto com estoque 2 | **409** — "Estoque insuficiente… disponivel 2, pedido 10" |
| `POST /pedidos/2/cancelar` (já cancelado) | **409** |
| `POST /pedidos` sem cliente e sem itens | **400** com a lista dos dois campos |
| `POST /pedidos` com o `catalogo-service` **desligado** | **503** — "O servico catalogo-service nao respondeu" |
| `GET /pedidos/1` com o `catalogo-service` desligado | **200** — a leitura não depende do catálogo |

As saídas completas estão em `evidencias/`.

## 6. Decisão deliberada: o estoque não é baixado

O `pedido-service` **verifica** o estoque, mas não o decrementa. Baixar estoque
em outro serviço exigiria uma escrita remota que pode falhar depois do pedido já
gravado, deixando os dois bancos inconsistentes — o problema que se resolve com
*saga* ou mensageria, assunto fora do escopo desta entrega. Preferi deixar a
limitação explícita a escondê-la atrás de um código que falharia em silêncio.
