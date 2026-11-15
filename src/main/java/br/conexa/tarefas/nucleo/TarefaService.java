package br.conexa.tarefas.nucleo;

import br.conexa.areas.porta.AreaExcluidaEvent;
import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.porta.AcessoNegadoException;
import br.conexa.conexoes.porta.PoliticaDeAcesso;
import br.conexa.tarefas.porta.DadosTarefa;
import br.conexa.tarefas.porta.TarefaInvalidaException;
import br.conexa.tarefas.porta.TarefaNaoEncontradaException;
import br.conexa.tarefas.porta.TarefaPort;
import br.conexa.tarefas.porta.TarefaResumo;
import br.conexa.tarefas.porta.StatusTarefa;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarefaService implements TarefaPort {

    static final int TAMANHO_MAXIMO_TITULO = 120;
    static final int TAMANHO_MAXIMO_DESCRICAO = 1000;

    private final TarefaRepositorio tarefas;
    private final AreaPort areas;
    private final PoliticaDeAcesso politica;
    private final Clock relogio;

    public TarefaService(TarefaRepositorio tarefas, AreaPort areas, PoliticaDeAcesso politica, Clock relogio) {
        this.tarefas = tarefas;
        this.areas = areas;
        this.politica = politica;
        this.relogio = relogio;
    }

    /** Observer: ao excluir uma Área, remove as Tarefas dela, na mesma transação (CT-015). */
    @EventListener
    @Transactional
    public void aoExcluirArea(AreaExcluidaEvent evento) {
        tarefas.deletarDaArea(evento.areaId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarefaResumo> listarDaArea(Long pessoaId, Long areaId) {
        AreaResumo area = areas.buscar(pessoaId, areaId);
        politica.exigirVer(pessoaId, area);
        LocalDateTime agora = LocalDateTime.now(relogio);
        return tarefas.findByAreaIdOrderByPrazoAsc(areaId).stream().map(t -> resumo(t, agora)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TarefaResumo buscar(Long pessoaId, Long tarefaId) {
        Tarefa tarefa = carregar(tarefaId);
        politica.exigirVer(pessoaId, areas.buscar(pessoaId, tarefa.getAreaId()));
        return resumo(tarefa, LocalDateTime.now(relogio));
    }

    @Override
    @Transactional
    public TarefaResumo criar(Long pessoaId, Long areaId, DadosTarefa dados) {
        AreaResumo area = areas.buscar(pessoaId, areaId);
        exigirEscrita(pessoaId, area);
        DadosTarefa limpos = validar(dados);
        // RF07: sem Conexões, o responsável é o criador (que só pode ser o dono).
        Long responsavel = politica.podeSerResponsavel(pessoaId, area) ? pessoaId : area.donoId();
        LocalDateTime agora = LocalDateTime.now(relogio);
        return resumo(tarefas.save(new Tarefa(areaId, limpos, pessoaId, responsavel, agora)), agora);
    }

    @Override
    @Transactional
    public TarefaResumo editar(Long pessoaId, Long tarefaId, DadosTarefa dados) {
        Tarefa tarefa = carregar(tarefaId);
        exigirEscrita(pessoaId, areas.buscar(pessoaId, tarefa.getAreaId()));
        LocalDateTime agora = LocalDateTime.now(relogio);
        tarefa.editar(validar(dados), pessoaId, agora);
        return resumo(tarefa, agora);
    }

    @Override
    @Transactional
    public TarefaResumo concluir(Long pessoaId, Long tarefaId) {
        Tarefa tarefa = carregar(tarefaId);
        exigirEscrita(pessoaId, areas.buscar(pessoaId, tarefa.getAreaId()));
        LocalDateTime agora = LocalDateTime.now(relogio);
        tarefa.concluir(pessoaId, agora);
        return resumo(tarefa, agora);
    }

    @Override
    @Transactional
    public void excluir(Long pessoaId, Long tarefaId) {
        Tarefa tarefa = carregar(tarefaId);
        exigirEscrita(pessoaId, areas.buscar(pessoaId, tarefa.getAreaId()));
        tarefas.delete(tarefa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarefaResumo> listarPendentesDoResponsavel(Long pessoaId, LocalDateTime ate) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        return tarefas.findByResponsavelIdAndStatusAndPrazoLessThanEqualOrderByPrazoAsc(
                pessoaId, StatusTarefa.PENDENTE, ate).stream().map(t -> resumo(t, agora)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarefaResumo> listarAtrasadas(Long pessoaId) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        return tarefas.findByResponsavelIdAndStatusAndPrazoBeforeOrderByPrazoAsc(
                pessoaId, StatusTarefa.PENDENTE, agora).stream().map(t -> resumo(t, agora)).toList();
    }

    private void exigirEscrita(Long pessoaId, AreaResumo area) {
        if (!politica.podeEscrever(pessoaId, area)) {
            throw new AcessoNegadoException("Sem permissão de escrita na Área " + area.id());
        }
    }

    private Tarefa carregar(Long tarefaId) {
        return tarefas.findById(tarefaId).orElseThrow(() -> new TarefaNaoEncontradaException(tarefaId));
    }

    private DadosTarefa validar(DadosTarefa dados) {
        String titulo = dados.titulo() == null ? "" : dados.titulo().trim();
        if (titulo.isEmpty()) {
            throw new TarefaInvalidaException("Informe o título da tarefa.");
        }
        if (titulo.length() > TAMANHO_MAXIMO_TITULO) {
            throw new TarefaInvalidaException("O título deve ter no máximo " + TAMANHO_MAXIMO_TITULO + " caracteres.");
        }
        if (dados.prazo() == null) {
            throw new TarefaInvalidaException("Informe o prazo da tarefa.");
        }
        if (dados.prioridade() == null) {
            throw new TarefaInvalidaException("Informe a prioridade da tarefa.");
        }
        String descricao = dados.descricao() == null ? null : dados.descricao().trim();
        if (descricao != null && descricao.isEmpty()) {
            descricao = null;
        }
        if (descricao != null && descricao.length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new TarefaInvalidaException("A descrição deve ter no máximo " + TAMANHO_MAXIMO_DESCRICAO + " caracteres.");
        }
        return new DadosTarefa(titulo, descricao, dados.prazo(), dados.prioridade());
    }

    private TarefaResumo resumo(Tarefa t, LocalDateTime agora) {
        return new TarefaResumo(t.getId(), t.getAreaId(), t.getTitulo(), t.getDescricao(), t.getPrazo(),
                t.getPrioridade(), t.getStatus(), t.getResponsavelId(), t.estaAtrasada(agora));
    }
}
