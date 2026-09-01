# ATV1 — Comunicação cliente-servidor com sockets TCP

Servidor de eco sobre TCP. O cliente lê mensagens digitadas no console e envia ao servidor, que devolve cada uma prefixada com `Servidor recebeu:`. A conversa termina quando o cliente envia `sair`.

## Componentes

| Arquivo | Papel |
| --- | --- |
| `src/Servidor.java` | Abre um `ServerSocket` na porta 12345, aceita uma conexão e ecoa as mensagens recebidas |
| `src/Cliente.java` | Conecta em `localhost:12345`, lê do console e exibe a resposta do servidor |

## Protocolo

- **Transporte:** TCP
- **Porta:** 12345
- **Formato:** texto, uma mensagem por linha
- **Encerramento:** o cliente envia `sair`; ambos os lados fecham a conexão

## Como executar

Compilar:

```sh
javac -d bin src/Servidor.java src/Cliente.java
```

Em um terminal, subir o servidor:

```sh
java -cp bin Servidor
```

Em outro terminal, rodar o cliente:

```sh
java -cp bin Cliente
```

Digite mensagens no cliente e observe o eco. Para encerrar, digite `sair`.

## Limitações conhecidas

- Atende **um único cliente por vez** — o servidor não usa threads, então uma segunda conexão fica na fila até a primeira terminar.
- Após o cliente desconectar, o servidor encerra em vez de voltar a aceitar conexões.
- Endereço e porta estão fixos no código, sem parâmetro de linha de comando.
