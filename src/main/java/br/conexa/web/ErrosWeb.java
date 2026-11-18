package br.conexa.web;

import br.conexa.areas.porta.AreaNaoEncontradaException;
import br.conexa.conexoes.porta.AcessoNegadoException;
import br.conexa.conexoes.porta.ConexaoNaoEncontradaException;
import br.conexa.eventos.porta.EventoNaoEncontradoException;
import br.conexa.tarefas.porta.TarefaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Traduz erros de negócio das portas em páginas 403 e 404. */
@ControllerAdvice
public class ErrosWeb {

    @ExceptionHandler(AcessoNegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String acessoNegado() {
        return "error/403";
    }

    @ExceptionHandler({AreaNaoEncontradaException.class, TarefaNaoEncontradaException.class,
            EventoNaoEncontradoException.class, ConexaoNaoEncontradaException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrada() {
        return "error/404";
    }
}
