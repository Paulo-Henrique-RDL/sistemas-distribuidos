import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ControleRemoto extends Remote {
    String NOME_NO_REGISTRO = "ControleRemoto";

    String ligarDesligar() throws RemoteException;

    String aumentarVolume() throws RemoteException;

    String diminuirVolume() throws RemoteException;

    String proximoCanal() throws RemoteException;

    String canalAnterior() throws RemoteException;

    String irParaCanal(int canal) throws RemoteException;

    String alternarMudo() throws RemoteException;

    String status() throws RemoteException;
}
