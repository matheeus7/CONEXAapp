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

    /** RF10: as tarefas de quem perdeu o acesso voltam ao dono. */
    @Modifying
    @Query("update Tarefa t set t.responsavelId = :donoId where t.areaId = :areaId and t.responsavelId = :pessoaId")
    void devolverAoDono(@Param("areaId") Long areaId, @Param("pessoaId") Long pessoaId, @Param("donoId") Long donoId);

    @Modifying
    @Query("delete from Tarefa t where t.areaId = :areaId")
    void deletarDaArea(@Param("areaId") Long areaId);
}
