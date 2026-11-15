package br.conexa.tarefas.nucleo;

import br.conexa.tarefas.porta.StatusTarefa;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaRepositorio extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findByAreaIdOrderByPrazoAsc(Long areaId);

    List<Tarefa> findByResponsavelIdAndStatusAndPrazoBeforeOrderByPrazoAsc(
            Long responsavelId, StatusTarefa status, LocalDateTime limite);

    List<Tarefa> findByResponsavelIdAndStatusAndPrazoLessThanEqualOrderByPrazoAsc(
            Long responsavelId, StatusTarefa status, LocalDateTime limite);

    @Modifying
    @Query("delete from Tarefa t where t.areaId = :areaId")
    void deletarDaArea(@Param("areaId") Long areaId);
}
