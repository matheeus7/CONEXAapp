package br.conexa.radar.porta;

import java.time.LocalDateTime;

/** Um item que entra no Índice de Carga, com os pontos de prioridade e de urgência (RF11). */
public record ItemCarga(String tipo, String titulo, LocalDateTime quando, int pontosPrioridade, int pontosUrgencia) {

    public int total() {
        return pontosPrioridade + pontosUrgencia;
    }
}
