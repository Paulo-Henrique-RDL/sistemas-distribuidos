import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Jogador extends Remote {
    void rodadaAberta(int rodada, int segundosParaJogar) throws RemoteException;

    void aviso(String texto) throws RemoteException;

    void vitoria(int rodada, Gesto gesto) throws RemoteException;

    void mensagemDoVencedor(String vencedor, String mensagem) throws RemoteException;
}
