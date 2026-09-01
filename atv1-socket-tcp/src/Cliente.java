import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345)) {
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader entradaServidor = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            BufferedReader entradaConsole = new BufferedReader(new InputStreamReader(System.in));

            String mensagemUsuario;
            System.out.println("Conectado ao servidor. Digite suas mensagens ('sair' para encerrar).");

            while (true) {
                System.out.print("> ");
                mensagemUsuario = entradaConsole.readLine();

                saida.println(mensagemUsuario);

                if ("sair".equalsIgnoreCase(mensagemUsuario)) {
                    break;
                }

                String respostaServidor = entradaServidor.readLine();
                System.out.println("Resposta do Servidor: " + respostaServidor);
            }

        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Conexão encerrada.");
    }
}