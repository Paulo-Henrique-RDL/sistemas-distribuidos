# Contas a Pagar e a Receber

Projeto Integrador da disciplina de Sistemas Distribuídos. Sistema de controle financeiro construído em duas etapas: primeiro como **aplicação monolítica**, depois decomposto em **microsserviços**.

Este repositório contém a **etapa monolítica**.

> **Integrante:** Paulo Henrique Rodrigues de Lima

## Requisitos

- **JDK 21 ou superior** — confira com `java -version`
- Nada além disso: o Maven vem embutido no projeto (Maven Wrapper) e o banco H2 não precisa de instalação

## Como executar

```sh
cd monolito
./mvnw spring-boot:run
```

No Windows, use `mvnw` em vez de `./mvnw`.

A aplicação sobe em **http://localhost:8080**. O banco é criado automaticamente na primeira execução, em `monolito/dados/contas.mv.db`.

### Primeiros passos

O sistema sobe com o banco vazio. Para criar um cadastro mínimo e lançar uma conta:

```sh
# 1. um fornecedor
curl -X POST http://localhost:8080/api/pessoas \
  -H "Content-Type: application/json" \
  -d '{"nome":"Papelaria Central","documento":"12.345.678/0001-90","tipo":"FORNECEDOR"}'

# 2. uma categoria
curl -X POST http://localhost:8080/api/categorias \
  -H "Content-Type: application/json" \
  -d '{"nome":"Material de escritorio"}'

# 3. uma forma de pagamento
curl -X POST http://localhost:8080/api/formas-pagamento \
  -H "Content-Type: application/json" -d '{"nome":"PIX"}'

# 4. uma conta a pagar
curl -X POST http://localhost:8080/api/contas-pagar \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Compra de papel","fornecedorId":1,"categoriaId":1,
       "valor":450.00,"dataEmissao":"2026-10-01","dataVencimento":"2026-11-10"}'

# 5. registrar o pagamento
curl -X POST http://localhost:8080/api/contas-pagar/1/pagar \
  -H "Content-Type: application/json" \
  -d '{"dataPagamento":"2026-10-20","valorPago":450.00,"formaPagamentoId":1}'
```

Se preferir começar com dados prontos, aplique `scripts/carga-exemplo.sql` pelo console do banco.

### Console do banco

**http://localhost:8080/h2-console**

| Campo | Valor |
| --- | --- |
| JDBC URL | `jdbc:h2:file:./dados/contas` |
| Usuário | `sa` |
| Senha | *(em branco)* |

### Testes

```sh
cd monolito
./mvnw test
```

São 32 testes cobrindo regras de negócio, endpoints, integração com o banco, entradas inválidas, situações de erro e operações duplicadas.

## Endpoints

| Recurso | Rotas |
| --- | --- |
| Pessoas | `/api/pessoas` — POST, GET, GET/{id}, PUT/{id}, DELETE/{id} |
| Categorias | `/api/categorias` — mesmas operações |
| Formas de pagamento | `/api/formas-pagamento` — mesmas operações |
| Contas a pagar | `/api/contas-pagar` + `POST /{id}/pagar` e `POST /{id}/cancelar` |
| Contas a receber | `/api/contas-receber` + `POST /{id}/receber` e `POST /{id}/cancelar` |

Consultas por período e situação:

```
GET /api/contas-pagar?situacao=VENCIDA
GET /api/contas-pagar?de=2026-10-01&ate=2026-10-31
GET /api/contas-receber?situacao=ABERTA&clienteId=3
```

A descrição completa das APIs, com corpo de requisição e códigos de resposta, está em [`docs/documentacao.md`](docs/documentacao.md).

## Estrutura do repositório

```
contas-pagar-receber/
├── monolito/          aplicação Spring Boot
│   └── src/main/java/br/edu/ifgoiano/contas/
│       ├── modelo/        entidades e enums
│       ├── repositorio/   acesso a dados
│       ├── servico/       regras de negócio
│       ├── controlador/   endpoints REST
│       ├── dto/           objetos de transferência
│       └── excecao/       exceções e tratamento de erros
├── docs/
│   ├── documentacao.md    documentação e decisões arquiteturais
│   └── evidencias/        saídas dos testes executados
└── scripts/
    ├── schema.sql         estrutura do banco
    └── carga-exemplo.sql  dados de exemplo (opcional)
```

## Tecnologias

Java 21, Spring Boot 3.5.3 (Web, Data JPA, Validation), Hibernate, banco H2 em arquivo, JUnit 5 e MockMvc.
