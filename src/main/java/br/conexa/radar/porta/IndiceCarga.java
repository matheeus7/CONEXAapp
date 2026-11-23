package br.conexa.radar.porta;

import java.time.LocalDateTime;
import java.util.List;

/** Resultado do Radar: inteiro de 0 a 100, com a soma de pontos e os itens que o explicam. */
public record IndiceCarga(int valor, int somaPontos, int fatorEscala, List<ItemCarga> itens,
                          LocalDateTime calculadoEm) {
}
