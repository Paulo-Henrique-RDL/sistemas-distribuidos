import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorJogo {
    // 1100 e não 1099 para o jogo e o controle remoto poderem rodar ao mesmo
    // tempo na mesma máquina, sem disputar a porta.
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

        // IP desta máquina, gravado dentro do stub entregue aos clientes.
        // Precisa ser definido antes de exportar o objeto.
        if (args.length > 1) {
            System.setProperty("java.rmi.server.hostname", args[1]);
        }

        // Um cliente que caiu não pode travar a rodada esperando resposta indefinidamente.
        // Sem este limite, o laço de rodadas ficaria parado num cliente que sumiu
        // da rede sem fechar a conexão - a máquina desligada na tomada, por exemplo.
        // Com ele, a chamada falha em 5 s, e o paraTodos remove o jogador.
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "5000");

        try {
            // Registry embutido no processo: um comando só sobe tudo.
            Registry registro = LocateRegistry.createRegistry(porta);
            // Construir já exporta o objeto, na mesma porta do registry.
            JogoImpl jogo = new JogoImpl(porta);
            registro.rebind(Jogo.NOME_NO_REGISTRO, jogo);

            String endereco = System.getProperty("java.rmi.server.hostname", "localhost");
            System.out.println("Pedra, Papel, Tesoura, Lagarto e Spock publicado como \"" + Jogo.NOME_NO_REGISTRO
                + "\" em " + endereco + ":" + porta + ".");
            // Mostra as quatro regras do enunciado já na subida, o que ajuda na
            // demonstração do vídeo.
            System.out.printf("De %d a %d jogadores | %d s entre rodadas | %d s para jogar.%n",
                JogoImpl.MINIMO_JOGADORES, JogoImpl.MAXIMO_JOGADORES,
                JogoImpl.SEGUNDOS_ENTRE_RODADAS, JogoImpl.SEGUNDOS_PARA_JOGAR);

            // Diferente do controle remoto, aqui o main NÃO termina: ele vira o
            // relógio do jogo. Este laço conduz intervalo, abertura e encerramento
            // das rodadas, enquanto as chamadas dos clientes são atendidas em
            // paralelo, cada uma em sua própria thread do RMI.
            jogo.executar();

        } catch (RemoteException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
