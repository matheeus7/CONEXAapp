package br.conexa.tarefas.porta;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta pública das Tarefas (RF05, RF07). As duas últimas consultas são as que o Radar e os alertas
 * usam (diagrama de classes).
 */
public interface TarefaPort {

    List<TarefaResumo> listarDaArea(Long pessoaId, Long areaId);

    TarefaResumo buscar(Long pessoaId, Long tarefaId);

    TarefaResumo criar(Long pessoaId, Long areaId, DadosTarefa dados);

    TarefaResumo editar(Long pessoaId, Long tarefaId, DadosTarefa dados);

    TarefaResumo concluir(Long pessoaId, Long tarefaId);

    void excluir(Long pessoaId, Long tarefaId);

    List<TarefaResumo> listarPendentesDoResponsavel(Long pessoaId, LocalDateTime ate);

    List<TarefaResumo> listarAtrasadas(Long pessoaId);
}
