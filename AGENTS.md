# Como trabalhar neste repositório

Guia de operação do repositório. Vale para qualquer pessoa — ou agente — que mexer aqui.

## Regras fixas

- Autor dos commits: `DTBM27 <phrdlima1@gmail.com>`
- **Nenhum commit pode conter `Co-Authored-By`**, nem qualquer menção a Claude, Anthropic ou ferramenta de IA — nem na mensagem, nem no corpo, nem no histórico. Isso vale também para os repositórios publicados a partir daqui.
- Mensagens de commit em português, no imperativo, sem acento (`Adiciona`, `Corrige`, `Traz`).
- O `gh` não está no PATH. O caminho completo é `C:\Program Files\GitHub CLI\gh.exe`.

## Os dois tipos de repositório

Cada atividade existe em dois lugares, com papéis diferentes:

| | Monorepo | Repositório da entrega |
| --- | --- | --- |
| Qual é | `sistemas-distribuidos` | um por atividade, sufixo `-temporario` |
| Contém | **todas** as atividades | **só** aquela atividade, na raiz |
| Para quê | onde se trabalha | o link que vai para o professor |
| Histórico | completo | um commit só, sobrescrito a cada publicação |

Só se edita o monorepo. O repositório da entrega é sempre gerado a partir dele — nunca o contrário.

### Mapa atual

| Atividade | Pasta aqui | Repositório da entrega |
| --- | --- | --- |
| ATV1 — sockets TCP | `atv1-socket-tcp/` | `atv1-socket-tcp-temporario` |
| ATV2 — datagramas UDP | `atv2-socket-udp/` | `atv2-socket-udp-temporario` |
| ATV3 — RMI | `atv3-rmi/` | `atv3-rmi-temporario` |
| Projeto Integrador | `projeto-integrador/` | `contas-pagar-receber` |
| ATV4 — Spring Cloud Config | `atv4-config-server/` | `atv4-config-server-temporario` |

`atv4-repo-configs` é um caso à parte: não é cópia de nada. É o repositório de configurações que o Config Server da ATV4 clona em tempo de execução, e por isso precisa existir sozinho.

## O fluxo

### 1. Trabalhar

Sempre no monorepo, nunca na cópia publicada.

```sh
git add -A
git commit -m "Descreve o que mudou"
git push
```

### 2. Publicar uma atividade para o professor

Troque `<pasta>` e `<repo>` pela linha correspondente no mapa acima.

```sh
rm -rf /tmp/pub && mkdir -p /tmp/pub
git archive HEAD:<pasta> | tar -x -C /tmp/pub

cd /tmp/pub
git init -b main
git add -A
git commit -m "<mensagem da entrega>"
git remote add origin https://github.com/Paulo-Henrique-RDL/<repo>.git
git push -u origin main --force
```

Por que `git archive HEAD:<pasta>`:

- exporta **só o que está versionado** — o que está no `.gitignore` (binários, `target/`, bancos locais, material de estudo) não vai junto;
- exporta a partir do **último commit**, não do disco — o que não foi commitado não é publicado por engano;
- a pasta vira a **raiz** do repositório publicado, que é como o professor espera receber.

O `--force` é intencional: a entrega é sempre um retrato do estado atual, com um commit só. Republicar depois de uma correção é rodar o mesmo bloco de novo.

### 3. Conferir antes de entregar

```sh
GH="/c/Program Files/GitHub CLI/gh.exe"

# nenhuma co-autoria no histórico publicado
"$GH" api repos/Paulo-Henrique-RDL/<repo>/commits --jq '.[].commit.message' \
  | grep -ci -E "co-authored|claude|anthropic"      # tem que dar 0

# contribuidor único
"$GH" api repos/Paulo-Henrique-RDL/<repo>/contributors --jq '.[].login'
```

### Criar um repositório de entrega novo

```sh
"/c/Program Files/GitHub CLI/gh.exe" repo create <repo> --public \
  --description "<descricao>"
```

## Ambiente

- **JDK 24** instalado; os projetos Java compilam para **21**.
- **`mvn` não está no PATH.** Os projetos Maven trazem o Maven Wrapper — use `mvnw` (Windows) ou `./mvnw`.
- A pasta fica dentro do OneDrive. Se um arquivo aparecer vazio ou der erro de "provedor de nuvem não está em execução", ele não foi baixado: abra a pasta no Explorer e marque "Sempre manter neste dispositivo" antes de continuar.
