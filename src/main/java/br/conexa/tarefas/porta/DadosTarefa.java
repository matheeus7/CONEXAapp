package br.conexa.tarefas.porta;

import java.time.LocalDateTime;

/** Campos que a pessoa informa ao criar ou editar uma Tarefa. */
public record DadosTarefa(String titulo, String descricao, LocalDateTime prazo, Prioridade prioridade,
                          Long responsavelId) {

    /** Sem responsável explícito: na criação vale o criador; na edição, o responsável não muda. */
    public DadosTarefa(String titulo, String descricao, LocalDateTime prazo, Prioridade prioridade) {
        this(titulo, descricao, prazo, prioridade, null);
    }
}
