package br.conexa.conexoes.porta;

public class ConexaoNaoEncontradaException extends RuntimeException {

    public ConexaoNaoEncontradaException(Long id) {
        super("Conexão não encontrada: " + id);
    }
}
