import java.io.*;
// Lançada ao tentar desexportar um objeto que já não está exportado.
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public class ClienteJogo {
    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 1100;
    // A frase exigida pelo enunciado, que o vencedor envia aos perdedores.
    private static final String MENSAGEM_AOS_PERDEDORES = "Perdeu Loser...Tente na próxima.";

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = PORTA_PADRAO;
        // Sem nome informado, sorteia um: evita colisão quando se abrem vários
        // clientes rápido para testar, já que o servidor recusa nome repetido.
        String nome = args.length > 2 ? args[2] : "jogador-" + (100 + new Random().nextInt(900));

        if (args.length > 1) {
            try {
                porta = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: " + args[1] + ". Usando " + PORTA_PADRAO + ".");
            }
        }

        // Quarto argumento: o IP DESTE cliente. Necessário porque aqui o cliente
        // também é servidor - ele exporta o próprio objeto, e o servidor do jogo
        // precisa alcançá-lo de volta. O endereço vai gravado no stub que o
        // cliente entrega, então definir isto antes de exportar é obrigatório.
        if (args.length > 3) {
            System.setProperty("java.rmi.server.hostname", args[3]);
        }

        // Declarado fora do try para continuar acessível no finally.
        JogadorImpl jogador = null;

        try {
            // Mesmo par de sempre: alcança o registry e pega o stub do jogo.
            Jogo jogo = (Jogo) LocateRegistry.getRegistry(host, porta).lookup(Jogo.NOME_NO_REGISTRO);
            // Cria e EXPORTA o objeto de callback deste cliente.
            jogador = new JogadorImpl(jogo, nome);
            // Entrega a referência ao servidor: a partir daqui o servidor pode
            // chamar este cliente. É a inversão que o jogo exige.
            jogo.entrar(nome, jogador);

            System.out.println("Digite 1 a 5 (ou o nome do gesto) quando a rodada abrir. 'sair' encerra.");
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String linha;

            // A thread principal só lê o teclado. Os avisos do servidor chegam em
            // paralelo, nas threads do RMI, e por isso aparecem na tela mesmo
            // enquanto esta linha está bloqueada esperando o Enter.
            while ((linha = console.readLine()) != null) {
                linha = linha.trim();

                // Enter vazio não vira jogada.
                if (linha.isEmpty()) {
                    continue;
                }

                if ("sair".equalsIgnoreCase(linha)) {
                    break;
                }

                Gesto gesto = Gesto.de(linha);

                // Entrada inválida é tratada aqui, sem incomodar o servidor.
                if (gesto == null) {
                    System.out.println("Opção inválida. Use 1 a 5, o nome do gesto ou 'sair'.");
                    continue;
                }

                // A resposta já vem pronta: registrada, fora do prazo ou repetida.
                System.out.println(jogo.jogar(nome, gesto));
            }

            // Saída combinada: o servidor remove na hora e avisa os outros.
            jogo.sair(nome);
            System.out.println("Você saiu do jogo.");

        } catch (EntradaRecusadaException e) {
            // Recusa por regra (nome em uso, sala cheia): não é erro de rede,
            // então usa a saída normal e mostra só a mensagem, sem pilha.
            System.out.println("Entrada recusada: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Nenhum jogo publicado em " + host + ":" + porta + ".");
        } catch (IOException e) {
            // Cobre RemoteException (subclasse de IOException) e falha de teclado.
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (jogador != null) {
                try {
                    // Passo essencial: um objeto exportado mantém threads vivas no
                    // RMI, e sem isto o programa não encerraria depois do 'sair'.
                    // O true força a retirada mesmo com chamadas em andamento.
                    UnicastRemoteObject.unexportObject(jogador, true);
                } catch (NoSuchObjectException ignorada) {
                    // Já estava desexportado: nada a fazer.
                }
            }
        }
    }

    // O objeto de CALLBACK: o cliente também vira servidor.
    // Estender UnicastRemoteObject aqui é o que permite ao servidor do jogo
    // chamar estes métodos pela rede. O construtor sem porta deixa o sistema
    // escolher uma qualquer, já que ninguém procura este objeto por endereço -
    // ele é entregue de mão em mão, no entrar().
    static class JogadorImpl extends UnicastRemoteObject implements Jogador {
        // Guarda o stub do jogo para poder responder ao servidor.
        private final Jogo jogo;
        private final String nome;

        JogadorImpl(Jogo jogo, String nome) throws RemoteException {
            // Exporta o objeto; daqui em diante ele pode receber chamadas.
            super();
            this.jogo = jogo;
            this.nome = nome;
        }

        // Daqui para baixo, todos os métodos rodam em threads do RMI, não na
        // thread principal: é o servidor quem os dispara.
        @Override
        public void rodadaAberta(int rodada, int segundosParaJogar) {
            System.out.printf("%n=== Rodada %d: você tem %d s para jogar ===%n", rodada, segundosParaJogar);
            System.out.println("1) Pedra  2) Papel  3) Tesoura  4) Lagarto  5) Spock");
            // print sem ln: deixa o prompt na mesma linha, esperando a digitação.
            System.out.print("Sua jogada: ");
        }

        @Override
        public void aviso(String texto) {
            // O servidor já manda o texto pronto; o cliente não interpreta nada.
            System.out.println(texto);
        }

        @Override
        public void vitoria(int rodada, Gesto gesto) {
            System.out.printf("%nVocê venceu a rodada %d com %s! Avisando os perdedores...%n", rodada, gesto);

            // Em outra thread para devolver o controle logo ao servidor, que ainda está encerrando a rodada.
            // Aqui se fecha o ciclo pedido pelo enunciado: o servidor avisou o
            // vencedor, e o vencedor responde chamando o servidor de volta, que
            // repassa a frase a quem perdeu. São três saltos, porque um cliente
            // não conhece o endereço dos outros.
            new Thread(() -> {
                try {
                    jogo.enviarAosPerdedores(nome, MENSAGEM_AOS_PERDEDORES);
                } catch (RemoteException e) {
                    // Falha aqui não derruba o cliente: a partida continua.
                    System.err.println("Não foi possível avisar os perdedores: " + e.getMessage());
                }
            }).start();
        }

        @Override
        public void mensagemDoVencedor(String vencedor, String mensagem) {
            // Chega a quem perdeu, com o nome de quem mandou entre colchetes.
            System.out.printf("%n[%s] %s%n", vencedor, mensagem);
        }
    }
}
