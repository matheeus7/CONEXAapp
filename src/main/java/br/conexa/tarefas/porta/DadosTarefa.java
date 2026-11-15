package br.conexa.tarefas.porta;

import java.time.LocalDateTime;

/** Campos que a pessoa informa ao criar ou editar uma Tarefa. */
public record DadosTarefa(String titulo, String descricao, LocalDateTime prazo, Prioridade prioridade) {
}
