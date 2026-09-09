import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

public class SensorUDP {
    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 12345;
    private static final String ID_PADRAO = "sensor-01";
    private static final String GRANDEZA_PADRAO = "temperatura";
    private static final long INTERVALO_MS = 1000;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = PORTA_PADRAO;
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

        if (grandeza == null) {
            System.err.println("Grandeza desconhecida: " + nomeGrandeza);
            System.err.println("Use: temperatura, umidade, pressao ou vazao.");
            return;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress endereco = InetAddress.getByName(host);
            Random aleatorio = new Random();

            double valor = grandeza.sortearInicial(aleatorio);
            long sequencia = 1;

            System.out.printf("Sensor %s (%s) enviando para %s:%d a cada %d ms.%n",
                id, grandeza.nome, host, porta, INTERVALO_MS);
            System.out.println("Ctrl+C para encerrar.");

            while (true) {
                String mensagem = String.format(Locale.US, "%s;%d;%s;%.1f;%s",
                    id, sequencia, grandeza.nome, valor, grandeza.unidade);

                byte[] dados = mensagem.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(dados, dados.length, endereco, porta));

                System.out.println("Enviado: " + mensagem);

                Thread.sleep(INTERVALO_MS);

                valor = grandeza.proximoValor(valor, aleatorio);
                sequencia++;
            }

        } catch (IOException e) {
            System.err.println("Erro no sensor: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Sensor interrompido.");
        }
    }

    private static class Grandeza {
        private final String nome;
        private final String unidade;
        private final double minimo;
        private final double maximo;
        private final double variacao;

        private Grandeza(String nome, String unidade, double minimo, double maximo, double variacao) {
            this.nome = nome;
            this.unidade = unidade;
            this.minimo = minimo;
            this.maximo = maximo;
            this.variacao = variacao;
        }

        private static Grandeza de(String nome) {
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

        private double sortearInicial(Random aleatorio) {
            return minimo + aleatorio.nextDouble() * (maximo - minimo);
        }

        private double proximoValor(double atual, Random aleatorio) {
            double proximo = atual + (aleatorio.nextDouble() - 0.5) * 2 * variacao;
            return Math.max(minimo, Math.min(maximo, proximo));
        }
    }
}
