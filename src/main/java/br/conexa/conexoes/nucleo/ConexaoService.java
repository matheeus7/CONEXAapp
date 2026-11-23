package br.conexa.conexoes.nucleo;

import br.conexa.areas.porta.AreaExcluidaEvent;
import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.conexoes.porta.ConexaoInvalidaException;
import br.conexa.conexoes.porta.ConexaoNaoEncontradaException;
import br.conexa.conexoes.porta.ConexaoPort;
import br.conexa.conexoes.porta.ConexaoResumo;
import br.conexa.conexoes.porta.ConexaoRevogadaEvent;
import br.conexa.conexoes.porta.NivelPermissao;
import br.conexa.conexoes.porta.PoliticaDeAcesso;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConexaoService implements ConexaoPort {

    private final ConexaoRepositorio conexoes;
    private final AreaPort areas;
    private final AutenticacaoPort autenticacao;
    private final PoliticaDeAcesso politica;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public ConexaoService(ConexaoRepositorio conexoes, AreaPort areas, AutenticacaoPort autenticacao,
                          PoliticaDeAcesso politica, ApplicationEventPublisher eventos, Clock relogio) {
        this.conexoes = conexoes;
        this.areas = areas;
        this.autenticacao = autenticacao;
        this.politica = politica;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /** Observer: ao excluir uma Área, as Conexões dela somem, na mesma transação. */
    @EventListener
    @Transactional
    public void aoExcluirArea(AreaExcluidaEvent evento) {
        conexoes.deletarDaArea(evento.areaId());
    }

    @Override
    @Transactional
    public ConexaoResumo conectar(Long donoId, Long areaId, String email, NivelPermissao nivel) {
        exigirDono(donoId, areaId);
        if (email == null || email.isBlank()) {
            throw new ConexaoInvalidaException("Informe o e-mail da pessoa.");
        }
        if (nivel == null) {
            throw new ConexaoInvalidaException("Escolha o nível de acesso.");
        }
        PessoaResumo pessoa = autenticacao.buscarPorEmail(email)
                .orElseThrow(() -> new ConexaoInvalidaException("Não encontramos uma conta com esse e-mail."));
        if (pessoa.id().equals(donoId)) {
            throw new ConexaoInvalidaException("Você já é o dono desta Área.");
        }
        if (conexoes.existsByAreaIdAndPessoaId(areaId, pessoa.id())) {
            throw new ConexaoInvalidaException("Essa pessoa já está conectada a esta Área.");
        }
        Conexao nova = conexoes.save(new Conexao(areaId, pessoa.id(), nivel, LocalDateTime.now(relogio)));
        return resumo(nova, pessoa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConexaoResumo> listar(Long donoId, Long areaId) {
        exigirDono(donoId, areaId);
        return conexoes.findByAreaIdOrderByCriadaEmAsc(areaId).stream().map(this::resumo).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConexaoResumo buscar(Long donoId, Long conexaoId) {
        Conexao conexao = carregar(conexaoId);
        exigirDono(donoId, conexao.getAreaId());
        return resumo(conexao);
    }

    @Override
    @Transactional
    public ConexaoResumo alterarNivel(Long donoId, Long conexaoId, NivelPermissao nivel) {
        Conexao conexao = carregar(conexaoId);
        exigirDono(donoId, conexao.getAreaId());
        if (nivel == null) {
            throw new ConexaoInvalidaException("Escolha o nível de acesso.");
        }
        conexao.alterarNivel(nivel);
        return resumo(conexao);
    }

    @Override
    @Transactional
    public void revogar(Long donoId, Long conexaoId) {
        Conexao conexao = carregar(conexaoId);
        exigirDono(donoId, conexao.getAreaId());
        eventos.publishEvent(new ConexaoRevogadaEvent(conexao.getAreaId(), conexao.getPessoaId(), donoId));
        conexoes.delete(conexao);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> colaboradoresDaArea(Long areaId) {
        return conexoes.findByAreaIdOrderByCriadaEmAsc(areaId).stream()
                .filter(c -> c.getNivel() == NivelPermissao.COLABORADOR)
                .map(Conexao::getPessoaId)
                .toList();
    }

    private AreaResumo exigirDono(Long donoId, Long areaId) {
        AreaResumo area = areas.buscar(donoId, areaId); // falha com 403/404 se a pessoa nem enxerga a Área
        politica.exigirDono(donoId, area);
        return area;
    }

    private Conexao carregar(Long conexaoId) {
        return conexoes.findById(conexaoId).orElseThrow(() -> new ConexaoNaoEncontradaException(conexaoId));
    }

    private ConexaoResumo resumo(Conexao c) {
        PessoaResumo pessoa = autenticacao.buscarPorId(c.getPessoaId()).orElseThrow();
        return resumo(c, pessoa);
    }

    private ConexaoResumo resumo(Conexao c, PessoaResumo pessoa) {
        return new ConexaoResumo(c.getId(), c.getAreaId(), pessoa.id(), pessoa.nome(), pessoa.email(), c.getNivel());
    }
}
