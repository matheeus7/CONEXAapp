package br.conexa.eventos.nucleo;

import br.conexa.areas.porta.AreaExcluidaEvent;
import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.porta.AcessoNegadoException;
import br.conexa.conexoes.porta.PoliticaDeAcesso;
import br.conexa.eventos.porta.DadosEvento;
import br.conexa.eventos.porta.EventoInvalidoException;
import br.conexa.eventos.porta.EventoNaoEncontradoException;
import br.conexa.eventos.porta.EventoPort;
import br.conexa.eventos.porta.EventoResumo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService implements EventoPort {

    static final int TAMANHO_MAXIMO_TITULO = 120;
    static final int DURACAO_MAXIMA_MINUTOS = 24 * 60;

    private final EventoRepositorio eventos;
    private final AreaPort areas;
    private final PoliticaDeAcesso politica;
    private final Clock relogio;

    public EventoService(EventoRepositorio eventos, AreaPort areas, PoliticaDeAcesso politica, Clock relogio) {
        this.eventos = eventos;
        this.areas = areas;
        this.politica = politica;
        this.relogio = relogio;
    }

    /** Observer: ao excluir uma Área, remove os Eventos dela, na mesma transação. */
    @EventListener
    @Transactional
    public void aoExcluirArea(AreaExcluidaEvent evento) {
        eventos.deletarDaArea(evento.areaId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResumo> listarDoPeriodo(Long pessoaId, LocalDateTime de, LocalDateTime ate) {
        return visaoDaPessoa(pessoaId).stream()
                .filter(e -> e.fim().isAfter(de) && e.inicio().isBefore(ate))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResumo> listarDaArea(Long pessoaId, Long areaId) {
        politica.exigirVer(pessoaId, areas.buscar(pessoaId, areaId));
        return visaoDaPessoa(pessoaId).stream().filter(e -> e.areaId().equals(areaId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventoResumo buscar(Long pessoaId, Long eventoId) {
        Evento evento = carregar(eventoId);
        politica.exigirVer(pessoaId, areas.buscar(pessoaId, evento.getAreaId()));
        return visao(pessoaId, eventoId);
    }

    @Override
    @Transactional
    public EventoResumo criar(Long pessoaId, Long areaId, DadosEvento dados) {
        if (areaId == null) {
            throw new EventoInvalidoException("Escolha a Área do evento.");
        }
        exigirEscrita(pessoaId, areas.buscar(pessoaId, areaId));
        Intervalo intervalo = validar(dados);
        Evento salvo = eventos.save(new Evento(areaId, intervalo.titulo, intervalo.inicio, intervalo.fim,
                pessoaId, LocalDateTime.now(relogio)));
        return visao(pessoaId, salvo.getId());
    }

    @Override
    @Transactional
    public EventoResumo editar(Long pessoaId, Long eventoId, DadosEvento dados) {
        Evento evento = carregar(eventoId);
        exigirEscrita(pessoaId, areas.buscar(pessoaId, evento.getAreaId()));
        Intervalo intervalo = validar(dados);
        evento.alterar(intervalo.titulo, intervalo.inicio, intervalo.fim, pessoaId, LocalDateTime.now(relogio));
        return visao(pessoaId, eventoId);
    }

    @Override
    @Transactional
    public void excluir(Long pessoaId, Long eventoId) {
        Evento evento = carregar(eventoId);
        exigirEscrita(pessoaId, areas.buscar(pessoaId, evento.getAreaId()));
        eventos.delete(evento);
    }

    // ----- apoio -----

    private record Intervalo(String titulo, LocalDateTime inicio, LocalDateTime fim) {
    }

    private Intervalo validar(DadosEvento dados) {
        String titulo = dados.titulo() == null ? "" : dados.titulo().trim();
        if (titulo.isEmpty()) {
            throw new EventoInvalidoException("Informe o título do evento.");
        }
        if (titulo.length() > TAMANHO_MAXIMO_TITULO) {
            throw new EventoInvalidoException("O título deve ter no máximo " + TAMANHO_MAXIMO_TITULO + " caracteres.");
        }
        if (dados.inicio() == null) {
            throw new EventoInvalidoException("Informe a data e a hora do evento.");
        }
        Integer minutos = dados.duracaoMinutos();
        if (minutos == null || minutos < 1 || minutos > DURACAO_MAXIMA_MINUTOS) {
            throw new EventoInvalidoException("A duração deve ficar entre 1 minuto e 24 horas.");
        }
        return new Intervalo(titulo, dados.inicio(), dados.inicio().plusMinutes(minutos));
    }

    private void exigirEscrita(Long pessoaId, AreaResumo area) {
        if (!politica.podeEscrever(pessoaId, area)) {
            throw new AcessoNegadoException("Sem permissão de escrita na Área " + area.id());
        }
    }

    private Evento carregar(Long eventoId) {
        return eventos.findById(eventoId).orElseThrow(() -> new EventoNaoEncontradoException(eventoId));
    }

    private EventoResumo visao(Long pessoaId, Long eventoId) {
        return visaoDaPessoa(pessoaId).stream().filter(e -> e.id().equals(eventoId)).findFirst()
                .orElseThrow(() -> new EventoNaoEncontradoException(eventoId));
    }

    /** Todos os eventos das Áreas que a pessoa enxerga, já com a marca de conflito (entre Áreas inclusive). */
    private List<EventoResumo> visaoDaPessoa(Long pessoaId) {
        Map<Long, String> nomes = new HashMap<>();
        for (AreaResumo area : areas.listarAcessiveis(pessoaId)) {
            nomes.put(area.id(), area.nome());
        }
        if (nomes.isEmpty()) {
            return List.of();
        }
        List<Evento> todos = eventos.findByAreaIdInOrderByInicioAsc(nomes.keySet());
        Set<Long> emConflito = idsEmConflito(todos);
        return todos.stream()
                .map(e -> new EventoResumo(e.getId(), e.getAreaId(), nomes.get(e.getAreaId()), e.getTitulo(),
                        e.getInicio(), e.getFim(), emConflito.contains(e.getId())))
                .toList();
    }

    static Set<Long> idsEmConflito(List<Evento> todos) {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < todos.size(); i++) {
            for (int j = i + 1; j < todos.size(); j++) {
                if (todos.get(i).conflitaCom(todos.get(j))) {
                    ids.add(todos.get(i).getId());
                    ids.add(todos.get(j).getId());
                }
            }
        }
        return ids;
    }
}
