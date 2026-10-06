import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ControleRemotoImpl extends UnicastRemoteObject implements ControleRemoto {
    private static final int VOLUME_MAXIMO = 100;
    private static final int PASSO_VOLUME = 5;
    private static final int PRIMEIRO_CANAL = 1;
    private static final int ULTIMO_CANAL = 99;
    private static final String TV_DESLIGADA = "A TV está desligada. Ligue-a primeiro.";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private boolean ligada;
    private boolean mudo;
    private int volume = 20;
    private int canal = PRIMEIRO_CANAL;

    public ControleRemotoImpl(int porta) throws RemoteException {
        super(porta);
    }

    @Override
    public synchronized String ligarDesligar() {
        ligada = !ligada;
        return registrar("ligar/desligar", ligada ? "TV ligada no canal " + canal + "." : "TV desligada.");
    }

    @Override
    public synchronized String aumentarVolume() {
        if (!ligada) {
            return registrar("volume +", TV_DESLIGADA);
        }

        mudo = false;
        volume = Math.min(VOLUME_MAXIMO, volume + PASSO_VOLUME);
        return registrar("volume +", "Volume: " + volume + ".");
    }

    @Override
    public synchronized String diminuirVolume() {
        if (!ligada) {
            return registrar("volume -", TV_DESLIGADA);
        }

        mudo = false;
        volume = Math.max(0, volume - PASSO_VOLUME);
        return registrar("volume -", "Volume: " + volume + ".");
    }

    @Override
    public synchronized String proximoCanal() {
        if (!ligada) {
            return registrar("canal +", TV_DESLIGADA);
        }

        canal = canal == ULTIMO_CANAL ? PRIMEIRO_CANAL : canal + 1;
        return registrar("canal +", "Canal: " + canal + ".");
    }

    @Override
    public synchronized String canalAnterior() {
        if (!ligada) {
            return registrar("canal -", TV_DESLIGADA);
        }

        canal = canal == PRIMEIRO_CANAL ? ULTIMO_CANAL : canal - 1;
        return registrar("canal -", "Canal: " + canal + ".");
    }

    @Override
    public synchronized String irParaCanal(int numero) {
        String comando = "ir para o canal " + numero;

        if (!ligada) {
            return registrar(comando, TV_DESLIGADA);
        }

        if (numero < PRIMEIRO_CANAL || numero > ULTIMO_CANAL) {
            return registrar(comando, "Canal inválido: use de " + PRIMEIRO_CANAL + " a " + ULTIMO_CANAL + ".");
        }

        canal = numero;
        return registrar(comando, "Canal: " + canal + ".");
    }

    @Override
    public synchronized String alternarMudo() {
        if (!ligada) {
            return registrar("mudo", TV_DESLIGADA);
        }

        mudo = !mudo;
        return registrar("mudo", mudo ? "Mudo ativado." : "Som restaurado no volume " + volume + ".");
    }

    @Override
    public synchronized String status() {
        return registrar("status", descreverEstado());
    }

    private String descreverEstado() {
        if (!ligada) {
            return "TV desligada.";
        }

        return String.format("TV ligada | canal %d | volume %d | mudo: %s", canal, volume, mudo ? "sim" : "não");
    }

    private String registrar(String comando, String resposta) {
        System.out.printf("[%s] %s pediu \"%s\" -> %s%n", LocalTime.now().format(HORA), origem(), comando, resposta);
        System.out.println("           Estado da TV: " + descreverEstado());
        return resposta;
    }

    private static String origem() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "local";
        }
    }
}
