package br.conexa.autenticacao.porta;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() {
        super("Este e-mail já está em uso.");
    }
}
