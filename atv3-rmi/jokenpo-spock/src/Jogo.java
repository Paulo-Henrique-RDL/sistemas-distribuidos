import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Jogo extends Remote {
    String NOME_NO_REGISTRO = "JokenpoSpock";

    void entrar(String nome, Jogador jogador) throws RemoteException, EntradaRecusadaException;

    String jogar(String nome, Gesto gesto) throws RemoteException;

    void enviarAosPerdedores(String nome, String mensagem) throws RemoteException;

    void sair(String nome) throws RemoteException;
}
