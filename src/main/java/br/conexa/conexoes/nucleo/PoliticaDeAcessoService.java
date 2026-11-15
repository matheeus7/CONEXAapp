package br.conexa.conexoes.nucleo;

import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.porta.PoliticaDeAcesso;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PoliticaDeAcessoService implements PoliticaDeAcesso {

    // Regra provisória da Sprint 4.1: só o dono age. Na Etapa 5, consultará as Conexões da Área.

    @Override
    public boolean podeVer(Long pessoaId, AreaResumo area) {
        return ehDono(pessoaId, area);
    }

    @Override
    public boolean podeEscrever(Long pessoaId, AreaResumo area) {
        return ehDono(pessoaId, area);
    }

    @Override
    public boolean ehDono(Long pessoaId, AreaResumo area) {
        return pessoaId != null && Objects.equals(pessoaId, area.donoId());
    }

    @Override
    public boolean podeSerResponsavel(Long pessoaId, AreaResumo area) {
        return ehDono(pessoaId, area);
    }
}
