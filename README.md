# Sistemas Distribuídos

Repositório das atividades da disciplina de Sistemas Distribuídos. Cada atividade fica em sua própria pasta e é documentada em uma seção deste README, com enunciado, o que foi implementado e como executar.

## Índice

| # | Atividade | Tema | Pasta |
| --- | --- | --- | --- |
| 1 | Múltiplas mensagens sobre TCP | Sockets, comunicação cliente-servidor | [`atv1-socket-tcp/`](atv1-socket-tcp/) |

## Requisitos

- **JDK 17 ou superior** — confira com `java -version`
- Dois terminais abertos (um para o servidor, outro para o cliente)

## Convenções do repositório

Cada atividade segue o formato `atvN-tema/`, contendo apenas o código-fonte:

```
atvN-tema/
└── src/     código-fonte Java
```

Os arquivos compilados (`bin/`, `*.class`) não são versionados — o `.gitignore` cuida disso. Toda a documentação fica centralizada neste README.

---

## ATV1 — Múltiplas mensagens sobre TCP

### Enunciado

> Modificar o exemplo feito em sala de aula para múltiplas mensagens. O servidor responde as mensagens até o cliente digitar `sair`.

### O que foi implementado

O exemplo da aula trocava **uma única mensagem** e encerrava. A modificação transformou a troca em uma **conversa contínua**:

- O cliente entrou em um laço que lê do console e envia ao servidor indefinidamente, em vez de enviar uma vez só.
- O servidor entrou em um laço que lê do socket enquanto houver mensagem, respondendo cada uma com `Servidor recebeu: <mensagem>`.
- Foi definida a palavra-chave `sair` como condição de parada, verificada nos **dois lados** — o cliente para de ler o console e o servidor fecha a conexão de forma controlada, sem esperar timeout nem lançar exceção.

### Estrutura

```
atv1-socket-tcp/
└── src/
    ├── Servidor.java    abre o ServerSocket na porta 12345 e ecoa as mensagens
    └── Cliente.java     conecta em localhost:12345 e lê do console
```

### Protocolo

| Item | Valor |
| --- | --- |
| Transporte | TCP |
| Porta | 12345 |
| Endereço | `localhost` |
| Formato | Texto, uma mensagem por linha |
| Encerramento | O cliente envia `sair` |

### Como executar

**1. Compilar** — a partir da raiz do repositório:

```sh
cd atv1-socket-tcp
javac -d bin src/Servidor.java src/Cliente.java
```

**2. Iniciar o servidor** — no primeiro terminal:

```sh
java -cp bin Servidor
```

**3. Iniciar o cliente** — no segundo terminal, também dentro de `atv1-socket-tcp`:

```sh
java -cp bin Cliente
```

Digite mensagens no cliente. Cada uma recebe a resposta do servidor. Para encerrar, digite `sair`.

### Exemplo de execução

Terminal do servidor:

```
Servidor aguardando conexão...
Cliente conectado!
Cliente disse: ola servidor
Cliente disse: teste 2
Cliente disse: sair
Cliente solicitou o encerramento. Fechando conexão.
```

Terminal do cliente:

```
Conectado ao servidor. Digite suas mensagens ('sair' para encerrar).
> ola servidor
Resposta do Servidor: Servidor recebeu: ola servidor
> teste 2
Resposta do Servidor: Servidor recebeu: teste 2
> sair
Conexão encerrada.
```

### Limitações conhecidas

- Atende **um cliente por vez** — o servidor é single-thread, então uma segunda conexão fica na fila até a primeira terminar.
- O servidor **encerra após o primeiro cliente desconectar**, em vez de voltar a aceitar novas conexões.
- Endereço e porta estão **fixos no código**, sem parâmetro de linha de comando.
