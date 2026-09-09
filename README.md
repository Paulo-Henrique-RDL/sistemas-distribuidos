# Sistemas Distribuídos

Atividades da disciplina. Cada uma fica em sua própria pasta e é documentada em uma seção abaixo.

| # | Atividade | Pasta |
| --- | --- | --- |
| 1 | Múltiplas mensagens sobre TCP | [`atv1-socket-tcp/`](atv1-socket-tcp/) |
| 2 | Monitoramento de sensores em tempo real sobre UDP | [`atv2-socket-udp/`](atv2-socket-udp/) |

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
