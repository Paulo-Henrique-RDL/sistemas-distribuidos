import java.rmi.RemoteException;
// RemoteServer.getClientHost() informa o endereço de quem está chamando agora.
// É o RMI expondo um dado que, no socket, eu teria que extrair do pacote.
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
// UnicastRemoteObject é a classe que torna um objeto comum acessível de fora.
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Esta classe é a TV: ela guarda o estado e executa os comandos.
// Estender UnicastRemoteObject é o que faz o objeto ser EXPORTADO - o RMI passa
// a escutar numa porta por chamadas a ele e a despachá-las para estes métodos.
public class ControleRemotoImpl extends UnicastRemoteObject implements ControleRemoto {
    private static final int VOLUME_MAXIMO = 100;
    private static final int PASSO_VOLUME = 5;
    private static final int PRIMEIRO_CANAL = 1;
    private static final int ULTIMO_CANAL = 99;
    private static final String TV_DESLIGADA = "A TV está desligada. Ligue-a primeiro.";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    // O ESTADO fica no servidor, não no controle. É essa escolha que faz vários
    // controles verem a mesma TV: o cliente não guarda nada.
    private boolean ligada;
    private boolean mudo;
    private int volume = 20;
    private int canal = PRIMEIRO_CANAL;

    // O construtor precisa declarar RemoteException porque a exportação do objeto
    // acontece aqui, dentro do super(), e pode falhar.
    // Passar a porta (em vez de super() sem argumento) faz o objeto ser exportado
    // numa porta conhecida, a mesma do registry - assim só uma porta precisa ser
    // liberada no firewall quando as máquinas são diferentes.
    public ControleRemotoImpl(int porta) throws RemoteException {
        super(porta);
    }

    // Todos os métodos são synchronized por um motivo concreto: o RMI atende cada
    // chamada numa thread própria, então dois controles podem chegar ao mesmo
    // tempo. Sem a trava, duas chamadas de aumentarVolume poderiam ler o mesmo
    // valor e gravar o mesmo resultado, perdendo um dos incrementos.
    @Override
    public synchronized String ligarDesligar() {
        // Inverte o estado: um único botão para as duas ações, como num controle real.
        ligada = !ligada;
        return registrar("ligar/desligar", ligada ? "TV ligada no canal " + canal + "." : "TV desligada.");
    }

    @Override
    public synchronized String aumentarVolume() {
        // Regra de negócio no servidor: com a TV desligada, o comando é recusado.
        // O cliente não precisa saber dessa regra - ele só mostra a resposta.
        if (!ligada) {
            return registrar("volume +", TV_DESLIGADA);
        }

        // Mexer no volume cancela o mudo, que é o comportamento esperado de uma TV.
        mudo = false;
        // Math.min aplica o teto sem precisar de if.
        volume = Math.min(VOLUME_MAXIMO, volume + PASSO_VOLUME);
        return registrar("volume +", "Volume: " + volume + ".");
    }

    @Override
    public synchronized String diminuirVolume() {
        if (!ligada) {
            return registrar("volume -", TV_DESLIGADA);
        }

        mudo = false;
        // Math.max aplica o piso: o volume nunca fica negativo.
        volume = Math.max(0, volume - PASSO_VOLUME);
        return registrar("volume -", "Volume: " + volume + ".");
    }

    @Override
    public synchronized String proximoCanal() {
        if (!ligada) {
            return registrar("canal +", TV_DESLIGADA);
        }

        // Circular: passou do último, volta ao primeiro.
        canal = canal == ULTIMO_CANAL ? PRIMEIRO_CANAL : canal + 1;
        return registrar("canal +", "Canal: " + canal + ".");
    }

    @Override
    public synchronized String canalAnterior() {
        if (!ligada) {
            return registrar("canal -", TV_DESLIGADA);
        }

        // Mesma lógica circular no sentido contrário.
        canal = canal == PRIMEIRO_CANAL ? ULTIMO_CANAL : canal - 1;
        return registrar("canal -", "Canal: " + canal + ".");
    }

    @Override
    public synchronized String irParaCanal(int numero) {
        // Monta o rótulo com o número pedido para o log do servidor ficar completo.
        String comando = "ir para o canal " + numero;

        if (!ligada) {
            return registrar(comando, TV_DESLIGADA);
        }

        // Validação do parâmetro no servidor: nunca confiar no que o cliente envia,
        // mesmo que o menu dele já filtre.
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

        // O volume não é zerado: ele continua guardado, e voltar do mudo o restaura.
        mudo = !mudo;
        return registrar("mudo", mudo ? "Mudo ativado." : "Som restaurado no volume " + volume + ".");
    }

    @Override
    public synchronized String status() {
        // Também synchronized: sem a trava, poderia ler o estado no meio de uma
        // alteração feita por outro cliente e devolver uma foto inconsistente.
        return registrar("status", descreverEstado());
    }

    private String descreverEstado() {
        if (!ligada) {
            return "TV desligada.";
        }

        return String.format("TV ligada | canal %d | volume %d | mudo: %s", canal, volume, mudo ? "sim" : "não");
    }

    // Centraliza duas coisas em um lugar só: registrar o comando no console do
    // servidor e devolver a resposta ao cliente. Assim nenhum método precisa
    // repetir o System.out, e a demonstração do vídeo mostra tudo o que chega.
    private String registrar(String comando, String resposta) {
        System.out.printf("[%s] %s pediu \"%s\" -> %s%n", LocalTime.now().format(HORA), origem(), comando, resposta);
        System.out.println("           Estado da TV: " + descreverEstado());
        return resposta;
    }

    private static String origem() {
        try {
            // Só funciona dentro de uma chamada remota em andamento: o RMI sabe
            // qual cliente está sendo atendido naquela thread.
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            // Acontece se o método for chamado localmente, sem passar pela rede.
            return "local";
        }
    }
}
