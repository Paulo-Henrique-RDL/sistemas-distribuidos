# Sistemas Distribuídos

Atividades da disciplina. Cada uma fica em sua própria pasta e é documentada em uma seção abaixo.

| # | Atividade | Pasta |
| --- | --- | --- |
| 1 | Múltiplas mensagens sobre TCP | [`atv1-socket-tcp/`](atv1-socket-tcp/) |
| 2 | Monitoramento de sensores em tempo real sobre UDP | [`atv2-socket-udp/`](atv2-socket-udp/) |
| 3 | RMI: controle remoto e Pedra, Papel, Tesoura, Lagarto e Spock | [`atv3-rmi/`](atv3-rmi/) |

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

---

## ATV2 — Monitoramento de sensores em tempo real (UDP)

> **Enunciado:** vários "sensores" (clientes) enviam leituras simuladas para um servidor central, que as exibe em tempo real. A perda ocasional de uma leitura não é crítica, pois novas leituras chegam constantemente.

UDP se encaixa porque o que importa é a **leitura mais recente**, não o histórico: sem handshake e sem retransmissão, cada leitura sai em um único datagrama e o sensor já segue para a próxima. Um datagrama perdido é substituído pela leitura seguinte um segundo depois.

Como não há conexão, um único socket no servidor atende quantos sensores forem necessários — não existe `accept()` nem uma thread por cliente. O sensor também não precisa do servidor no ar para começar a enviar.

Comunicação em UDP na porta 12345 (configurável), um datagrama de texto por leitura:

```
id;sequencia;grandeza;valor;unidade      ex.: sensor-01;42;temperatura;25.3;C
```

O número de sequência é o que permite ao servidor **contar as perdas**: se a leitura 41 não chega, o salto de 40 para 42 aparece na coluna `PERDIDOS` sem interromper a exibição. Datagramas fora do formato são descartados com aviso, e o servidor mantém apenas a última leitura de cada sensor, redesenhando o painel a cada datagrama recebido:

```
=== Monitoramento de sensores em tempo real (UDP :12345) ===
Atualizado em 19:58:06 | sensores: 3 | recebidos: 35 | perdidos: 2

SENSOR       GRANDEZA     LEITURA      ORIGEM                SEQ  RECEBIDOS  PERDIDOS      HORA
sensor-01    temperatura  29.4 C       127.0.0.1              11         11         0  19:58:06
sensor-02    umidade      59.3 %RH     127.0.0.1              11         11         0  19:58:06
sensor-03    vazao        43.6 L/min   127.0.0.1              13         11         2  19:58:06
```

### Como executar

```sh
cd atv2-socket-udp
javac -d bin src/ServidorUDP.java src/SensorUDP.java
```

Servidor no primeiro terminal:

```sh
java -cp bin ServidorUDP            # ou: java -cp bin ServidorUDP <porta>
```

Cada sensor em um terminal próprio — o `id` distingue as linhas do painel:

```sh
java -cp bin SensorUDP localhost 12345 sensor-01 temperatura
java -cp bin SensorUDP localhost 12345 sensor-02 umidade
java -cp bin SensorUDP localhost 12345 sensor-03 vazao
```

Argumentos do sensor: `<host> <porta> <id> <grandeza>`, todos opcionais (padrão `localhost 12345 sensor-01 temperatura`). Grandezas disponíveis: `temperatura` (C), `umidade` (%RH), `pressao` (hPa) e `vazao` (L/min) — cada leitura caminha aleatoriamente dentro da faixa da grandeza e é enviada a cada segundo. Encerre com `Ctrl+C`.

### Entre dois computadores

Na máquina do servidor, descubra o IP na rede local (`ipconfig getifaddr en0` no macOS, `ip addr` no Linux, `ipconfig` no Windows) e deixe o `ServidorUDP` rodando. Nas máquinas dos sensores, troque `localhost` por esse IP:

```sh
java -cp bin SensorUDP 192.168.0.10 12345 sensor-notebook temperatura
```

As duas máquinas precisam estar na mesma rede e a porta 12345/UDP liberada no firewall do servidor.

---

## ATV3 — RMI: controle remoto e Pedra, Papel, Tesoura, Lagarto e Spock

> **Enunciado:** criar aplicações com RMI em que um computador é o servidor e outro é o cliente. **(1)** Emular pelo menos 5 funções de um controle remoto. **(2)** Jogo Pedra, Papel, Tesoura, Lagarto e Spock para 2 a 5 clientes: o servidor controla as rodadas (10 s entre rodadas, 5 s para jogar, desclassificando quem não joga), indica o vencedor ou o empate e exibe o placar; o vencedor envia "Perdeu Loser...Tente na próxima." aos perdedores.

Com RMI o cliente chama métodos de um objeto que está em outra máquina como se ele fosse local. O servidor publica o objeto no *registry* com um nome; o cliente busca esse nome e recebe um *stub*, que transforma cada chamada de método em uma troca de mensagens pela rede.

### Aplicação 1 — Controle remoto

O servidor é a TV, que guarda o estado (ligada, canal, volume, mudo). O cliente é o controle, com 8 funções: ligar/desligar, volume +, volume −, canal +, canal −, ir para um canal, mudo e status. A TV registra cada comando com o IP de quem enviou.

```sh
cd atv3-rmi/controle-remoto
javac -d bin src/*.java
java -cp bin ServidorControle              # [porta] [ip-do-servidor]
java -cp bin ClienteControle localhost     # [host] [porta]
```

### Aplicação 2 — Pedra, Papel, Tesoura, Lagarto e Spock

RMI só vai do cliente para o servidor, mas o jogo precisa do caminho inverso: avisar que a rodada abriu, o resultado, o placar e a mensagem do vencedor. Para isso cada cliente publica seu próprio objeto remoto `Jogador` e o entrega ao entrar no jogo; o servidor passa a chamá-lo de volta (*callback*).

Regras que o enunciado deixa em aberto:

- **Vencedor:** cada jogador soma quantos adversários o seu gesto derrota, e a maior soma única vence. Se a maior soma se repetir, ou se todos jogarem o mesmo gesto, é empate e todos jogam de novo na rodada seguinte.
- **Sem jogada em 5 s:** o jogador é desclassificado da rodada e conta como perdedor. Se só um jogar, vence por W.O.; se ninguém jogar, a rodada é anulada.
- **Mensagem aos perdedores:** o cliente vencedor envia, e o servidor repassa, porque só ele conhece os demais clientes.

```sh
cd atv3-rmi/jokenpo-spock
javac -d bin src/*.java
java -cp bin ServidorJogo                          # [porta] [ip-do-servidor]
java -cp bin ClienteJogo localhost 1100 ana        # [host] [porta] [nome] [ip-deste-cliente]
```

As rodadas começam sozinhas quando há pelo menos 2 jogadores. Quando a rodada abrir, digite de 1 a 5 ou o nome do gesto; `sair` encerra.

### Entre dois computadores

O RMI grava o endereço do servidor dentro do *stub*. Se a máquina tiver mais de uma rede (Wi-Fi, cabo, VPN), ele pode escolher a errada, então informe o IP. No jogo o servidor também se conecta de volta ao cliente, por isso o cliente informa o próprio IP:

```sh
java -cp bin ServidorJogo 1100 192.168.0.10
java -cp bin ClienteJogo 192.168.0.10 1100 ana 192.168.0.20
```

O controle remoto usa a porta 1099 e o jogo, a 1100. Libere o Java no firewall das duas máquinas.
