import java.io.*;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public class ClienteJogo {
    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 1100;
    private static final String MENSAGEM_AOS_PERDEDORES = "Perdeu Loser...Tente na próxima.";

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = PORTA_PADRAO;
        String nome = args.length > 2 ? args[2] : "jogador-" + (100 + new Random().nextInt(900));

        if (args.length > 1) {
            try {
                porta = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: " + args[1] + ". Usando " + PORTA_PADRAO + ".");
            }
        }

        if (args.length > 3) {
            System.setProperty("java.rmi.server.hostname", args[3]);
        }

        JogadorImpl jogador = null;

        try {
            Jogo jogo = (Jogo) LocateRegistry.getRegistry(host, porta).lookup(Jogo.NOME_NO_REGISTRO);
            jogador = new JogadorImpl(jogo, nome);
            jogo.entrar(nome, jogador);

            System.out.println("Digite 1 a 5 (ou o nome do gesto) quando a rodada abrir. 'sair' encerra.");
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String linha;

            while ((linha = console.readLine()) != null) {
                linha = linha.trim();

                if (linha.isEmpty()) {
                    continue;
                }

                if ("sair".equalsIgnoreCase(linha)) {
                    break;
                }

                Gesto gesto = Gesto.de(linha);

                if (gesto == null) {
                    System.out.println("Opção inválida. Use 1 a 5, o nome do gesto ou 'sair'.");
                    continue;
                }

                System.out.println(jogo.jogar(nome, gesto));
            }

            jogo.sair(nome);
            System.out.println("Você saiu do jogo.");

        } catch (EntradaRecusadaException e) {
            System.out.println("Entrada recusada: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Nenhum jogo publicado em " + host + ":" + porta + ".");
        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (jogador != null) {
                try {
                    UnicastRemoteObject.unexportObject(jogador, true);
                } catch (NoSuchObjectException ignorada) {
                }
            }
        }
    }

    static class JogadorImpl extends UnicastRemoteObject implements Jogador {
        private final Jogo jogo;
        private final String nome;

        JogadorImpl(Jogo jogo, String nome) throws RemoteException {
            super();
            this.jogo = jogo;
            this.nome = nome;
        }

        @Override
        public void rodadaAberta(int rodada, int segundosParaJogar) {
            System.out.printf("%n=== Rodada %d: você tem %d s para jogar ===%n", rodada, segundosParaJogar);
            System.out.println("1) Pedra  2) Papel  3) Tesoura  4) Lagarto  5) Spock");
            System.out.print("Sua jogada: ");
        }

        @Override
        public void aviso(String texto) {
            System.out.println(texto);
        }

        @Override
        public void vitoria(int rodada, Gesto gesto) {
            System.out.printf("%nVocê venceu a rodada %d com %s! Avisando os perdedores...%n", rodada, gesto);

            // Em outra thread para devolver o controle logo ao servidor, que ainda está encerrando a rodada.
            new Thread(() -> {
                try {
                    jogo.enviarAosPerdedores(nome, MENSAGEM_AOS_PERDEDORES);
                } catch (RemoteException e) {
                    System.err.println("Não foi possível avisar os perdedores: " + e.getMessage());
                }
            }).start();
        }

        @Override
        public void mensagemDoVencedor(String vencedor, String mensagem) {
            System.out.printf("%n[%s] %s%n", vencedor, mensagem);
        }
    }
}
