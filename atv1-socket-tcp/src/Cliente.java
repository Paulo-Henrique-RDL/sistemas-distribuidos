// Mesmas classes de fluxo do servidor: aqui elas tratam tanto o socket
// quanto o teclado, porque em Java os dois são streams.
import java.io.*;
// Traz Socket, a ponta do cliente no TCP.
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        // No cliente não existe a separação recepção/sala: este construtor já é a
        // conexão. Ele resolve o nome "localhost" para um endereço, faz o handshake
        // de três vias do TCP e só retorna quando a conexão está estabelecida -
        // ou lança exceção se não houver ninguém escutando na porta 12345.
        // O try-with-resources fecha o socket ao sair do bloco.
        try (Socket socket = new Socket("localhost", 12345)) {
            // Canal de escrita para o servidor, com autoFlush ligado (o 'true').
            // Sem ele, a mensagem ficaria no buffer e o cliente travaria logo adiante,
            // esperando uma resposta de algo que nunca chegou a ser enviado.
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

            // Canal de leitura das respostas do servidor, com a mesma pilha de três
            // camadas do outro lado: bytes -> caracteres -> linhas.
            BufferedReader entradaServidor = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            // Terceiro fluxo, agora do teclado. System.in também é um InputStream,
            // então a mesma pilha serve para ler o que o usuário digita.
            // São três canais distintos: teclado, envio ao servidor e resposta dele.
            BufferedReader entradaConsole = new BufferedReader(new InputStreamReader(System.in));

            // Guarda a linha digitada; declarada fora do laço para sobreviver a cada volta.
            String mensagemUsuario;
            System.out.println("Conectado ao servidor. Digite suas mensagens ('sair' para encerrar).");

            // Laço infinito: quem decide a parada é o conteúdo digitado, não uma contagem.
            while (true) {
                // print (sem ln) deixa o cursor na mesma linha, formando o prompt.
                System.out.print("> ");
                // Bloqueia até o usuário apertar Enter. Devolve a linha sem o '\n'.
                mensagemUsuario = entradaConsole.readLine();

                // Envia ao servidor. println recoloca o '\n' que o readLine() tirou,
                // e é esse caractere que marca o fim da mensagem para o outro lado.
                saida.println(mensagemUsuario);

                // Repare na ORDEM: o "sair" é enviado na linha acima e só depois o
                // cliente sai do laço. Se saísse antes de enviar, o servidor ficaria
                // preso no readLine() sem saber que a conversa acabou. O encerramento
                // é negociado, não abrupto.
                if ("sair".equalsIgnoreCase(mensagemUsuario)) {
                    break;
                }

                // Bloqueia até o servidor responder. É aqui que as duas pontas entram
                // em compasso travado: um envia, o outro responde, alternadamente.
                // Se qualquer lado quebrasse esse ritmo - mandando duas mensagens
                // seguidas, ou não respondendo - os dois ficariam esperando um ao outro.
                String respostaServidor = entradaServidor.readLine();
                System.out.println("Resposta do Servidor: " + respostaServidor);
            }

        } catch (IOException e) {
            // Cai aqui se o servidor não estiver no ar, se a conexão cair no meio
            // da conversa ou se a leitura do teclado falhar.
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
        // Fora do try: imprime tanto na saída normal quanto depois de uma falha,
        // e o socket já foi fechado pelo try-with-resources neste ponto.
        System.out.println("Conexão encerrada.");
    }
}
