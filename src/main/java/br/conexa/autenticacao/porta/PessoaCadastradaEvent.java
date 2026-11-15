package br.conexa.autenticacao.porta;

/** Evento interno (Observer): outros componentes reagem ao cadastro sem que Autenticação os conheça. */
public record PessoaCadastradaEvent(Long pessoaId) {
}
