# ATV4 — Configuração Centralizada com Spring Cloud Config

> **Enunciado:** implementar um Config Server que serve configurações a partir de um repositório Git remoto, e três aplicações clientes — `cliente-vendas`, `cliente-estoque` e `cliente-relatorios` — que consomem dele suas propriedades, demonstrando perfis de ambiente, versionamento e integração entre microsserviços.

**Integrante:** Paulo Henrique Rodrigues de Lima

Quatro aplicações Spring Boot independentes. Nenhuma das três clientes sabe qual
é a sua mensagem nem em que porta deve subir: as duas coisas chegam do servidor
de configuração, que por sua vez as lê de um repositório Git.

O repositório de configurações é outro:
**[atv4-repo-configs](https://github.com/Paulo-Henrique-RDL/atv4-repo-configs)**.

## Requisitos

**JDK 21 ou superior.** O Maven vem embutido (Maven Wrapper), e a primeira
execução baixa as dependências — precisa de internet. O Config Server também
precisa de internet em tempo de execução, porque clona o repositório acima.

## Como executar

Quatro terminais, todos a partir desta pasta. **O servidor vai primeiro** — os
clientes perguntam a ele a porta em que devem subir.

```sh
mvnw -f config-server      spring-boot:run     # terminal 1
mvnw -f cliente-vendas     spring-boot:run     # terminal 2
mvnw -f cliente-estoque    spring-boot:run     # terminal 3
mvnw -f cliente-relatorios spring-boot:run     # terminal 4
```

No Linux ou macOS, `./mvnw`.

## Como testar

Pelos clientes — cada um responde o texto que veio do repositório Git:

| URL | Resposta esperada |
| --- | --- |
| http://localhost:8081/mensagem | `Bem-vindo ao sistema de Vendas - Ambiente DEV` |
| http://localhost:8082/mensagem | `Serviço de Estoque inicializado - Ambiente DEV` |
| http://localhost:8083/mensagem | `Serviço de Relatórios em execução - Ambiente DEV` |

Direto no servidor — devolve o JSON que ele entrega a cada cliente:

- http://localhost:8888/cliente-vendas/dev
- http://localhost:8888/cliente-estoque/dev
- http://localhost:8888/cliente-relatorios/dev

Saúde do servidor: http://localhost:8888/actuator/health

### A prova de que a configuração veio mesmo do servidor

Nenhum cliente declara `server.port`. As portas 8081, 8082 e 8083 estão só no
repositório de configurações. Se os três subirem na 8081/8082/8083, a
configuração central foi lida.

Para ver o contrário, **derrube o Config Server e suba um cliente**: por causa do
prefixo `optional:`, ele sobe assim mesmo — mas na porta 8080, respondendo
`Configuracao nao carregada` em `/mensagem`.

As duas situações estão registradas em [`evidencias/`](evidencias/).

## Estrutura

```
atv4-config-server/
├── config-server/        @EnableConfigServer, porta 8888
├── cliente-vendas/       porta 8081, vinda do repositorio de configuracoes
├── cliente-estoque/      porta 8082, idem
├── cliente-relatorios/   porta 8083, idem
├── evidencias/           saidas dos testes executados
├── relatorio.md          relatorio da entrega
└── mvnw, mvnw.cmd, .mvn/ Maven Wrapper, um so para os quatro projetos
```

Cada projeto tem o seu `pom.xml` e sobe sozinho — não há projeto-pai. O Maven
Wrapper é único para não repetir o mesmo arquivo quatro vezes; é por isso que os
comandos usam `-f <projeto>`.

## Tecnologias

Java 21, Spring Boot 3.5.3, Spring Cloud 2025.0.0 (Config Server e Config
Client), Spring Boot Actuator, Spring Web.
