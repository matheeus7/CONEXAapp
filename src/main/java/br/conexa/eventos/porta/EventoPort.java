package br.conexa.eventos.porta;

import java.time.LocalDateTime;
import java.util.List;

/** Porta pública dos Eventos (RF06). {@link #listarDoPeriodo} também é usada pelo Radar (diagrama de classes). */
public interface EventoPort {

    /** Eventos da pessoa que cruzam o intervalo [de, ate), de todas as Áreas que ela enxerga. */
    List<EventoResumo> listarDoPeriodo(Long pessoaId, LocalDateTime de, LocalDateTime ate);

    List<EventoResumo> listarDaArea(Long pessoaId, Long areaId);

    EventoResumo buscar(Long pessoaId, Long eventoId);

    EventoResumo criar(Long pessoaId, Long areaId, DadosEvento dados);

    EventoResumo editar(Long pessoaId, Long eventoId, DadosEvento dados);

    void excluir(Long pessoaId, Long eventoId);
}
