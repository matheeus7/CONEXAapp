package br.conexa.areas.nucleo;

import br.conexa.areas.porta.AreaExcluidaEvent;
import br.conexa.areas.porta.AreaNaoEncontradaException;
import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.areas.porta.NomeDeAreaInvalidoException;
import br.conexa.conexoes.porta.PoliticaDeAcesso;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AreaService implements AreaPort {

    static final int TAMANHO_MAXIMO_NOME = 80;

    private final AreaRepositorio areas;
    private final MinhaVidaRepositorio minhasVidas;
    private final PoliticaDeAcesso politica;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public AreaService(AreaRepositorio areas, MinhaVidaRepositorio minhasVidas, PoliticaDeAcesso politica,
                       ApplicationEventPublisher eventos, Clock relogio) {
        this.areas = areas;
        this.minhasVidas = minhasVidas;
        this.politica = politica;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaResumo> listar(Long pessoaId) {
        return minhasVidas.findByPessoaId(pessoaId)
                .map(vida -> areas.findByMinhaVidaIdOrderByNomeAsc(vida.getId()).stream()
                        .map(a -> resumo(a, vida.getPessoaId())).toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaResumo> listarCompartilhadas(Long pessoaId) {
        List<Long> ids = politica.areasCompartilhadasCom(pessoaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return areas.findAllById(ids).stream()
                .map(a -> resumo(a, donoDe(a)))
                .sorted(Comparator.comparing(AreaResumo::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaResumo> listarAcessiveis(Long pessoaId) {
        List<AreaResumo> todas = new ArrayList<>(listar(pessoaId));
        todas.addAll(listarCompartilhadas(pessoaId));
        return todas;
    }

    @Override
    @Transactional(readOnly = true)
    public AreaResumo buscar(Long pessoaId, Long areaId) {
        Area area = carregar(areaId);
        AreaResumo resumo = resumo(area, donoDe(area));
        politica.exigirVer(pessoaId, resumo);
        return resumo;
    }

    @Override
    @Transactional
    public AreaResumo criar(Long pessoaId, String nome) {
        MinhaVida vida = minhasVidas.findByPessoaId(pessoaId)
                .orElseThrow(() -> new IllegalStateException("Pessoa sem Minha Vida: " + pessoaId));
        String limpo = validar(vida.getId(), nome);
        return resumo(areas.save(new Area(vida.getId(), limpo, LocalDateTime.now(relogio))), pessoaId);
    }

    @Override
    @Transactional
    public AreaResumo renomear(Long pessoaId, Long areaId, String novoNome) {
        Area area = carregar(areaId);
        Long donoId = donoDe(area);
        politica.exigirDono(pessoaId, resumo(area, donoId));
        String limpo = novoNome == null ? "" : novoNome.trim();
        if (!limpo.equalsIgnoreCase(area.getNome())) {
            limpo = validar(area.getMinhaVidaId(), limpo);
        } else {
            limpo = validarFormato(limpo);
        }
        area.renomear(limpo);
        return resumo(area, donoId);
    }

    @Override
    @Transactional
    public void excluir(Long pessoaId, Long areaId) {
        Area area = carregar(areaId);
        politica.exigirDono(pessoaId, resumo(area, donoDe(area)));
        // Os assinantes removem os itens da Área primeiro; só então a Área sai (chaves estrangeiras).
        eventos.publishEvent(new AreaExcluidaEvent(areaId));
        areas.delete(area);
    }

    private Area carregar(Long areaId) {
        return areas.findById(areaId).orElseThrow(() -> new AreaNaoEncontradaException(areaId));
    }

    private Long donoDe(Area area) {
        return minhasVidas.findById(area.getMinhaVidaId()).orElseThrow().getPessoaId();
    }

    private String validar(Long minhaVidaId, String nome) {
        String limpo = validarFormato(nome);
        if (areas.existsByMinhaVidaIdAndNomeIgnoreCase(minhaVidaId, limpo)) {
            throw new NomeDeAreaInvalidoException("Você já tem uma Área com esse nome.");
        }
        return limpo;
    }

    private String validarFormato(String nome) {
        String limpo = nome == null ? "" : nome.trim();
        if (limpo.isEmpty()) {
            throw new NomeDeAreaInvalidoException("Informe o nome da Área.");
        }
        if (limpo.length() > TAMANHO_MAXIMO_NOME) {
            throw new NomeDeAreaInvalidoException("O nome deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        return limpo;
    }

    private AreaResumo resumo(Area area, Long donoId) {
        return new AreaResumo(area.getId(), area.getNome(), donoId, area.getCriadaEm());
    }
}
