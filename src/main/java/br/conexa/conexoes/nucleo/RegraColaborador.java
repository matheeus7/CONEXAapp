package br.conexa.conexoes.nucleo;

public class RegraColaborador implements RegraDeAcesso {

    @Override
    public boolean permite(Operacao operacao) {
        return true; // ver, escrever e ser responsável; gerir a Área e as Conexões continua só do dono
    }
}
