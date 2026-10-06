import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public enum Gesto {
    PEDRA("Pedra", "a pedra"),
    PAPEL("Papel", "o papel"),
    TESOURA("Tesoura", "a tesoura"),
    LAGARTO("Lagarto", "o lagarto"),
    SPOCK("Spock", "o Spock");

    private static final Map<Gesto, Map<Gesto, String>> VITORIAS = new EnumMap<>(Gesto.class);

    static {
        regra(TESOURA, "corta", PAPEL);
        regra(PAPEL, "cobre", PEDRA);
        regra(PEDRA, "esmaga", LAGARTO);
        regra(LAGARTO, "envenena", SPOCK);
        regra(SPOCK, "quebra", TESOURA);
        regra(TESOURA, "decapita", LAGARTO);
        regra(LAGARTO, "come", PAPEL);
        regra(PAPEL, "refuta", SPOCK);
        regra(SPOCK, "vaporiza", PEDRA);
        regra(PEDRA, "amassa", TESOURA);
    }

    private final String nome;
    private final String comArtigo;

    Gesto(String nome, String comArtigo) {
        this.nome = nome;
        this.comArtigo = comArtigo;
    }

    private static void regra(Gesto vencedor, String verbo, Gesto perdedor) {
        VITORIAS.computeIfAbsent(vencedor, gesto -> new EnumMap<>(Gesto.class)).put(perdedor, verbo);
    }

    public boolean vence(Gesto outro) {
        return VITORIAS.get(this).containsKey(outro);
    }

    public String explicarVitoria(Gesto perdedor) {
        return nome + " " + VITORIAS.get(this).get(perdedor) + " " + perdedor.comArtigo;
    }

    public static Gesto de(String texto) {
        String valor = texto.trim().toLowerCase(Locale.ROOT);

        for (Gesto gesto : values()) {
            if (valor.equals(String.valueOf(gesto.ordinal() + 1)) || valor.equals(gesto.nome.toLowerCase(Locale.ROOT))) {
                return gesto;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return nome;
    }
}
