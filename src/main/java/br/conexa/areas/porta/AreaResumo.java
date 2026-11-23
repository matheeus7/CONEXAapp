package br.conexa.areas.porta;

import java.time.LocalDateTime;

public record AreaResumo(Long id, String nome, Long donoId, LocalDateTime criadaEm) {
}
