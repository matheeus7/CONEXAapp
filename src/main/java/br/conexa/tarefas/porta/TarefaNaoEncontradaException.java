package br.conexa.tarefas.porta;

public class TarefaNaoEncontradaException extends RuntimeException {

    public TarefaNaoEncontradaException(Long id) {
        super("Tarefa não encontrada: " + id);
    }
}
