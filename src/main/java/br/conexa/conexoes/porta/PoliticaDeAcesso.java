package br.conexa.conexoes.porta;

import br.conexa.areas.porta.AreaResumo;

/**
 * Única regra de acesso do sistema, exposta pelo componente Conexões (diagrama de classes).
 * Todo componente que protege uma Área pergunta aqui antes de agir. Nesta sprint só vale a regra
 * do dono; os níveis Visualizador e Colaborador entram na Etapa 5.
 */
public interface PoliticaDeAcesso {

    boolean podeVer(Long pessoaId, AreaResumo area);

    boolean podeEscrever(Long pessoaId, AreaResumo area);

    boolean ehDono(Long pessoaId, AreaResumo area);

    boolean podeSerResponsavel(Long pessoaId, AreaResumo area);

    default void exigirVer(Long pessoaId, AreaResumo area) {
        if (!podeVer(pessoaId, area)) {
            throw new AcessoNegadoException("Sem acesso à Área " + area.id());
        }
    }

    /** Renomear e excluir a Área são ações só do dono, mesmo para Colaborador (CT-017). */
    default void exigirDono(Long pessoaId, AreaResumo area) {
        if (!ehDono(pessoaId, area)) {
            throw new AcessoNegadoException("Só o dono pode alterar a Área " + area.id());
        }
    }
}
