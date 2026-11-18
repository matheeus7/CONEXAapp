package br.conexa.eventos.porta;

import java.time.LocalDateTime;

/** Campos informados ao criar ou editar um Evento. O fim é o início mais a duração. */
public record DadosEvento(String titulo, LocalDateTime inicio, Integer duracaoMinutos) {
}
