package br.conexa.conexoes.porta;

public record ConexaoResumo(Long id, Long areaId, Long pessoaId, String nome, String email, NivelPermissao nivel) {
}
