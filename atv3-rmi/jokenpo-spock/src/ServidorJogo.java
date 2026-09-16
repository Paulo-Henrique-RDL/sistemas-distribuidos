import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorJogo {
    private static final int PORTA_PADRAO = 1100;

    public static void main(String[] args) {
        int porta = PORTA_PADRAO;

        if (args.length > 0) {
            try {
                porta = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: " + args[0] + ". Usando " + PORTA_PADRAO + ".");
            }
        }

        if (args.length > 1) {
            System.setProperty("java.rmi.server.hostname", args[1]);
        }

        // Um cliente que caiu não pode travar a rodada esperando resposta indefinidamente.
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "5000");

        try {
            Registry registro = LocateRegistry.createRegistry(porta);
            JogoImpl jogo = new JogoImpl(porta);
            registro.rebind(Jogo.NOME_NO_REGISTRO, jogo);

            String endereco = System.getProperty("java.rmi.server.hostname", "localhost");
            System.out.println("Pedra, Papel, Tesoura, Lagarto e Spock publicado como \"" + Jogo.NOME_NO_REGISTRO
                + "\" em " + endereco + ":" + porta + ".");
            System.out.printf("De %d a %d jogadores | %d s entre rodadas | %d s para jogar.%n",
                JogoImpl.MINIMO_JOGADORES, JogoImpl.MAXIMO_JOGADORES,
                JogoImpl.SEGUNDOS_ENTRE_RODADAS, JogoImpl.SEGUNDOS_PARA_JOGAR);

            jogo.executar();

        } catch (RemoteException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
