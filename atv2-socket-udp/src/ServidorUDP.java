// IOException: comunicação em rede pode falhar, e o Java obriga a tratar.
import java.io.*;
// DatagramSocket e DatagramPacket: o par do UDP, sem conexão.
import java.net.*;
// Charset explícito para converter bytes em texto sempre do mesmo jeito,
// independente da configuração da máquina onde o programa roda.
import java.nio.charset.StandardCharsets;
// Marca o horário de cada leitura exibida no painel.
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
// Map, TreeMap, Locale: estrutura do painel e formatação numérica.
import java.util.*;

public class ServidorUDP {
    private static final int PORTA_PADRAO = 12345;
    // 1024 bytes é bem mais do que uma leitura precisa (ela tem algumas dezenas),
    // mas o buffer precisa caber o maior datagrama possível: o que não couber é
    // descartado silenciosamente pelo UDP, sem erro.
    private static final int TAMANHO_BUFFER = 1024;
    // Sequência ANSI: move o cursor para o topo e limpa a tela. É o que permite
    // redesenhar o painel no lugar, em vez de empurrar linhas para baixo.
    private static final String LIMPAR_TELA = "\033[H\033[2J";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        int porta = PORTA_PADRAO;

        // A porta vem por argumento para permitir subir dois servidores na mesma
        // máquina durante os testes, sem recompilar.
        if (args.length > 0) {
            try {
                porta = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // Argumento inválido não derruba o servidor: avisa e usa o padrão.
                System.err.println("Porta inválida: " + args[0] + ". Usando " + PORTA_PADRAO + ".");
            }
        }

        // O estado do servidor: a ÚLTIMA leitura de cada sensor, indexada pelo id.
        // TreeMap e não HashMap porque ele mantém as chaves ordenadas: as linhas do
        // painel saem sempre na mesma ordem. Com HashMap elas dançariam na tela a
        // cada atualização, já que a ordem de iteração não é garantida.
        Map<String, Leitura> ultimasLeituras = new TreeMap<>();

