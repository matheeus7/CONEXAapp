package br.conexa.eventos.porta;

import java.time.Duration;
import java.time.LocalDateTime;

/** Visão de um Evento. {@code emConflito} considera todos os eventos da pessoa, de qualquer Área (RF06). */
public record EventoResumo(Long id, Long areaId, String areaNome, String titulo,
                           LocalDateTime inicio, LocalDateTime fim, boolean emConflito) {

    public long duracaoMinutos() {
        return Duration.between(inicio, fim).toMinutes();
    }
}
