// Leitura do teclado, igual às atividades anteriores.
import java.io.*;
// NotBoundException: o registry existe, mas ninguém publicou aquele nome nele.
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
            // As duas linhas que concentram todo o RMI do lado do cliente:
            //   getRegistry(host, porta) -> alcança o catálogo na outra máquina
            //   lookup(nome)             -> devolve o STUB do objeto publicado
            // O stub é um objeto gerado pelo RMI que implementa ControleRemoto.
            // Ele não tem o estado da TV: cada método dele empacota os argumentos,
            // envia pela rede, espera a execução no servidor e devolve o retorno.
            // O cast é necessário porque lookup devolve Remote, o tipo genérico.
            ControleRemoto controle = (ControleRemoto) LocateRegistry.getRegistry(host, porta)
                .lookup(ControleRemoto.NOME_NO_REGISTRO);

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Controle conectado à TV em " + host + ":" + porta + ".");

            // Daqui para baixo não há mais nada de rede: é um menu comum chamando
            // métodos de um objeto. Essa é exatamente a promessa do RMI - a chamada
            // remota se parece com uma chamada local.
            while (true) {
                exibirMenu();
                String opcao = console.readLine();

                // readLine devolve null quando a entrada acaba (Ctrl+Z / fim de pipe),
                // e "0" é a saída pelo menu. Os dois encerram o laço.
                if (opcao == null || "0".equals(opcao.trim())) {
                    break;
                }

                String resposta;

                // switch sobre String: cada caso dispara uma chamada remota distinta.
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
                        // Única opção que precisa de um segundo dado do usuário.
                        System.out.print("Número do canal: ");
                        String numero = console.readLine();

                        try {
                            // parseInt pode lançar NumberFormatException (texto não
                            // numérico) e o trim pode lançar NullPointerException se
                            // a entrada tiver acabado. O multi-catch trata os dois
                            // no mesmo lugar, sem derrubar o controle.
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
                        // Opção inválida é tratada aqui, sem ida à rede.
                        resposta = "Opção inválida.";
                }

                // A resposta já vem pronta do servidor: o cliente só exibe.
                System.out.println("TV: " + resposta);
            }

        } catch (NotBoundException e) {
            // O registry respondeu, mas o nome não está publicado: normalmente
            // significa que o servidor não chegou a subir.
            System.err.println("Nenhum controle remoto publicado em " + host + ":" + porta + ".");
        } catch (IOException e) {
            // RemoteException é subclasse de IOException, então este catch cobre
            // tanto falha de rede quanto falha na leitura do teclado.
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
        // print sem ln: o cursor fica na linha do prompt.
        System.out.print("> ");
    }
}
