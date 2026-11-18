package br.conexa.conexoes.nucleo;

public class RegraDono implements RegraDeAcesso {

    @Override
    public boolean permite(Operacao operacao) {
        return true;
    }
}
