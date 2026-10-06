// IOException, lançada pelo envio do datagrama.
import java.io.*;
// DatagramSocket, DatagramPacket e InetAddress: o trio do UDP no remetente.
import java.net.*;
// Charset explícito: os dois lados precisam combinar, senão acento vira lixo.
import java.nio.charset.StandardCharsets;
// Locale para o separador decimal; Random para simular a oscilação do sensor.
import java.util.Locale;
import java.util.Random;

public class SensorUDP {
    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 12345;
    private static final String ID_PADRAO = "sensor-01";
    private static final String GRANDEZA_PADRAO = "temperatura";
    // Uma leitura por segundo: é essa frequência que torna a perda tolerável,
    // porque o dado perdido é substituído logo pelo seguinte.
    private static final long INTERVALO_MS = 1000;

    public static void main(String[] args) {
        // Todos os argumentos são opcionais e posicionais: <host> <porta> <id> <grandeza>.
        // O operador ternário escolhe entre o argumento e o padrão em uma linha.
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = PORTA_PADRAO;
        // O id é o que separa as linhas no painel: cada sensor precisa de um próprio.
        String id = args.length > 2 ? args[2] : ID_PADRAO;

        if (args.length > 1) {
            try {
                porta = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: " + args[1] + ". Usando " + PORTA_PADRAO + ".");
            }
        }

        String nomeGrandeza = args.length > 3 ? args[3] : GRANDEZA_PADRAO;
        Grandeza grandeza = Grandeza.de(nomeGrandeza);

        // Grandeza desconhecida é erro de uso, não de rede: avisa o que é aceito e sai
        // antes de abrir qualquer socket.
        if (grandeza == null) {
            System.err.println("Grandeza desconhecida: " + nomeGrandeza);
            System.err.println("Use: temperatura, umidade, pressao ou vazao.");
            return;
        }

        // Construtor SEM porta: o sensor não precisa de porta fixa porque ninguém
        // procura por ele. O sistema operacional escolhe uma porta qualquer.
        // O destino não está no socket - vai escrito em cada pacote.
        try (DatagramSocket socket = new DatagramSocket()) {
            // Resolve o nome ("localhost" ou um IP) para um endereço, uma única vez,
            // em vez de repetir a resolução a cada envio.
            InetAddress endereco = InetAddress.getByName(host);
            Random aleatorio = new Random();

            // Ponto de partida sorteado dentro da faixa da grandeza, para dois
            // sensores iguais não começarem no mesmo valor.
            double valor = grandeza.sortearInicial(aleatorio);
            // O contador que permite ao servidor detectar perdas. Começa em 1 e só cresce.
            long sequencia = 1;

            System.out.printf("Sensor %s (%s) enviando para %s:%d a cada %d ms.%n",
                id, grandeza.nome, host, porta, INTERVALO_MS);
            System.out.println("Ctrl+C para encerrar.");

            // Laço infinito: o sensor mede e envia indefinidamente.
            while (true) {
                // Monta a mensagem no formato combinado com o servidor.
                // Locale.US garante ponto decimal; %.1f corta em uma casa.
                String mensagem = String.format(Locale.US, "%s;%d;%s;%.1f;%s",
                    id, sequencia, grandeza.nome, valor, grandeza.unidade);

                // Texto vira bytes: é isso que trafega. O UDP não sabe o que é String.
                byte[] dados = mensagem.getBytes(StandardCharsets.UTF_8);
                // O pacote carrega os dados E o destino. Diferente do TCP, onde o
                // destino foi definido uma vez na conexão, aqui vai em cada envio.
                // send() NÃO espera confirmação e NÃO bloqueia esperando resposta:
                // entrega ao sistema operacional e volta. O sensor nem sabe se o
                // servidor está no ar - e, pelo enunciado, não precisa saber.
                socket.send(new DatagramPacket(dados, dados.length, endereco, porta));

                System.out.println("Enviado: " + mensagem);

                // Espera um segundo antes da próxima medição. Thread.sleep lança
                // InterruptedException, tratada no catch lá embaixo.
                Thread.sleep(INTERVALO_MS);

                // Próxima medição: caminha a partir do valor atual, simulando um
                // sensor real, em que a grandeza varia aos poucos.
                valor = grandeza.proximoValor(valor, aleatorio);
                // Incrementa SEMPRE, mesmo que o datagrama anterior tenha se perdido.
                // É justamente o buraco na numeração que denuncia a perda no servidor.
                sequencia++;
            }

        } catch (IOException e) {
            // Falha ao abrir o socket ou ao enviar.
            System.err.println("Erro no sensor: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            // Boa prática: restaura o sinal de interrupção que o catch consumiu,
            // para quem chamou esta thread saber que ela foi interrompida.
            Thread.currentThread().interrupt();
            System.out.println("Sensor interrompido.");
        }
    }

    // Classe aninhada que simula o comportamento físico de cada tipo de sensor:
    // guarda a faixa válida, a unidade e o quanto o valor pode variar por leitura.
    private static class Grandeza {
        private final String nome;
        private final String unidade;
        private final double minimo;
        private final double maximo;
        // Variação máxima entre duas leituras seguidas: é o que faz o valor
        // caminhar suavemente em vez de pular de um extremo ao outro.
        private final double variacao;

        private Grandeza(String nome, String unidade, double minimo, double maximo, double variacao) {
            this.nome = nome;
            this.unidade = unidade;
            this.minimo = minimo;
            this.maximo = maximo;
            this.variacao = variacao;
        }

        // Fábrica por nome: concentra num só lugar as faixas realistas de cada
        // grandeza. Devolve null para nome desconhecido, tratado por quem chama.
        private static Grandeza de(String nome) {
            // Locale.ROOT no toLowerCase evita a armadilha do idioma turco, onde
            // o 'I' maiúsculo não vira 'i'.
            switch (nome.toLowerCase(Locale.ROOT)) {
                case "temperatura":
                    return new Grandeza("temperatura", "C", 18.0, 32.0, 0.6);
                case "umidade":
                    return new Grandeza("umidade", "%RH", 30.0, 90.0, 2.0);
                case "pressao":
                    return new Grandeza("pressao", "hPa", 980.0, 1040.0, 1.5);
                case "vazao":
                    return new Grandeza("vazao", "L/min", 0.0, 120.0, 4.0);
                default:
                    return null;
            }
        }

        // Sorteia um ponto qualquer dentro da faixa: nextDouble() dá [0,1), que
        // esticado pela amplitude e somado ao mínimo cobre toda a faixa.
        private double sortearInicial(Random aleatorio) {
            return minimo + aleatorio.nextDouble() * (maximo - minimo);
        }

        // Passeio aleatório: (nextDouble - 0.5) dá algo entre -0.5 e +0.5, que
        // multiplicado por 2 e pela variação resulta em um passo entre -variacao
        // e +variacao, para cima ou para baixo com a mesma chance.
        private double proximoValor(double atual, Random aleatorio) {
            double proximo = atual + (aleatorio.nextDouble() - 0.5) * 2 * variacao;
            // Prende o resultado na faixa: o max impede cair abaixo do mínimo e o
            // min impede passar do máximo, sem precisar de if.
            return Math.max(minimo, Math.min(maximo, proximo));
        }
    }
}
