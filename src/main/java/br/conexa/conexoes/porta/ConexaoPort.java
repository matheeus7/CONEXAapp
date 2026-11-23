package br.conexa.conexoes.porta;

import java.util.List;

/** Porta pública das Conexões (RF08 a RF10). Só o dono da Área gerencia conexões. */
public interface ConexaoPort {

    ConexaoResumo conectar(Long donoId, Long areaId, String email, NivelPermissao nivel);

    List<ConexaoResumo> listar(Long donoId, Long areaId);

    ConexaoResumo buscar(Long donoId, Long conexaoId);

    ConexaoResumo alterarNivel(Long donoId, Long conexaoId, NivelPermissao nivel);

    void revogar(Long donoId, Long conexaoId);

    /** Pessoas com nível Colaborador na Área (candidatas a responsável, RF07). */
    List<Long> colaboradoresDaArea(Long areaId);
}
