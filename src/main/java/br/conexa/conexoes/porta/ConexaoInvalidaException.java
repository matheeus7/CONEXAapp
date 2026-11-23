package br.conexa.conexoes.porta;

/** E-mail sem conta, auto-conexão, conexão duplicada ou dados ausentes. */
public class ConexaoInvalidaException extends RuntimeException {

    public ConexaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
