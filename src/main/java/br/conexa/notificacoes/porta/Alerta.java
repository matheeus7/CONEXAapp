package br.conexa.notificacoes.porta;

import br.conexa.tarefas.porta.TarefaResumo;
import java.time.LocalDateTime;

/** Alerta calculado na consulta; nunca é guardado (RF12). */
public record Alerta(TarefaResumo tarefa, TipoAlerta tipo, LocalDateTime prazo) {
}
