package br.conexa.areas.porta;

import java.util.List;

/** Porta pública das Áreas (RF04). Toda operação recebe a pessoa que age e consulta a política de acesso. */
public interface AreaPort {

    /** Áreas que a pessoa possui. */
    List<AreaResumo> listar(Long pessoaId);

    AreaResumo buscar(Long pessoaId, Long areaId);

    /** Áreas de outras pessoas às quais ela está conectada ("Compartilhadas comigo"). */
    List<AreaResumo> listarCompartilhadas(Long pessoaId);

    /** Próprias e compartilhadas: tudo o que a pessoa enxerga. */
    List<AreaResumo> listarAcessiveis(Long pessoaId);

    AreaResumo criar(Long pessoaId, String nome);

    AreaResumo renomear(Long pessoaId, Long areaId, String novoNome);

    void excluir(Long pessoaId, Long areaId);
}
