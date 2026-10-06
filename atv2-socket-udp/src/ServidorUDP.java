import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ServidorUDP {
    private static final int PORTA_PADRAO = 12345;
    private static final int TAMANHO_BUFFER = 1024;
    private static final String LIMPAR_TELA = "\033[H\033[2J";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        int porta = PORTA_PADRAO;

        if (args.length > 0) {
            try {
                porta = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: " + args[0] + ". Usando " + PORTA_PADRAO + ".");
            }
        }

        Map<String, Leitura> ultimasLeituras = new TreeMap<>();

        try (DatagramSocket socket = new DatagramSocket(porta)) {
            System.out.println("Servidor UDP escutando na porta " + porta + ".");
            System.out.println("Aguardando leituras dos sensores (Ctrl+C para encerrar)...");

            byte[] buffer = new byte[TAMANHO_BUFFER];

            while (true) {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacote);

                String texto = new String(pacote.getData(), pacote.getOffset(), pacote.getLength(),
                    StandardCharsets.UTF_8).trim();

                Leitura leitura = Leitura.analisar(texto, pacote.getAddress().getHostAddress());

                if (leitura == null) {
                    System.err.println("Datagrama fora do formato esperado, descartado: " + texto);
                    continue;
                }

                leitura.acumular(ultimasLeituras.get(leitura.id));
                ultimasLeituras.put(leitura.id, leitura);

                exibirPainel(ultimasLeituras, porta);
            }

        } catch (IOException e) {
            System.err.println("Erro no Servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void exibirPainel(Map<String, Leitura> ultimasLeituras, int porta) {
        long recebidos = 0;
        long perdidos = 0;

        for (Leitura leitura : ultimasLeituras.values()) {
            recebidos += leitura.recebidos;
            perdidos += leitura.perdidos;
        }

        StringBuilder painel = new StringBuilder(LIMPAR_TELA);
        painel.append("=== Monitoramento de sensores em tempo real (UDP :").append(porta).append(") ===\n");
        painel.append(String.format("Atualizado em %s | sensores: %d | recebidos: %d | perdidos: %d%n%n",
            LocalTime.now().format(HORA), ultimasLeituras.size(), recebidos, perdidos));
        painel.append(String.format("%-12s %-12s %-12s %-16s %8s %10s %9s %9s%n",
            "SENSOR", "GRANDEZA", "LEITURA", "ORIGEM", "SEQ", "RECEBIDOS", "PERDIDOS", "HORA"));

        for (Leitura leitura : ultimasLeituras.values()) {
            painel.append(leitura.formatarLinha()).append('\n');
        }

        System.out.print(painel);
        System.out.flush();
    }

    private static class Leitura {
        private final String id;
        private final long sequencia;
        private final String grandeza;
        private final double valor;
        private final String unidade;
        private final String origem;
        private final LocalTime instante = LocalTime.now();

        private long recebidos = 1;
        private long perdidos = 0;

        private Leitura(String id, long sequencia, String grandeza, double valor, String unidade, String origem) {
            this.id = id;
            this.sequencia = sequencia;
            this.grandeza = grandeza;
            this.valor = valor;
            this.unidade = unidade;
            this.origem = origem;
        }

        private static Leitura analisar(String texto, String origem) {
            String[] campos = texto.split(";");

            if (campos.length != 5) {
                return null;
            }

            try {
                return new Leitura(campos[0], Long.parseLong(campos[1]), campos[2],
                    Double.parseDouble(campos[3]), campos[4], origem);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private void acumular(Leitura anterior) {
            if (anterior == null) {
                return;
            }

            recebidos = anterior.recebidos + 1;
            perdidos = anterior.perdidos + Math.max(0, sequencia - anterior.sequencia - 1);
        }

        private String formatarLinha() {
            return String.format(Locale.US, "%-12s %-12s %-12s %-16s %8d %10d %9d %9s",
                id, grandeza, String.format(Locale.US, "%.1f %s", valor, unidade), origem,
                sequencia, recebidos, perdidos, instante.format(HORA));
        }
    }
}
