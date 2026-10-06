// EnumMap é um Map otimizado para chaves que são enum: internamente ele é um
// array indexado pelo ordinal, então busca e inserção são imediatas e sem hash.
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

// enum em vez de constantes int ou String: o compilador passa a garantir que só
// existem esses cinco valores, o switch fica exaustivo e - importante para esta
// atividade - todo enum é Serializable, então o Gesto viaja pela rede no RMI
// sem eu escrever uma linha de conversão.
public enum Gesto {
    // Cada constante guarda duas formas do nome: a de exibição e a com artigo,
    // usada para montar frases como "Spock vaporiza a pedra".
    PEDRA("Pedra", "a pedra"),
    PAPEL("Papel", "o papel"),
    TESOURA("Tesoura", "a tesoura"),
    LAGARTO("Lagarto", "o lagarto"),
    SPOCK("Spock", "o Spock");

    // Tabela de quem vence quem, e com qual verbo.
    // Estrutura: vencedor -> (perdedor -> verbo). Guardar o verbo junto da regra
    // permite ao servidor explicar a vitória com a frase exata do enunciado,
    // em vez de só dizer quem ganhou.
    private static final Map<Gesto, Map<Gesto, String>> VITORIAS = new EnumMap<>(Gesto.class);

    // Bloco estático: roda uma vez, quando a classe é carregada.
    // Ele não pode estar no construtor porque, quando o construtor de uma
    // constante roda, as constantes seguintes ainda não existem - o Java proíbe
    // referenciá-las ali. Aqui todas já estão criadas.
    static {
        // As 10 regras do enunciado, uma por linha, na mesma ordem do original.
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

    // Construtor de enum é sempre privado: as únicas instâncias são as cinco
    // constantes declaradas acima.
    Gesto(String nome, String comArtigo) {
        this.nome = nome;
        this.comArtigo = comArtigo;
    }

    // computeIfAbsent cria o mapa interno na primeira regra de cada vencedor e
    // reaproveita nas seguintes, evitando um if de verificação a cada chamada.
    private static void regra(Gesto vencedor, String verbo, Gesto perdedor) {
        VITORIAS.computeIfAbsent(vencedor, gesto -> new EnumMap<>(Gesto.class)).put(perdedor, verbo);
    }

    // A consulta fica O(1) e, por construção, o gesto nunca vence a si mesmo:
    // nenhuma das 10 regras tem o mesmo gesto dos dois lados.
    public boolean vence(Gesto outro) {
        return VITORIAS.get(this).containsKey(outro);
    }

    // Monta a frase da regra: nome do vencedor + verbo + perdedor com artigo.
    public String explicarVitoria(Gesto perdedor) {
        return nome + " " + VITORIAS.get(this).get(perdedor) + " " + perdedor.comArtigo;
    }

    // Converte o que o jogador digitou em um Gesto, aceitando tanto o número do
    // menu quanto o nome. Devolve null para entrada inválida, porque digitar
    // errado é esperado, não é situação excepcional.
    public static Gesto de(String texto) {
        // Locale.ROOT evita o comportamento do idioma turco no toLowerCase.
        String valor = texto.trim().toLowerCase(Locale.ROOT);

        for (Gesto gesto : values()) {
            // ordinal() + 1 transforma a posição no enum (0 a 4) no número do
            // menu (1 a 5), sem precisar de uma tabela separada.
            if (valor.equals(String.valueOf(gesto.ordinal() + 1)) || valor.equals(gesto.nome.toLowerCase(Locale.ROOT))) {
                return gesto;
            }
        }

        return null;
    }

    // Sobrescrito para que qualquer concatenação mostre "Spock" em vez do
    // SPOCK padrão do enum - vale nas mensagens enviadas aos clientes.
    @Override
    public String toString() {
        return nome;
    }
}
