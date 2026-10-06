import java.rmi.Remote;
import java.rmi.RemoteException;

// Primeira das DUAS interfaces remotas do jogo. Esta é o sentido comum do RMI:
// o que o SERVIDOR oferece e o cliente chama.
// A outra, Jogador, inverte os papéis - e é ela que resolve a exigência do
// enunciado de o servidor avisar todo mundo sem ser perguntado.
public interface Jogo extends Remote {
    // Nome de publicação no registry, compartilhado entre servidor e cliente.
    String NOME_NO_REGISTRO = "JokenpoSpock";

    // Entrada na sala. Recebe o nome e, principalmente, o objeto remoto do
    // próprio cliente: é passando esta referência que o cliente autoriza o
    // servidor a chamá-lo de volta depois.
    // Declara DUAS exceções: RemoteException para falha de rede e
    // EntradaRecusadaException para recusa de regra (nome repetido, sala cheia).
    // Separar as duas deixa claro, em quem chama, o que é problema de infra e o
    // que é resposta legítima do jogo.
    void entrar(String nome, Jogador jogador) throws RemoteException, EntradaRecusadaException;

    // Registra a jogada da rodada. Devolve String em vez de void para o cliente
    // saber o que aconteceu: aceita, fora do prazo, repetida ou entrou tarde.
    // O Gesto é um enum, logo Serializable: o RMI o envia sem conversão manual.
    String jogar(String nome, Gesto gesto) throws RemoteException;

    // O enunciado diz que é o CLIENTE vencedor quem manda a mensagem aos
    // perdedores. Mas um cliente não conhece o endereço dos outros - quem tem a
    // lista é o servidor. Então o vencedor chama este método e o servidor repassa.
    void enviarAosPerdedores(String nome, String mensagem) throws RemoteException;

    // Saída combinada, para o servidor remover o jogador na hora em vez de
    // descobrir só quando uma chamada de callback falhar.
    void sair(String nome) throws RemoteException;
}
