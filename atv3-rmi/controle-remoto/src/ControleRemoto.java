// Remote é uma interface vazia, de marcação: ela não declara método nenhum,
// serve só para o RMI reconhecer que esta interface pode ser chamada de outra
// máquina. Sem estender Remote, nada aqui seria exportável.
import java.rmi.Remote;
// RemoteException representa as falhas que só existem quando a chamada
// atravessa a rede: servidor fora do ar, conexão perdida, timeout.
import java.rmi.RemoteException;

// Esta interface é o CONTRATO entre as duas máquinas, e é a grande diferença
// para as ATVs 1 e 2: lá eu inventava um formato de texto e interpretava a
// string do outro lado. Aqui o acordo é uma assinatura de método, verificada
// pelo compilador nos dois lados.
public interface ControleRemoto extends Remote {
    // O nome sob o qual o objeto é publicado no registry. Fica na interface
    // porque servidor e cliente precisam usar exatamente a mesma string: um
    // publica com ele, o outro procura por ele. Campos de interface já são
    // public static final por padrão.
    String NOME_NO_REGISTRO = "ControleRemoto";

    // Todo método remoto PRECISA declarar RemoteException. É uma exigência do
    // RMI, e é honesta: quem chama tem de lidar com a possibilidade de a
    // chamada não chegar ao destino - algo que nunca acontece em chamada local.
    // Cada método devolve String com a resposta já pronta para exibir, de modo
    // que a decisão sobre o que aconteceu fique no servidor (a TV), não no controle.

    // Função 1: liga se estiver desligada, desliga se estiver ligada.
    String ligarDesligar() throws RemoteException;

    // Função 2 e 3: volume em passos fixos, com limites tratados no servidor.
    String aumentarVolume() throws RemoteException;

    String diminuirVolume() throws RemoteException;

    // Funções 4 e 5: navegação sequencial de canais, com volta ao início no fim.
    String proximoCanal() throws RemoteException;

    String canalAnterior() throws RemoteException;

    // Função 6: o único método com parâmetro. O int é serializado, enviado pela
    // rede e entregue ao servidor - o RMI cuida disso sem código meu.
    String irParaCanal(int canal) throws RemoteException;

    // Função 7: alterna o mudo sem perder o volume anterior.
    String alternarMudo() throws RemoteException;

    // Função 8: consulta pura, não altera nada. Útil para o cliente que acabou
    // de conectar descobrir em que estado a TV está.
    String status() throws RemoteException;
}
