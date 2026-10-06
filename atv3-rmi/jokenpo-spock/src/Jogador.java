import java.rmi.Remote;
import java.rmi.RemoteException;

// A interface do CALLBACK, e o ponto central desta atividade.
// No RMI quem chama é sempre o cliente; o servidor só responde. Mas o enunciado
// exige o contrário: o servidor precisa avisar que a rodada abriu, comunicar o
// empate a todos, mandar resultado e placar.
// A solução é inverter os papéis: cada CLIENTE implementa esta interface,
// exporta o próprio objeto e o entrega ao servidor no entrar(). A partir daí
// o servidor é quem chama, e o cliente é quem atende.
public interface Jogador extends Remote {
    // Abre a janela de jogada. O servidor informa o número da rodada e quantos
    // segundos restam, para o cliente exibir o menu no momento certo.
    void rodadaAberta(int rodada, int segundosParaJogar) throws RemoteException;

    // Canal genérico de texto: entrada e saída de jogadores, contagem para a
    // próxima rodada, resultado, placar, empate e aviso de desclassificação.
    // Mantido genérico de propósito - o servidor monta o texto, e o cliente só
    // imprime, sem precisar conhecer as regras do jogo.
    void aviso(String texto) throws RemoteException;

    // Avisa SÓ o vencedor, e é o gatilho da exigência do enunciado: ao receber
    // esta chamada, o cliente responde mandando a mensagem aos perdedores.
    void vitoria(int rodada, Gesto gesto) throws RemoteException;

    // Entrega a mensagem do vencedor a quem perdeu, com o nome de quem mandou.
    void mensagemDoVencedor(String vencedor, String mensagem) throws RemoteException;
}
