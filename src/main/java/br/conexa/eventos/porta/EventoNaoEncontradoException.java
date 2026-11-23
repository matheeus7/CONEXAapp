package br.conexa.eventos.porta;

public class EventoNaoEncontradoException extends RuntimeException {

    public EventoNaoEncontradoException(Long id) {
        super("Evento não encontrado: " + id);
    }
}
