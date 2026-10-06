import java.rmi.RemoteException;
// LocateRegistry cria ou localiza o registry, que é o "catálogo telefônico"
// onde objetos remotos ficam registrados por nome.
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorControle {
    // 1099 é a porta padrão do RMI, o equivalente ao 80 do HTTP.
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

        // Segundo argumento opcional: o IP desta máquina na rede.
        // Isso resolve um problema real do RMI entre computadores. O endereço do
        // servidor fica GRAVADO DENTRO do stub que o cliente recebe, e o Java o
        // deduz sozinho. Numa máquina com várias redes (Wi-Fi, cabo, VPN) ele pode
        // escolher a errada, e o cliente tenta conectar num endereço inalcançável.
        // A propriedade precisa ser definida ANTES de exportar o objeto, por isso
        // esta linha vem antes do new ControleRemotoImpl.
        if (args.length > 1) {
            System.setProperty("java.rmi.server.hostname", args[1]);
        }

        try {
            // Cria o registry DENTRO deste processo, em vez de exigir o comando
            // rmiregistry rodando à parte: um único "java ServidorControle" basta.
            Registry registro = LocateRegistry.createRegistry(porta);
            // Construir o objeto já o exporta (é o super(porta) da implementação).
            // A partir daqui ele está pronto para receber chamadas pela rede.
            ControleRemotoImpl televisor = new ControleRemotoImpl(porta);
            // Publica o objeto sob o nome combinado na interface.
            // rebind e não bind: rebind substitui um registro anterior de mesmo nome,
            // então reiniciar o servidor não quebra com "nome já em uso".
            registro.rebind(ControleRemoto.NOME_NO_REGISTRO, televisor);

            // Mostra o endereço efetivo para conferir, no vídeo, que é o esperado.
            String endereco = System.getProperty("java.rmi.server.hostname", "localhost");
            System.out.println("TV pronta. Controle remoto publicado como \"" + ControleRemoto.NOME_NO_REGISTRO
                + "\" em " + endereco + ":" + porta + ".");
            System.out.println("Aguardando comandos (Ctrl+C para encerrar)...");

            // Repare que não existe laço aqui, diferente das ATVs 1 e 2.
            // O main termina, mas o programa continua vivo: o RMI mantém threads
            // não-daemon escutando a porta enquanto houver objeto exportado.

        } catch (RemoteException e) {
            // Porta ocupada ou falha ao exportar o objeto.
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
