package br.conexa.web;

import br.conexa.eventos.porta.EventoResumo;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Monta a grade de um mês (semanas de domingo a sábado) com os eventos de cada dia. */
public final class CalendarioMes {

    public record Dia(LocalDate data, boolean doMes, boolean hoje, List<EventoResumo> eventos) {
    }

    private CalendarioMes() {
    }

    public static List<List<Dia>> semanas(YearMonth mes, LocalDate hoje, List<EventoResumo> eventos) {
        LocalDate primeiro = mes.atDay(1);
        LocalDate dia = primeiro.minusDays(primeiro.getDayOfWeek().getValue() % 7);
        LocalDate ultimo = mes.atEndOfMonth();
        List<List<Dia>> semanas = new ArrayList<>();
        while (!dia.isAfter(ultimo)) {
            List<Dia> semana = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                semana.add(new Dia(dia, YearMonth.from(dia).equals(mes), dia.equals(hoje), eventosDoDia(eventos, dia)));
                dia = dia.plusDays(1);
            }
            semanas.add(semana);
        }
        return semanas;
    }

    public static List<EventoResumo> eventosDoDia(List<EventoResumo> eventos, LocalDate dia) {
        return eventos.stream()
                .filter(e -> e.inicio().isBefore(dia.plusDays(1).atStartOfDay()) && e.fim().isAfter(dia.atStartOfDay()))
                .toList();
    }
}
