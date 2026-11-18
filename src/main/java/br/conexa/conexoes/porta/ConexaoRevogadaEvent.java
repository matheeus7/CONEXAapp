package br.conexa.conexoes.porta;

/** Publicado ao revogar uma Conexão; Tarefas devolve ao dono o que era da pessoa revogada (RF10). */
public record ConexaoRevogadaEvent(Long areaId, Long pessoaId, Long donoId) {
}
