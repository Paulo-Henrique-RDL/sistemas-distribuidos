// Exceção própria para a recusa de entrada: nome vazio, nome já em uso ou
// sala cheia. Estende Exception (e não RuntimeException) para ser CHECADA -
// assim o compilador obriga o cliente a tratar o caso.
//
// Por que não usar RemoteException para isso: ela significa falha de rede.
// Misturar as duas faria o cliente mostrar "erro de conexão" quando, na
// verdade, o jogo recusou a entrada por uma regra. Separar deixa a mensagem
// certa chegar ao jogador.
//
// Para atravessar a rede, a exceção precisa ser serializável - e Exception já
// implementa Serializable. O RMI serializa o objeto no servidor, envia e o
// relança no cliente.
public class EntradaRecusadaException extends Exception {
    // Identifica a versão da classe na serialização: fixar o valor evita que
    // uma recompilação gere um id diferente e quebre a compatibilidade entre
    // as duas pontas, que podem ter sido compiladas em momentos distintos.
    private static final long serialVersionUID = 1L;

    // O motivo vai para a mensagem padrão de Exception, recuperável com
    // getMessage() no cliente.
    public EntradaRecusadaException(String motivo) {
        super(motivo);
    }
}
