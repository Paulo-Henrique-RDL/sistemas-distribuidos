import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JogoImpl extends UnicastRemoteObject implements Jogo {
    static final int MINIMO_JOGADORES = 2;
    static final int MAXIMO_JOGADORES = 5;
    static final int SEGUNDOS_ENTRE_RODADAS = 10;
    static final int SEGUNDOS_PARA_JOGAR = 5;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<String, Jogador> jogadores = new LinkedHashMap<>();
    private final Map<String, Integer> pontos = new LinkedHashMap<>();
    private final Map<String, Gesto> jogadas = new LinkedHashMap<>();
    private final Set<String> participantes = new LinkedHashSet<>();
    private final List<String> perdedoresDaRodada = new ArrayList<>();

    private boolean aceitandoJogadas;
    private int rodada;
    private int empates;
    private int anuladas;
    private String vencedorDaRodada;

    public JogoImpl(int porta) throws RemoteException {
        super(porta);
    }

    @Override
    public void entrar(String nome, Jogador jogador) throws EntradaRecusadaException {
        String limpo = nome == null ? "" : nome.trim();
        Map<String, Jogador> jaConectados;

        synchronized (this) {
            if (limpo.isEmpty()) {
                throw new EntradaRecusadaException("Informe um nome para jogar.");
            }
            if (jogadores.containsKey(limpo)) {
                throw new EntradaRecusadaException("O nome \"" + limpo + "\" já está em uso.");
            }
            if (jogadores.size() >= MAXIMO_JOGADORES) {
                throw new EntradaRecusadaException("A sala está cheia: máximo de " + MAXIMO_JOGADORES + " jogadores.");
            }

            jaConectados = new LinkedHashMap<>(jogadores);
            jogadores.put(limpo, jogador);
            pontos.put(limpo, 0);
        }

        log(limpo + " entrou, vindo de " + origem() + ". Jogadores: " + total() + "/" + MAXIMO_JOGADORES);

        String boasVindas = String.format("Bem-vindo, %s! Jogadores na sala: %s.%n"
            + "As rodadas começam sozinhas com pelo menos %d jogadores. A cada rodada você tem %d s para jogar.",
            limpo, String.join(", ", copiaJogadores().keySet()), MINIMO_JOGADORES, SEGUNDOS_PARA_JOGAR);

        paraTodos(Map.of(limpo, jogador), (n, j) -> j.aviso(boasVindas));
        paraTodos(jaConectados, (n, j) -> j.aviso(limpo + " entrou no jogo."));
    }

    @Override
    public synchronized String jogar(String nome, Gesto gesto) {
        if (!jogadores.containsKey(nome)) {
            return "Você não está no jogo.";
        }
        if (gesto == null) {
            return "Jogada inválida.";
        }
        if (!aceitandoJogadas) {
            return "Fora do prazo: aguarde a próxima rodada.";
        }
        if (!participantes.contains(nome)) {
            return "Você entrou durante a rodada " + rodada + " e joga a partir da próxima.";
        }
        if (jogadas.containsKey(nome)) {
            return "Você já jogou " + jogadas.get(nome) + " nesta rodada.";
        }

        jogadas.put(nome, gesto);
        log("Rodada " + rodada + ": " + nome + " jogou.");
        return "Jogada registrada: " + gesto + ".";
    }

    @Override
    public void enviarAosPerdedores(String nome, String mensagem) {
        Map<String, Jogador> destinos = new LinkedHashMap<>();

        synchronized (this) {
            if (vencedorDaRodada == null || !vencedorDaRodada.equals(nome)) {
                return;
            }

            for (String perdedor : perdedoresDaRodada) {
                Jogador jogador = jogadores.get(perdedor);

                if (jogador != null) {
                    destinos.put(perdedor, jogador);
                }
            }

            vencedorDaRodada = null;
        }

        log(nome + " enviou aos perdedores (" + String.join(", ", destinos.keySet()) + "): " + mensagem);
        paraTodos(destinos, (n, j) -> j.mensagemDoVencedor(nome, mensagem));
    }

    @Override
    public void sair(String nome) {
        if (removerJogador(nome)) {
            log(nome + " saiu do jogo. Jogadores: " + total() + "/" + MAXIMO_JOGADORES);
            paraTodos(copiaJogadores(), (n, j) -> j.aviso(nome + " saiu do jogo."));
        }
    }

    public void executar() {
        while (true) {
            try {
                aguardarJogadores();

                paraTodos(copiaJogadores(), (n, j) -> j.aviso("Próxima rodada em " + SEGUNDOS_ENTRE_RODADAS + " s."));
                Thread.sleep(SEGUNDOS_ENTRE_RODADAS * 1000L);

                if (total() < MINIMO_JOGADORES) {
                    continue;
                }

                abrirRodada();
                Thread.sleep(SEGUNDOS_PARA_JOGAR * 1000L);
                encerrarRodada();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void aguardarJogadores() throws InterruptedException {
        int ultimoTotal = -1;

        while (total() < MINIMO_JOGADORES) {
            int atual = total();

            if (atual != ultimoTotal) {
                log("Aguardando jogadores (" + atual + "/" + MINIMO_JOGADORES + ")...");
                ultimoTotal = atual;
            }

            Thread.sleep(1000);
        }
    }

    private void abrirRodada() {
        Map<String, Jogador> destinos;
        int numero;

        synchronized (this) {
            numero = ++rodada;
            jogadas.clear();
            participantes.clear();
            participantes.addAll(jogadores.keySet());
            aceitandoJogadas = true;
            destinos = new LinkedHashMap<>(jogadores);
        }

        log("Rodada " + numero + " aberta para " + String.join(", ", destinos.keySet())
            + ". Prazo: " + SEGUNDOS_PARA_JOGAR + " s.");
        paraTodos(destinos, (n, j) -> j.rodadaAberta(numero, SEGUNDOS_PARA_JOGAR));
    }

    private void encerrarRodada() {
        Map<String, Gesto> validas;
        List<String> desclassificados = new ArrayList<>();
        int numero;

        synchronized (this) {
            aceitandoJogadas = false;
            numero = rodada;
            validas = new LinkedHashMap<>(jogadas);

            for (String nome : participantes) {
                if (!jogadas.containsKey(nome) && jogadores.containsKey(nome)) {
                    desclassificados.add(nome);
                }
            }
        }

        Resultado resultado = avaliar(validas);
        Map<String, Jogador> destinos;
        Map<String, Integer> placar;
        int totalEmpates;
        int totalAnuladas;

        synchronized (this) {
            perdedoresDaRodada.clear();
            vencedorDaRodada = null;

            if (resultado.vencedor != null && pontos.containsKey(resultado.vencedor)) {
                pontos.merge(resultado.vencedor, 1, Integer::sum);
                vencedorDaRodada = resultado.vencedor;

                for (String nome : participantes) {
                    if (!nome.equals(resultado.vencedor)) {
                        perdedoresDaRodada.add(nome);
                    }
                }
            } else if (validas.isEmpty()) {
                anuladas++;
            } else {
                empates++;
            }

            destinos = new LinkedHashMap<>(jogadores);
            placar = new LinkedHashMap<>(pontos);
            totalEmpates = empates;
            totalAnuladas = anuladas;
        }

        String relatorio = formatarResultado(numero, validas, desclassificados, resultado);
        System.out.println(relatorio);
        System.out.println(formatarPlacar(numero, placar, totalEmpates, totalAnuladas, null));

        for (String nome : desclassificados) {
            Jogador jogador = destinos.get(nome);

            if (jogador != null) {
                paraTodos(Map.of(nome, jogador), (n, j) -> j.aviso("Você não jogou em " + SEGUNDOS_PARA_JOGAR
                    + " s e foi desclassificado da rodada " + numero + "."));
            }
        }

        paraTodos(destinos, (n, j) -> j.aviso(relatorio));
        paraTodos(destinos, (n, j) -> j.aviso(formatarPlacar(numero, placar, totalEmpates, totalAnuladas, n)));

        Jogador vencedor = resultado.vencedor == null ? null : destinos.get(resultado.vencedor);

        if (vencedor != null) {
            Gesto gesto = validas.get(resultado.vencedor);
            paraTodos(Map.of(resultado.vencedor, vencedor), (n, j) -> j.vitoria(numero, gesto));
        }
    }

    static Resultado avaliar(Map<String, Gesto> jogadas) {
        Map<String, Integer> derrotados = new LinkedHashMap<>();

        for (Map.Entry<String, Gesto> jogador : jogadas.entrySet()) {
            int total = 0;

            for (Gesto outro : jogadas.values()) {
                if (jogador.getValue().vence(outro)) {
                    total++;
                }
            }

            derrotados.put(jogador.getKey(), total);
        }

        if (jogadas.size() == 1) {
            return new Resultado(derrotados, jogadas.keySet().iterator().next(), true);
        }

        int maximo = derrotados.values().stream().max(Integer::compare).orElse(0);
        List<String> lideres = new ArrayList<>();

        for (Map.Entry<String, Integer> jogador : derrotados.entrySet()) {
            if (jogador.getValue() == maximo) {
                lideres.add(jogador.getKey());
            }
        }

        String vencedor = maximo > 0 && lideres.size() == 1 ? lideres.get(0) : null;
        return new Resultado(derrotados, vencedor, false);
    }

    private static String formatarResultado(int rodada, Map<String, Gesto> validas,
                                            List<String> desclassificados, Resultado resultado) {
        StringBuilder texto = new StringBuilder();
        texto.append(String.format("%n=== Resultado da rodada %d ===%n", rodada));

        for (Map.Entry<String, Gesto> jogada : validas.entrySet()) {
            texto.append(String.format("%-14s %-8s derrotou %d%n",
                jogada.getKey(), jogada.getValue(), resultado.derrotados.get(jogada.getKey())));
        }

        for (String nome : desclassificados) {
            texto.append(String.format("%-14s %-8s desclassificado: não jogou em %d s%n",
                nome, "-", SEGUNDOS_PARA_JOGAR));
        }

        if (validas.isEmpty()) {
            texto.append("Rodada anulada: ninguém jogou dentro do prazo.");
        } else if (resultado.porWO) {
            texto.append("Vencedor por W.O.: ").append(resultado.vencedor)
                .append(" com ").append(validas.get(resultado.vencedor)).append(", o único a jogar no prazo.");
        } else if (resultado.vencedor != null) {
            Gesto gestoVencedor = validas.get(resultado.vencedor);
            texto.append("Vencedor: ").append(resultado.vencedor).append(" com ").append(gestoVencedor);

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

    private static String formatarPlacar(int rodada, Map<String, Integer> placar, int empates, int anuladas,
                                         String destinatario) {
        List<Map.Entry<String, Integer>> ordenado = new ArrayList<>(placar.entrySet());
        ordenado.sort((a, b) -> b.getValue() - a.getValue());

        StringBuilder texto = new StringBuilder();
        texto.append(String.format("--- Placar após a rodada %d ---%n", rodada));

        for (Map.Entry<String, Integer> jogador : ordenado) {
            String nome = jogador.getKey().equals(destinatario) ? jogador.getKey() + " (você)" : jogador.getKey();
            texto.append(String.format("  %-20s %d ponto(s)%n", nome, jogador.getValue()));
        }

        texto.append(String.format("  Empates: %d | Rodadas anuladas: %d", empates, anuladas));
        return texto.toString();
    }

    private interface Chamada {
        void executar(String nome, Jogador jogador) throws RemoteException;
    }

    private void paraTodos(Map<String, Jogador> destinos, Chamada chamada) {
        List<String> desconectados = new ArrayList<>();

        for (Map.Entry<String, Jogador> destino : destinos.entrySet()) {
            try {
                chamada.executar(destino.getKey(), destino.getValue());
            } catch (RemoteException e) {
                desconectados.add(destino.getKey());
            }
        }

        for (String nome : desconectados) {
            if (removerJogador(nome)) {
                log(nome + " perdeu a conexão e foi removido. Jogadores: " + total() + "/" + MAXIMO_JOGADORES);
                paraTodos(copiaJogadores(), (n, j) -> j.aviso(nome + " perdeu a conexão e saiu do jogo."));
            }
        }
    }

    private synchronized boolean removerJogador(String nome) {
        if (nome == null || jogadores.remove(nome) == null) {
            return false;
        }

        pontos.remove(nome);
        jogadas.remove(nome);
        participantes.remove(nome);
        return true;
    }

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

    static class Resultado {
        final Map<String, Integer> derrotados;
        final String vencedor;
        final boolean porWO;

        Resultado(Map<String, Integer> derrotados, String vencedor, boolean porWO) {
            this.derrotados = derrotados;
            this.vencedor = vencedor;
            this.porWO = porWO;
        }
    }
}
