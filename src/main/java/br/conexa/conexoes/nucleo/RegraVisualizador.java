package br.conexa.conexoes.nucleo;

public class RegraVisualizador implements RegraDeAcesso {

    @Override
    public boolean permite(Operacao operacao) {
        return operacao == Operacao.VER;
    }
}
