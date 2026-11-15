package br.conexa.tarefas.porta;

import java.time.LocalDateTime;

public record TarefaResumo(Long id, Long areaId, String titulo, String descricao, LocalDateTime prazo,
                           Prioridade prioridade, StatusTarefa status, Long responsavelId, boolean atrasada) {

    public boolean concluida() {
        return status == StatusTarefa.CONCLUIDA;
    }
}
