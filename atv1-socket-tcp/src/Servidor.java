import java.io.*;
import java.net.*;

public class Servidor {
    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(12345)) {
            System.out.println("Servidor aguardando conexão...");

            Socket socket = servidor.accept();
            System.out.println("Cliente conectado!");

            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

            String mensagemCliente;

            while ((mensagemCliente = entrada.readLine()) != null) {
                System.out.println("Cliente disse: " + mensagemCliente);

                if ("sair".equalsIgnoreCase(mensagemCliente)) {
                    System.out.println("Cliente solicitou o encerramento. Fechando conexão.");
                    break;
                }

                saida.println("Servidor recebeu: " + mensagemCliente);
            }

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}