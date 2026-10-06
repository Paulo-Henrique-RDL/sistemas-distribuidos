import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// O servidor do jogo. Acumula três papéis: objeto remoto que os clientes
// chamam, dono do estado da partida e relógio que conduz as rodadas.
public class JogoImpl extends UnicastRemoteObject implements Jogo {
    // Os quatro números do enunciado, num lugar só.
    static final int MINIMO_JOGADORES = 2;
    static final int MAXIMO_JOGADORES = 5;
    static final int SEGUNDOS_ENTRE_RODADAS = 10;
    static final int SEGUNDOS_PARA_JOGAR = 5;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Nome -> objeto remoto do cliente. É este mapa que permite ao servidor
    // chamar de volta. LinkedHashMap preserva a ordem de entrada, então as
    // listagens saem na ordem em que as pessoas chegaram.
    private final Map<String, Jogador> jogadores = new LinkedHashMap<>();
    // Placar acumulado: uma vitória vale um ponto.
    private final Map<String, Integer> pontos = new LinkedHashMap<>();
    // Jogadas da rodada corrente; esvaziado a cada abertura.
    private final Map<String, Gesto> jogadas = new LinkedHashMap<>();
    // Fotografia de quem estava na sala quando a rodada abriu. Sem ela, quem
    // entrasse no meio da rodada seria cobrado por uma jogada que não podia fazer.
    private final Set<String> participantes = new LinkedHashSet<>();
    // Quem perdeu a última rodada - o alvo da mensagem do vencedor.
    private final List<String> perdedoresDaRodada = new ArrayList<>();

    // Janela de 5 s aberta/fechada: é o que o jogar() consulta para aceitar ou recusar.
    private boolean aceitandoJogadas;
    private int rodada;
    private int empates;
    private int anuladas;
    // Guarda quem venceu a última rodada, para só essa pessoa poder mandar a
    // mensagem aos perdedores. Volta a null depois do envio, limitando a uma
    // mensagem por vitória.
    private String vencedorDaRodada;

    public JogoImpl(int porta) throws RemoteException {
        // Exporta na mesma porta do registry: uma só porta no firewall.
        super(porta);
    }

    @Override
    public void entrar(String nome, Jogador jogador) throws EntradaRecusadaException {
        // Trata null e espaços antes de qualquer validação.
        String limpo = nome == null ? "" : nome.trim();
        Map<String, Jogador> jaConectados;

        // Bloco sincronizado CURTO: só valida e grava. Os avisos ficam para fora
        // da trava - essa separação é a decisão de concorrência mais importante
        // da classe, e o motivo está explicado em paraTodos().
        synchronized (this) {
            if (limpo.isEmpty()) {
                throw new EntradaRecusadaException("Informe um nome para jogar.");
            }
            // O nome é a chave de tudo (jogadas, placar, callbacks), então
            // duplicata é proibida.
            if (jogadores.containsKey(limpo)) {
                throw new EntradaRecusadaException("O nome \"" + limpo + "\" já está em uso.");
            }
            // Limite do enunciado: no máximo 5.
            if (jogadores.size() >= MAXIMO_JOGADORES) {
                throw new EntradaRecusadaException("A sala está cheia: máximo de " + MAXIMO_JOGADORES + " jogadores.");
            }

            // Cópia de quem já estava, feita ANTES da inserção, para o recém-chegado
            // não receber o aviso da própria entrada.
            jaConectados = new LinkedHashMap<>(jogadores);
            jogadores.put(limpo, jogador);
            pontos.put(limpo, 0);
        }

        log(limpo + " entrou, vindo de " + origem() + ". Jogadores: " + total() + "/" + MAXIMO_JOGADORES);

        String boasVindas = String.format("Bem-vindo, %s! Jogadores na sala: %s.%n"
            + "As rodadas começam sozinhas com pelo menos %d jogadores. A cada rodada você tem %d s para jogar.",
            limpo, String.join(", ", copiaJogadores().keySet()), MINIMO_JOGADORES, SEGUNDOS_PARA_JOGAR);

        // Map.of cria um mapa de um elemento só: reaproveita o paraTodos para
        // falar com uma pessoa, em vez de duplicar o tratamento de erro.
        paraTodos(Map.of(limpo, jogador), (n, j) -> j.aviso(boasVindas));
        paraTodos(jaConectados, (n, j) -> j.aviso(limpo + " entrou no jogo."));
    }

