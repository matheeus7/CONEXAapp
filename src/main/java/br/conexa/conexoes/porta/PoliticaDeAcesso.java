package br.conexa.conexoes.porta;

import br.conexa.areas.porta.AreaResumo;
import java.util.List;

/**
 * Única regra de acesso do sistema, exposta pelo componente Conexões (diagrama de classes).
 * Todo componente que protege uma Área pergunta aqui antes de agir, a cada requisição (RNF01, RNF03).
 */
public interface PoliticaDeAcesso {

    boolean podeVer(Long pessoaId, AreaResumo area);

    boolean podeEscrever(Long pessoaId, AreaResumo area);

    boolean ehDono(Long pessoaId, AreaResumo area);

    boolean podeSerResponsavel(Long pessoaId, AreaResumo area);

    /** Ids das Áreas de outras pessoas às quais esta pessoa está conectada ("Compartilhadas comigo"). */
    List<Long> areasCompartilhadasCom(Long pessoaId);

    default void exigirVer(Long pessoaId, AreaResumo area) {
        if (!podeVer(pessoaId, area)) {
            throw new AcessoNegadoException("Sem acesso à Área " + area.id());
        }
    }

    /** Renomear, excluir a Área e gerenciar Conexões são ações só do dono, mesmo para Colaborador. */
    default void exigirDono(Long pessoaId, AreaResumo area) {
        if (!ehDono(pessoaId, area)) {
            throw new AcessoNegadoException("Só o dono pode alterar a Área " + area.id());
        }
    }
}
