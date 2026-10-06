import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorControle {
    private static final int PORTA_PADRAO = 1099;

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

        try {
            Registry registro = LocateRegistry.createRegistry(porta);
            ControleRemotoImpl televisor = new ControleRemotoImpl(porta);
            registro.rebind(ControleRemoto.NOME_NO_REGISTRO, televisor);

            String endereco = System.getProperty("java.rmi.server.hostname", "localhost");
            System.out.println("TV pronta. Controle remoto publicado como \"" + ControleRemoto.NOME_NO_REGISTRO
                + "\" em " + endereco + ":" + porta + ".");
            System.out.println("Aguardando comandos (Ctrl+C para encerrar)...");

        } catch (RemoteException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
