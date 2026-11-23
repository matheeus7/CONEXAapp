package br.conexa.conexoes.nucleo;

import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.porta.NivelPermissao;
import br.conexa.conexoes.porta.PoliticaDeAcesso;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Decide o acesso consultando o banco a cada chamada, sem cache: a revogação ou a troca de nível valem
 * já na próxima requisição (RF09, RF10). O dono usa {@link RegraDono}; os demais, a regra do seu nível.
 */
@Service
public class PoliticaDeAcessoService implements PoliticaDeAcesso {

    private final ConexaoRepositorio conexoes;
    private final RegraDeAcesso regraDoDono = new RegraDono();
    private final Map<NivelPermissao, RegraDeAcesso> regras = new EnumMap<>(NivelPermissao.class);

    public PoliticaDeAcessoService(ConexaoRepositorio conexoes) {
        this.conexoes = conexoes;
        regras.put(NivelPermissao.VISUALIZADOR, new RegraVisualizador());
        regras.put(NivelPermissao.COLABORADOR, new RegraColaborador());
    }

    @Override
    public boolean podeVer(Long pessoaId, AreaResumo area) {
        return permite(pessoaId, area, Operacao.VER);
    }

    @Override
    public boolean podeEscrever(Long pessoaId, AreaResumo area) {
        return permite(pessoaId, area, Operacao.ESCREVER);
    }

    @Override
    public boolean ehDono(Long pessoaId, AreaResumo area) {
        return pessoaId != null && Objects.equals(pessoaId, area.donoId());
    }

    @Override
    public boolean podeSerResponsavel(Long pessoaId, AreaResumo area) {
        return permite(pessoaId, area, Operacao.SER_RESPONSAVEL);
    }

    @Override
    public List<Long> areasCompartilhadasCom(Long pessoaId) {
        return conexoes.findByPessoaId(pessoaId).stream().map(Conexao::getAreaId).toList();
    }

    private boolean permite(Long pessoaId, AreaResumo area, Operacao operacao) {
        if (pessoaId == null) {
            return false;
        }
        if (ehDono(pessoaId, area)) {
            return regraDoDono.permite(operacao);
        }
        return conexoes.findByAreaIdAndPessoaId(area.id(), pessoaId)
                .map(c -> regras.get(c.getNivel()).permite(operacao))
                .orElse(false);
    }
}
