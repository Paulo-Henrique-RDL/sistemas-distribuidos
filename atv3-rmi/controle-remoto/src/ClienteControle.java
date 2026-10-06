import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;

public class ClienteControle {
    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 1099;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = PORTA_PADRAO;

        if (args.length > 1) {
            try {
                porta = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: " + args[1] + ". Usando " + PORTA_PADRAO + ".");
            }
        }

        try {
            ControleRemoto controle = (ControleRemoto) LocateRegistry.getRegistry(host, porta)
                .lookup(ControleRemoto.NOME_NO_REGISTRO);

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Controle conectado à TV em " + host + ":" + porta + ".");

            while (true) {
                exibirMenu();
                String opcao = console.readLine();

                if (opcao == null || "0".equals(opcao.trim())) {
                    break;
                }

                String resposta;

                switch (opcao.trim()) {
                    case "1":
                        resposta = controle.ligarDesligar();
                        break;
                    case "2":
                        resposta = controle.aumentarVolume();
                        break;
                    case "3":
                        resposta = controle.diminuirVolume();
                        break;
                    case "4":
                        resposta = controle.proximoCanal();
                        break;
                    case "5":
                        resposta = controle.canalAnterior();
                        break;
                    case "6":
                        System.out.print("Número do canal: ");
                        String numero = console.readLine();

                        try {
                            resposta = controle.irParaCanal(Integer.parseInt(numero.trim()));
                        } catch (NumberFormatException | NullPointerException e) {
                            resposta = "Digite apenas o número do canal.";
                        }
                        break;
                    case "7":
                        resposta = controle.alternarMudo();
                        break;
                    case "8":
                        resposta = controle.status();
                        break;
                    default:
                        resposta = "Opção inválida.";
                }

                System.out.println("TV: " + resposta);
            }

        } catch (NotBoundException e) {
            System.err.println("Nenhum controle remoto publicado em " + host + ":" + porta + ".");
        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Controle desconectado.");
    }

    private static void exibirMenu() {
        System.out.println();
        System.out.println("=== Controle remoto ===");
        System.out.println("1) Ligar/desligar   2) Volume +   3) Volume -");
        System.out.println("4) Canal +          5) Canal -    6) Ir para canal");
        System.out.println("7) Mudo             8) Status     0) Sair");
        System.out.print("> ");
    }
}
