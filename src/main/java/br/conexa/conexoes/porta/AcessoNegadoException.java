package br.conexa.conexoes.porta;

/** A pessoa não tem permissão para a operação pedida (CT-017). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
