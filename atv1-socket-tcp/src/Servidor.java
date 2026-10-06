// Traz BufferedReader, InputStreamReader, PrintWriter e IOException.
// Toda comunicação em Java é tratada como fluxo (stream), e essas classes
// são as que transformam bytes do socket em texto e texto de volta em bytes.
import java.io.*;
// Traz ServerSocket e Socket, as duas pontas do TCP no lado do servidor.
import java.net.*;

public class Servidor {
    public static void main(String[] args) {
        // ServerSocket é a "recepção": pede ao sistema operacional a porta 12345
        // e passa a escutar pedidos de conexão. Por ele não trafega mensagem nenhuma.
        // O try-with-resources garante que a porta seja devolvida ao SO mesmo se
        // uma exceção estourar no meio do bloco.
        try (ServerSocket servidor = new ServerSocket(12345)) {
            // A partir daqui a porta já está reservada: um segundo servidor na mesma
            // porta falharia. Ninguém conectou ainda.
            System.out.println("Servidor aguardando conexão...");

            // accept() BLOQUEIA a thread até alguém conectar. É por isso que o
            // servidor precisa subir antes do cliente.
            // Ele devolve um Socket NOVO, exclusivo desse cliente: a recepção
            // (ServerSocket) continua livre, e é esse socket que vira a conversa.
            Socket socket = servidor.accept();
            System.out.println("Cliente conectado!");

            // Pilha de três camadas, cada uma resolvendo um problema (padrão Decorator):
            //   socket.getInputStream() -> bytes crus que chegam pela rede
            //   InputStreamReader       -> converte bytes em caracteres usando o charset
            //   BufferedReader          -> acumula em memória e oferece readLine()
            // Sem o BufferedReader eu teria que procurar o '\n' byte a byte na mão.
            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            // Caminho inverso: recebe String, converte em bytes e escreve no socket.
            // O 'true' é o autoFlush, e ele é essencial: sem ele o PrintWriter
            // acumularia o texto no buffer e só enviaria quando o buffer enchesse.
            // O cliente ficaria travado esperando uma resposta que nunca saiu da
            // memória do servidor - um deadlock silencioso, sem erro na tela.
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

            // Guarda cada mensagem lida; declarada fora do laço para ser usada na condição.
            String mensagemCliente;

            // O coração da modificação pedida pelo enunciado: em vez de ler uma única
            // mensagem, o servidor fica lendo enquanto houver mensagem.
            // readLine() bloqueia até encontrar um '\n' e devolve a linha sem ele.
            // Esse '\n' é a fronteira de mensagem: o TCP entrega um fluxo contínuo de
            // bytes e NÃO preserva os limites de cada envio, então quem define onde uma
            // mensagem termina é o protocolo de aplicação - aqui, a quebra de linha.
            // Devolve null quando o cliente fecha a conexão, encerrando o laço.
            while ((mensagemCliente = entrada.readLine()) != null) {
                System.out.println("Cliente disse: " + mensagemCliente);

                // Condição de parada combinada entre as duas pontas.
                // equalsIgnoreCase com a constante à esquerda evita NullPointerException
                // caso mensagemCliente fosse nula, e aceita "SAIR", "Sair", "sair".
                if ("sair".equalsIgnoreCase(mensagemCliente)) {
                    System.out.println("Cliente solicitou o encerramento. Fechando conexão.");
                    // break sai do laço sem responder: o cliente não espera resposta do "sair".
                    break;
                }

                // Eco: devolve a mensagem prefixada. println acrescenta o '\n' que o
                // readLine() do cliente usa para saber onde a resposta termina.
                saida.println("Servidor recebeu: " + mensagemCliente);
            }

            // Fecha a conversa. Este socket veio do accept() e NÃO está no
            // try-with-resources da linha de cima, que cuida apenas do ServerSocket.
            socket.close();

        } catch (IOException e) {
            // Qualquer falha de rede ou de leitura cai aqui: cabo solto, cliente
            // derrubado, porta já em uso. IOException é checada justamente porque
            // comunicação pode falhar por motivos fora do controle do programa.
            System.err.println("Erro no Servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
