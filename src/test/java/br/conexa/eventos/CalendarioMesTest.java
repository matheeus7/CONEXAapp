package br.conexa.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import br.conexa.eventos.porta.EventoResumo;
import br.conexa.web.CalendarioMes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CT-032: a grade do mês coloca cada evento no dia correto. */
class CalendarioMesTest {

    @Test
    @DisplayName("novembro de 2026 começa no domingo dia 1 e tem 5 semanas")
    void grade() {
        var semanas = CalendarioMes.semanas(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 15), List.of());

        assertThat(semanas).hasSize(5);
        assertThat(semanas.get(0).get(0).data()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(semanas.get(0).get(0).doMes()).isTrue();
        assertThat(semanas.stream().flatMap(List::stream).filter(d -> d.hoje()).count()).isEqualTo(1);
        assertThat(semanas.get(4).get(1).data()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    @DisplayName("o evento aparece no dia em que acontece e só nele")
    void eventoNoDiaCorreto() {
        EventoResumo prova = new EventoResumo(1L, 10L, "Faculdade", "Prova",
                LocalDateTime.of(2026, 11, 12, 14, 0), LocalDateTime.of(2026, 11, 12, 16, 0), false);

        var semanas = CalendarioMes.semanas(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 15), List.of(prova));
        var diasComEvento = semanas.stream().flatMap(List::stream).filter(d -> !d.eventos().isEmpty()).toList();

        assertThat(diasComEvento).hasSize(1);
        assertThat(diasComEvento.get(0).data()).isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    @DisplayName("evento que termina à meia-noite não invade o dia seguinte")
    void terminaNaMeiaNoite() {
        EventoResumo festa = new EventoResumo(2L, 10L, "Casa", "Festa",
                LocalDateTime.of(2026, 11, 12, 22, 0), LocalDateTime.of(2026, 11, 13, 0, 0), false);

        assertThat(CalendarioMes.eventosDoDia(List.of(festa), LocalDate.of(2026, 11, 12))).hasSize(1);
        assertThat(CalendarioMes.eventosDoDia(List.of(festa), LocalDate.of(2026, 11, 13))).isEmpty();
    }
}
