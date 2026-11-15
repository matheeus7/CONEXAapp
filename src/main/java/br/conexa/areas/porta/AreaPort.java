package br.conexa.areas.porta;

import java.util.List;

/** Porta pública das Áreas (RF04). Toda operação recebe a pessoa que age e consulta a política de acesso. */
public interface AreaPort {

    List<AreaResumo> listar(Long pessoaId);

    AreaResumo buscar(Long pessoaId, Long areaId);

    AreaResumo criar(Long pessoaId, String nome);

    AreaResumo renomear(Long pessoaId, Long areaId, String novoNome);

    void excluir(Long pessoaId, Long areaId);
}
