package br.conexa.areas.porta;

/** Publicado ao excluir uma Área; Tarefas, Eventos e Conexões reagem removendo o que era dela. */
public record AreaExcluidaEvent(Long areaId) {
}