    // Método inteiro sincronizado: só mexe no estado, não chama cliente nenhum,
    // então segurar a trava aqui é seguro e simples.
    @Override
    public synchronized String jogar(String nome, Gesto gesto) {
        // Cada recusa devolve o motivo em texto; o cliente apenas imprime.
        if (!jogadores.containsKey(nome)) {
            return "Você não está no jogo.";
        }
        if (gesto == null) {
            return "Jogada inválida.";
        }
        // Prazo de 5 s: a janela foi fechada pelo encerrarRodada.
        if (!aceitandoJogadas) {
            return "Fora do prazo: aguarde a próxima rodada.";
        }
        // Entrou depois que a rodada abriu: joga só na próxima.
        if (!participantes.contains(nome)) {
            return "Você entrou durante a rodada " + rodada + " e joga a partir da próxima.";
        }
        // Uma jogada por rodada: a primeira vale, para ninguém trocar o gesto
        // depois de pensar melhor.
        if (jogadas.containsKey(nome)) {
            return "Você já jogou " + jogadas.get(nome) + " nesta rodada.";
        }

        jogadas.put(nome, gesto);
        // O log não revela o gesto: isso só aparece no resultado.
        log("Rodada " + rodada + ": " + nome + " jogou.");
        return "Jogada registrada: " + gesto + ".";
    }

    @Override
    public void enviarAosPerdedores(String nome, String mensagem) {
        Map<String, Jogador> destinos = new LinkedHashMap<>();

        synchronized (this) {
            // Só o vencedor da última rodada pode mandar - impede que um perdedor
            // use o método para provocar os outros.
            if (vencedorDaRodada == null || !vencedorDaRodada.equals(nome)) {
                return;
            }

            for (String perdedor : perdedoresDaRodada) {
                Jogador jogador = jogadores.get(perdedor);

                // Pode ser null se a pessoa saiu entre o fim da rodada e o envio.
                if (jogador != null) {
                    destinos.put(perdedor, jogador);
                }
            }

            // Zera o vencedor: uma mensagem por vitória, sem repetição.
            vencedorDaRodada = null;
        }

        log(nome + " enviou aos perdedores (" + String.join(", ", destinos.keySet()) + "): " + mensagem);
        // De novo, o envio acontece FORA do synchronized.
        paraTodos(destinos, (n, j) -> j.mensagemDoVencedor(nome, mensagem));
    }

    @Override
    public void sair(String nome) {
        // Só avisa os demais se a remoção realmente aconteceu.
        if (removerJogador(nome)) {
            log(nome + " saiu do jogo. Jogadores: " + total() + "/" + MAXIMO_JOGADORES);
            paraTodos(copiaJogadores(), (n, j) -> j.aviso(nome + " saiu do jogo."));
        }
    }

