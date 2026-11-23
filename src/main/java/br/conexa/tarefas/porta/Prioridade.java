package br.conexa.tarefas.porta;

/** Prioridade da Tarefa. O peso no Índice de Carga (RF11) fica aqui, sem switch sobre enum. */
public enum Prioridade {
    BAIXA("Baixa", 1), MEDIA("Média", 2), ALTA("Alta", 3);

    private final String rotulo;
    private final int peso;

    Prioridade(String rotulo, int peso) {
        this.rotulo = rotulo;
        this.peso = peso;
    }

    public String getRotulo() { return rotulo; }

    public int getPeso() { return peso; }
}
