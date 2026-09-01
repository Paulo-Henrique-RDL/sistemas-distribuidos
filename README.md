# Sistemas Distribuídos

Atividades da disciplina. Cada uma fica em sua própria pasta e é documentada em uma seção abaixo.

| # | Atividade | Pasta |
| --- | --- | --- |
| 1 | Múltiplas mensagens sobre TCP | [`atv1-socket-tcp/`](atv1-socket-tcp/) |

Requisito: **JDK 17 ou superior**.

---

## ATV1 — Múltiplas mensagens sobre TCP

> **Enunciado:** modificar o exemplo feito em sala de aula para múltiplas mensagens. O servidor responde as mensagens até o cliente digitar `sair`.

O exemplo da aula trocava uma única mensagem e encerrava. Cliente e servidor ganharam laços para manter a conversa, e `sair` passou a ser a condição de parada verificada **nos dois lados** — o encerramento é controlado, sem timeout nem exceção.

Comunicação em TCP na porta 12345, texto com uma mensagem por linha.

### Como executar

```sh
cd atv1-socket-tcp
javac -d bin src/Servidor.java src/Cliente.java
```

Servidor no primeiro terminal, cliente no segundo:

```sh
java -cp bin Servidor
java -cp bin Cliente
```

Cada mensagem digitada recebe `Servidor recebeu: <mensagem>` de volta. Digite `sair` para encerrar.

### Limitações

Servidor single-thread: atende um cliente por vez e encerra quando ele desconecta. Host e porta são fixos no código.
