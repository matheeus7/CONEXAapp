package br.conexa.radar.porta;

import br.conexa.eventos.porta.EventoResumo;
import br.conexa.tarefas.porta.TarefaResumo;
import java.util.List;

/**
 * Porta pública do Radar Individual (RF11). O Radar consome Tarefas e Eventos pelas portas deles, não
 * os possui, e calcula tudo na consulta, sem guardar resultado.
 */
public interface RadarPort {

    IndiceCarga calcularIndice(Long pessoaId);

    /** Tarefas pendentes da pessoa com prazo vencido. */
    List<TarefaResumo> listarAtrasadas(Long pessoaId);

    /** Eventos em conflito de horário nos próximos 7 dias. */
    List<EventoResumo> detectarConflitos(Long pessoaId);
}
