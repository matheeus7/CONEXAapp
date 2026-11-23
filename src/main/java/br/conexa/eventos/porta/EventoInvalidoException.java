package br.conexa.eventos.porta;

/** Título vazio ou longo demais, início ausente ou duração fora do intervalo permitido. */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
