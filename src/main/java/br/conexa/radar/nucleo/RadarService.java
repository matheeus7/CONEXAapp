package br.conexa.radar.nucleo;

import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.porta.PoliticaDeAcesso;
import br.conexa.eventos.porta.EventoPort;
import br.conexa.eventos.porta.EventoResumo;
import br.conexa.radar.porta.IndiceCarga;
import br.conexa.radar.porta.ItemCarga;
import br.conexa.radar.porta.RadarPort;
import br.conexa.tarefas.porta.Prioridade;
import br.conexa.tarefas.porta.TarefaPort;
import br.conexa.tarefas.porta.TarefaResumo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra determinística do escopo: para cada item, pontos = peso da prioridade (Alta 3, Média 2, Baixa 1)
 * + peso da urgência (vencido +2, até 2 dias +1, demais 0). Índice = min(100, soma × fator de escala).
 * Eventos entram como Média e nunca estão vencidos. Conflitos de horário não alteram o número.
 */
@Service
public class RadarService implements RadarPort {

    static final int JANELA_DIAS = 7;
    static final int URGENCIA_DIAS = 2;
    static final int PESO_EVENTO = 2;

    private final TarefaPort tarefas;
    private final EventoPort eventos;
    private final AreaPort areas;
    private final PoliticaDeAcesso politica;
    private final Clock relogio;
    private final int fatorDeEscala;

    public RadarService(TarefaPort tarefas, EventoPort eventos, AreaPort areas, PoliticaDeAcesso politica,
                        Clock relogio, @Value("${conexa.radar.fator-de-escala:4}") int fatorDeEscala) {
        this.tarefas = tarefas;
        this.eventos = eventos;
        this.areas = areas;
        this.politica = politica;
        this.relogio = relogio;
        this.fatorDeEscala = fatorDeEscala;
    }

    @Override
    @Transactional(readOnly = true)
    public IndiceCarga calcularIndice(Long pessoaId) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDateTime limite = agora.plusDays(JANELA_DIAS);
        LocalDateTime urgente = agora.plusDays(URGENCIA_DIAS);
        List<ItemCarga> itens = new ArrayList<>();

        // Tarefas pendentes em que a pessoa é a responsável (atrasadas ou nos próximos 7 dias)
        for (TarefaResumo t : tarefas.listarPendentesDoResponsavel(pessoaId, limite)) {
            int urgencia = t.prazo().isBefore(agora) ? 2 : (t.prazo().isAfter(urgente) ? 0 : 1);
            itens.add(new ItemCarga("Tarefa", t.titulo(), t.prazo(), peso(t.prioridade()), urgencia));
        }

        // Eventos dos próximos 7 dias nas Áreas em que a pessoa pode escrever (dono ou Colaborador)
        Set<Long> areasComEscrita = new HashSet<>();
        for (AreaResumo a : areas.listarAcessiveis(pessoaId)) {
            if (politica.podeEscrever(pessoaId, a)) {
                areasComEscrita.add(a.id());
            }
        }
        for (EventoResumo e : eventos.listarDoPeriodo(pessoaId, agora, limite)) {
            if (areasComEscrita.contains(e.areaId())) {
                int urgencia = e.inicio().isAfter(urgente) ? 0 : 1;
                itens.add(new ItemCarga("Evento", e.titulo(), e.inicio(), PESO_EVENTO, urgencia));
            }
        }

        int soma = itens.stream().mapToInt(ItemCarga::total).sum();
        int valor = (int) Math.min(100L, (long) soma * fatorDeEscala);
        return new IndiceCarga(valor, soma, fatorDeEscala, List.copyOf(itens), agora);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarefaResumo> listarAtrasadas(Long pessoaId) {
        return tarefas.listarAtrasadas(pessoaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResumo> detectarConflitos(Long pessoaId) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        return eventos.listarDoPeriodo(pessoaId, agora, agora.plusDays(JANELA_DIAS)).stream()
                .filter(EventoResumo::emConflito)
                .toList();
    }

    static int peso(Prioridade prioridade) {
        return prioridade.getPeso();
    }
}
