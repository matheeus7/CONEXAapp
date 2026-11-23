package br.conexa.eventos.nucleo;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepositorio extends JpaRepository<Evento, Long> {

    List<Evento> findByAreaIdInOrderByInicioAsc(Collection<Long> areaIds);

    @Modifying
    @Query("delete from Evento e where e.areaId = :areaId")
    void deletarDaArea(@Param("areaId") Long areaId);
}
