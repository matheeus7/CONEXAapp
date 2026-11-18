package br.conexa.areas.porta;

/** Nome vazio, longo demais ou já usado por outra Área da mesma pessoa. */
public class NomeDeAreaInvalidoException extends RuntimeException {

    public NomeDeAreaInvalidoException(String mensagem) {
        super(mensagem);
    }
}