        // Aqui o socket TEM porta fixa, porque é o endereço que os sensores procuram.
        // Diferença central para o TCP da ATV1: não existe ServerSocket, não existe
        // accept(), não existe conexão. Este único socket atende todos os sensores.
        try (DatagramSocket socket = new DatagramSocket(porta)) {
            System.out.println("Servidor UDP escutando na porta " + porta + ".");
            System.out.println("Aguardando leituras dos sensores (Ctrl+C para encerrar)...");

            // O array é criado uma vez e reaproveitado a cada volta do laço,
            // evitando alocar 1 KB por datagrama recebido.
            byte[] buffer = new byte[TAMANHO_BUFFER];

            // Laço infinito: o servidor vive recebendo. Quem encerra é o Ctrl+C.
            while (true) {
                // O pacote embrulha o buffer. Depois do receive ele carrega também
                // o endereço e a porta de quem enviou, e o tamanho real recebido.
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                // BLOQUEIA até chegar um datagrama, de qualquer sensor.
                // Como não há conexão, não importa quem envia nem quantos são.
                socket.receive(pacote);

                // Converte apenas os bytes realmente recebidos (getLength) em texto.
                // Usar buffer.length aqui traria lixo da leitura anterior, já que o
                // array é reaproveitado. O trim remove sobras de espaço/quebra.
                String texto = new String(pacote.getData(), pacote.getOffset(), pacote.getLength(),
                    StandardCharsets.UTF_8).trim();

                // getAddress() é o endereço de origem: no UDP ele vem em cada pacote,
                // porque não existe uma conexão que já identifique o remetente.
                Leitura leitura = Leitura.analisar(texto, pacote.getAddress().getHostAddress());

                // Qualquer um pode enviar para esta porta, então o servidor não confia
                // no conteúdo: datagrama fora do formato é descartado e o laço segue.
                // Um pacote malformado não pode derrubar o monitoramento.
                if (leitura == null) {
                    System.err.println("Datagrama fora do formato esperado, descartado: " + texto);
                    continue;
                }

                // Compara com a leitura anterior do MESMO sensor para descobrir
                // quantos datagramas se perderam no caminho.
                leitura.acumular(ultimasLeituras.get(leitura.id));
                // Substitui a anterior: o servidor guarda só o estado atual, não o histórico.
                ultimasLeituras.put(leitura.id, leitura);

                // Redesenha o painel inteiro a cada datagrama recebido.
                exibirPainel(ultimasLeituras, porta);
            }

        } catch (IOException e) {
            // Porta já em uso ou falha no socket.
            System.err.println("Erro no Servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void exibirPainel(Map<String, Leitura> ultimasLeituras, int porta) {
        long recebidos = 0;
        long perdidos = 0;

        // Totais da sessão: cada Leitura carrega os contadores acumulados do seu
        // sensor, então basta somar o que está no mapa.
        for (Leitura leitura : ultimasLeituras.values()) {
            recebidos += leitura.recebidos;
            perdidos += leitura.perdidos;
        }

        // Monta o painel inteiro em memória antes de imprimir. Se eu imprimisse
        // linha a linha com println, a tela piscaria entre a limpeza e o desenho.
        // StringBuilder evita criar uma String nova a cada concatenação.
        StringBuilder painel = new StringBuilder(LIMPAR_TELA);
        painel.append("=== Monitoramento de sensores em tempo real (UDP :").append(porta).append(") ===\n");
        // %n em vez de \n: o Java usa a quebra de linha da plataforma.
        painel.append(String.format("Atualizado em %s | sensores: %d | recebidos: %d | perdidos: %d%n%n",
            LocalTime.now().format(HORA), ultimasLeituras.size(), recebidos, perdidos));
        // Larguras fixas (%-12s alinha à esquerda, %8s à direita) para as colunas
        // ficarem paradas mesmo quando o valor muda de tamanho.
        painel.append(String.format("%-12s %-12s %-12s %-16s %8s %10s %9s %9s%n",
            "SENSOR", "GRANDEZA", "LEITURA", "ORIGEM", "SEQ", "RECEBIDOS", "PERDIDOS", "HORA"));

        // Uma linha por sensor, na ordem alfabética garantida pelo TreeMap.
        for (Leitura leitura : ultimasLeituras.values()) {
            painel.append(leitura.formatarLinha()).append('\n');
        }

        // print (não println) e um flush explícito: tudo vai para a tela de uma vez.
        System.out.print(painel);
        System.out.flush();
    }

    // Classe aninhada estática: representa uma leitura já validada, com os
    // contadores daquele sensor. Estática porque não precisa de acesso ao
    // ServidorUDP - é só um agrupamento de dados com comportamento próprio.
    private static class Leitura {
        // Todos final: depois de criada, a leitura não muda. O que evolui são os
        // contadores abaixo, calculados na chegada.
        private final String id;
        private final long sequencia;
        private final String grandeza;
        private final double valor;
        private final String unidade;
        private final String origem;
        // Momento em que o servidor recebeu, não em que o sensor mediu.
        private final LocalTime instante = LocalTime.now();

        // Começa em 1: a própria leitura que acabou de chegar.
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

        // Fábrica que valida: devolve null em vez de lançar exceção, porque
        // datagrama inválido é esperado aqui, não é situação excepcional.
        private static Leitura analisar(String texto, String origem) {
            // Protocolo de aplicação definido por mim: id;sequencia;grandeza;valor;unidade
            // Como o UDP entrega só bytes, o formato da mensagem é escolha minha.
            String[] campos = texto.split(";");

            // Confere o número de campos antes de tentar converter qualquer coisa.
            if (campos.length != 5) {
                return null;
            }

            try {
                return new Leitura(campos[0], Long.parseLong(campos[1]), campos[2],
                    Double.parseDouble(campos[3]), campos[4], origem);
            } catch (NumberFormatException e) {
                // Sequência ou valor não numéricos: descarta.
                return null;
            }
        }

        // O ponto central da atividade: medir a perda que o UDP não avisa.
        private void acumular(Leitura anterior) {
            // Primeira leitura deste sensor: não há com o que comparar.
            if (anterior == null) {
                return;
            }

            recebidos = anterior.recebidos + 1;
            // O salto na numeração revela os datagramas que se perderam: se a
            // anterior era 40 e chegou a 42, a 41 ficou pelo caminho (42-40-1 = 1).
            // Math.max com zero protege dois casos reais: pacote que chega fora de
            // ordem (o UDP não garante ordem) e sensor reiniciado, quando a
            // sequência volta a 1 e a diferença ficaria negativa.
            perdidos = anterior.perdidos + Math.max(0, sequencia - anterior.sequencia - 1);
        }

        private String formatarLinha() {
            // Locale.US força o ponto como separador decimal: sem isso, em máquina
            // configurada em português o valor sairia com vírgula e desalinharia.
            return String.format(Locale.US, "%-12s %-12s %-12s %-16s %8d %10d %9d %9s",
                id, grandeza, String.format(Locale.US, "%.1f %s", valor, unidade), origem,
                sequencia, recebidos, perdidos, instante.format(HORA));
        }
    }
}
