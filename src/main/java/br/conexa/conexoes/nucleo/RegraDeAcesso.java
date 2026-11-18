package br.conexa.conexoes.nucleo;

/** Strategy: cada nível de permissão é uma regra; novos níveis entram como novas implementações. */
public interface RegraDeAcesso {

    boolean permite(Operacao operacao);
}
