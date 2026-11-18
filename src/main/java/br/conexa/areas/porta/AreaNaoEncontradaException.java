package br.conexa.areas.porta;

public class AreaNaoEncontradaException extends RuntimeException {

    public AreaNaoEncontradaException(Long id) {
        super("Área não encontrada: " + id);
    }
}
