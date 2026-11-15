package br.conexa.areas.nucleo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepositorio extends JpaRepository<Area, Long> {

    List<Area> findByMinhaVidaIdOrderByNomeAsc(Long minhaVidaId);

    boolean existsByMinhaVidaIdAndNomeIgnoreCase(Long minhaVidaId, String nome);
}