    // O relógio do jogo, rodando na thread principal do servidor.
    // É aqui que o enunciado "o servidor controla as jogadas" vira código.
    public void executar() {
        while (true) {
            try {
                // 1) Espera ter gente suficiente.
                aguardarJogadores();

                // 2) Intervalo de 10 s entre rodadas, anunciado a todos.
                paraTodos(copiaJogadores(), (n, j) -> j.aviso("Próxima rodada em " + SEGUNDOS_ENTRE_RODADAS + " s."));
                Thread.sleep(SEGUNDOS_ENTRE_RODADAS * 1000L);

                // 3) Confere de novo: alguém pode ter saído durante o intervalo.
                if (total() < MINIMO_JOGADORES) {
                    continue;
                }

                // 4) Abre a rodada, dá os 5 s do enunciado e encerra.
                abrirRodada();
                Thread.sleep(SEGUNDOS_PARA_JOGAR * 1000L);
                encerrarRodada();

            } catch (InterruptedException e) {
                // Restaura o sinal de interrupção e encerra o laço com elegância.
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void aguardarJogadores() throws InterruptedException {
        // Guarda o último total registrado para não repetir a mesma linha de log
        // a cada segundo enquanto nada muda.
        int ultimoTotal = -1;

        while (total() < MINIMO_JOGADORES) {
            int atual = total();

            if (atual != ultimoTotal) {
                log("Aguardando jogadores (" + atual + "/" + MINIMO_JOGADORES + ")...");
                ultimoTotal = atual;
            }

            // Verificação por amostragem a cada segundo: mais simples que
            // wait/notify e suficiente para a escala desta aplicação.
            Thread.sleep(1000);
        }
    }

    private void abrirRodada() {
        Map<String, Jogador> destinos;
        int numero;

        synchronized (this) {
            // Pré-incremento: numero recebe o valor já atualizado.
            numero = ++rodada;
            jogadas.clear();
            participantes.clear();
            // A fotografia dos participantes: quem entrar depois desta linha
            // não é cobrado nem desclassificado nesta rodada.
            participantes.addAll(jogadores.keySet());
            // Abre a janela de 5 s.
            aceitandoJogadas = true;
            destinos = new LinkedHashMap<>(jogadores);
        }

        log("Rodada " + numero + " aberta para " + String.join(", ", destinos.keySet())
            + ". Prazo: " + SEGUNDOS_PARA_JOGAR + " s.");
        // numero é efetivamente final, por isso pode ser usado dentro do lambda.
        paraTodos(destinos, (n, j) -> j.rodadaAberta(numero, SEGUNDOS_PARA_JOGAR));
    }

    private void encerrarRodada() {
        Map<String, Gesto> validas;
        List<String> desclassificados = new ArrayList<>();
        int numero;

        synchronized (this) {
            // Fecha a janela: jogadas atrasadas passam a ser recusadas.
            aceitandoJogadas = false;
            numero = rodada;
            validas = new LinkedHashMap<>(jogadas);

            // Participante que não deixou jogada e ainda está na sala: desclassificado.
            // A checagem de jogadores.containsKey evita cobrar quem saiu no meio.
            for (String nome : participantes) {
                if (!jogadas.containsKey(nome) && jogadores.containsKey(nome)) {
                    desclassificados.add(nome);
                }
            }
        }

        // A apuração é feita fora da trava, por ser função pura sobre a cópia.
        Resultado resultado = avaliar(validas);
        Map<String, Jogador> destinos;
        Map<String, Integer> placar;
        int totalEmpates;
        int totalAnuladas;

        synchronized (this) {
            perdedoresDaRodada.clear();
            vencedorDaRodada = null;

            // containsKey protege o caso de o vencedor ter saído entre a jogada
            // e a apuração.
            if (resultado.vencedor != null && pontos.containsKey(resultado.vencedor)) {
                // merge soma 1 ao valor existente - um ponto por vitória.
                pontos.merge(resultado.vencedor, 1, Integer::sum);
                vencedorDaRodada = resultado.vencedor;

                // Perdedores = todos os participantes menos o vencedor, incluindo
                // os desclassificados por tempo. É quem recebe a mensagem.
                for (String nome : participantes) {
                    if (!nome.equals(resultado.vencedor)) {
                        perdedoresDaRodada.add(nome);
                    }
                }
            } else if (validas.isEmpty()) {
                // Ninguém jogou: rodada anulada, não conta como empate.
                anuladas++;
            } else {
                // Houve jogadas, mas sem vencedor único: empate.
                empates++;
            }

            // Cópias do estado para montar as mensagens fora da trava.
            destinos = new LinkedHashMap<>(jogadores);
            placar = new LinkedHashMap<>(pontos);
            totalEmpates = empates;
            totalAnuladas = anuladas;
        }

        String relatorio = formatarResultado(numero, validas, desclassificados, resultado);
        // O servidor também exibe, como pede o enunciado.
        System.out.println(relatorio);
        System.out.println(formatarPlacar(numero, placar, totalEmpates, totalAnuladas, null));

        // Aviso individual para quem perdeu o prazo.
        for (String nome : desclassificados) {
            Jogador jogador = destinos.get(nome);

            if (jogador != null) {
                paraTodos(Map.of(nome, jogador), (n, j) -> j.aviso("Você não jogou em " + SEGUNDOS_PARA_JOGAR
                    + " s e foi desclassificado da rodada " + numero + "."));
            }
        }

        paraTodos(destinos, (n, j) -> j.aviso(relatorio));
        // O placar é PERSONALIZADO: o lambda recebe o nome do destinatário, e o
        // formatarPlacar marca a linha dele com "(você)". É o que o enunciado
        // pede ao falar em informar a cada cliente a sua pontuação e a dos demais.
        paraTodos(destinos, (n, j) -> j.aviso(formatarPlacar(numero, placar, totalEmpates, totalAnuladas, n)));

        Jogador vencedor = resultado.vencedor == null ? null : destinos.get(resultado.vencedor);

        // Por último: avisa o vencedor, que vai responder mandando a mensagem
        // aos perdedores. Deixar por último garante que os perdedores já viram
        // o resultado quando o "Perdeu Loser" chegar.
        if (vencedor != null) {
            Gesto gesto = validas.get(resultado.vencedor);
            paraTodos(Map.of(resultado.vencedor, vencedor), (n, j) -> j.vitoria(numero, gesto));
        }
    }

    // Função pura: mesma entrada, mesma saída, sem tocar no estado da classe.
    // Por isso é estática e pôde ser testada isoladamente.
    // A regra de desempate com 3 a 5 jogadores não está no enunciado; a adotada
    // é: vence quem derrotar MAIS adversários, desde que ninguém empate com ele.
    static Resultado avaliar(Map<String, Gesto> jogadas) {
        Map<String, Integer> derrotados = new LinkedHashMap<>();

        // Para cada jogador, conta quantos adversários o gesto dele vence.
        for (Map.Entry<String, Gesto> jogador : jogadas.entrySet()) {
            int total = 0;

            // Percorre TODOS os gestos, inclusive repetidos: dois adversários com
            // o mesmo gesto valem dois pontos de contagem. Comparar consigo mesmo
            // é inofensivo, porque nenhum gesto vence a si próprio.
            for (Gesto outro : jogadas.values()) {
                if (jogador.getValue().vence(outro)) {
                    total++;
                }
            }

            derrotados.put(jogador.getKey(), total);
        }

        // W.O.: só uma pessoa jogou no prazo, então ela vence sozinha.
        if (jogadas.size() == 1) {
            return new Resultado(derrotados, jogadas.keySet().iterator().next(), true);
        }

        // orElse(0) cobre o mapa vazio, que aqui não ocorre, mas evita Optional solto.
        int maximo = derrotados.values().stream().max(Integer::compare).orElse(0);
        List<String> lideres = new ArrayList<>();

        for (Map.Entry<String, Integer> jogador : derrotados.entrySet()) {
            if (jogador.getValue() == maximo) {
                lideres.add(jogador.getKey());
            }
        }

        // Duas condições para haver vencedor: a maior soma tem de ser maior que
        // zero (maximo == 0 significa que todos jogaram o mesmo gesto) e ser de
        // uma pessoa só. Qualquer outro caso é empate, e todos jogam de novo.
        String vencedor = maximo > 0 && lideres.size() == 1 ? lideres.get(0) : null;
        return new Resultado(derrotados, vencedor, false);
    }

    private static String formatarResultado(int rodada, Map<String, Gesto> validas,
                                            List<String> desclassificados, Resultado resultado) {
        StringBuilder texto = new StringBuilder();
        texto.append(String.format("%n=== Resultado da rodada %d ===%n", rodada));

        // Uma linha por jogada válida, com quantos adversários cada um derrotou.
        for (Map.Entry<String, Gesto> jogada : validas.entrySet()) {
            texto.append(String.format("%-14s %-8s derrotou %d%n",
                jogada.getKey(), jogada.getValue(), resultado.derrotados.get(jogada.getKey())));
        }

        // Depois, quem perdeu o prazo.
        for (String nome : desclassificados) {
            texto.append(String.format("%-14s %-8s desclassificado: não jogou em %d s%n",
                nome, "-", SEGUNDOS_PARA_JOGAR));
        }

        // Os quatro desfechos possíveis da rodada.
        if (validas.isEmpty()) {
            texto.append("Rodada anulada: ninguém jogou dentro do prazo.");
        } else if (resultado.porWO) {
            texto.append("Vencedor por W.O.: ").append(resultado.vencedor)
                .append(" com ").append(validas.get(resultado.vencedor)).append(", o único a jogar no prazo.");
        } else if (resultado.vencedor != null) {
            Gesto gestoVencedor = validas.get(resultado.vencedor);
            texto.append("Vencedor: ").append(resultado.vencedor).append(" com ").append(gestoVencedor);

            // Lista cada vitória com a frase da regra, usando o verbo guardado
            // no enum: "Spock vaporiza a pedra (bruno)".
            for (Map.Entry<String, Gesto> jogada : validas.entrySet()) {
                if (gestoVencedor.vence(jogada.getValue())) {
                    texto.append(String.format("%n  %s (%s)", gestoVencedor.explicarVitoria(jogada.getValue()),
                        jogada.getKey()));
                }
            }
        } else {
            texto.append("Empate! Ninguém derrotou mais adversários que os demais. Todos jogam de novo na próxima rodada.");
        }

        return texto.toString();
    }

    // destinatario é o nome de quem vai ler: a linha dele ganha "(você)".
    // Passar null gera a versão neutra, usada no console do servidor.
    private static String formatarPlacar(int rodada, Map<String, Integer> placar, int empates, int anuladas,
                                         String destinatario) {
        List<Map.Entry<String, Integer>> ordenado = new ArrayList<>(placar.entrySet());
        // Ordena do maior para o menor; a subtração funciona porque são pontos
        // pequenos, sem risco de estouro do int.
        ordenado.sort((a, b) -> b.getValue() - a.getValue());

        StringBuilder texto = new StringBuilder();
        texto.append(String.format("--- Placar após a rodada %d ---%n", rodada));

        for (Map.Entry<String, Integer> jogador : ordenado) {
            String nome = jogador.getKey().equals(destinatario) ? jogador.getKey() + " (você)" : jogador.getKey();
            texto.append(String.format("  %-20s %d ponto(s)%n", nome, jogador.getValue()));
        }

        // Empates acumulados, como o enunciado pede.
        texto.append(String.format("  Empates: %d | Rodadas anuladas: %d", empates, anuladas));
        return texto.toString();
    }

    // Interface funcional própria: as do Java (Consumer, BiConsumer) não aceitam
    // método que lance exceção checada, e toda chamada remota lança RemoteException.
    // Com ela, cada ponto do código descreve O QUE dizer e o paraTodos cuida de
    // COMO entregar e do que fazer quando falha.
    private interface Chamada {
        void executar(String nome, Jogador jogador) throws RemoteException;
    }

    // Único caminho pelo qual o servidor fala com os clientes.
    // Duas garantias importantes: ele é sempre chamado FORA de synchronized, e
    // ele trata a queda de cliente num lugar só.
    // Por que fora da trava: o cliente pode responder chamando o servidor de
    // volta (é o que o vencedor faz). Se a trava estivesse presa aqui, essa
    // chamada de volta ficaria esperando, e as duas pontas travariam.
    private void paraTodos(Map<String, Jogador> destinos, Chamada chamada) {
        List<String> desconectados = new ArrayList<>();

        for (Map.Entry<String, Jogador> destino : destinos.entrySet()) {
            try {
                chamada.executar(destino.getKey(), destino.getValue());
            } catch (RemoteException e) {
                // Cliente fechou a janela ou caiu da rede. Não dá para remover
                // agora: alterar o mapa durante a iteração quebraria o laço.
                desconectados.add(destino.getKey());
            }
        }

        // Remoção depois do laço, já com a lista fechada.
        for (String nome : desconectados) {
            if (removerJogador(nome)) {
                log(nome + " perdeu a conexão e foi removido. Jogadores: " + total() + "/" + MAXIMO_JOGADORES);
                // Chamada recursiva, mas finita: cada volta remove pelo menos um
                // jogador do mapa, então a recursão para.
                paraTodos(copiaJogadores(), (n, j) -> j.aviso(nome + " perdeu a conexão e saiu do jogo."));
            }
        }
    }

    // Devolve true só se o jogador existia: evita avisos duplicados quando a
    // saída e a queda acontecem quase juntas.
    private synchronized boolean removerJogador(String nome) {
        if (nome == null || jogadores.remove(nome) == null) {
            return false;
        }

        // Limpa todos os vestígios para o nome poder ser reutilizado.
        pontos.remove(nome);
        jogadas.remove(nome);
        participantes.remove(nome);
        return true;
    }

    // Cópia defensiva: quem itera trabalha sobre um retrato estável, enquanto o
    // mapa original pode mudar por outra thread.
    private synchronized Map<String, Jogador> copiaJogadores() {
        return new LinkedHashMap<>(jogadores);
    }

    private synchronized int total() {
        return jogadores.size();
    }

    private static void log(String texto) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + texto);
    }

    private static String origem() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "local";
        }
    }

    // Agrupa o desfecho da rodada num objeto só, para avaliar() poder devolver
    // três informações de uma vez. Campos final: o resultado não muda depois
    // de calculado.
    static class Resultado {
        // Quantos adversários cada jogador derrotou - vai para o relatório.
        final Map<String, Integer> derrotados;
        // null quando houve empate ou rodada anulada.
        final String vencedor;
        // Diferencia a vitória normal da vitória por ausência dos outros.
        final boolean porWO;

        Resultado(Map<String, Integer> derrotados, String vencedor, boolean porWO) {
            this.derrotados = derrotados;
            this.vencedor = vencedor;
            this.porWO = porWO;
        }
    }
}
