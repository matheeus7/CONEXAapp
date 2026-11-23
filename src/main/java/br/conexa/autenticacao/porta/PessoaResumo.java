package br.conexa.autenticacao.porta;

/** Visão pública de uma Pessoa. Nunca carrega a senha nem o hash. */
public record PessoaResumo(Long id, String nome, String email) {
}
