package br.conexa.tarefas.porta;

/** Título vazio ou longo demais, prazo ausente ou prioridade ausente. */
public class TarefaInvalidaException extends RuntimeException {

    public TarefaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
