package br.conexa.notificacoes.nucleo;

import br.conexa.notificacoes.porta.Alerta;
import br.conexa.notificacoes.porta.NotificacaoPort;
import br.conexa.notificacoes.porta.TipoAlerta;
import br.conexa.tarefas.porta.TarefaPort;
import br.conexa.tarefas.porta.TarefaResumo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula os alertas no momento da consulta: uma tarefa vira atrasada pela passagem do tempo, sem que
 * nada seja alterado, então não há evento nem registro a manter (CT-074).
 */
@Service
public class AlertaService implements NotificacaoPort {

    static final int HORAS_DE_AVISO = 24;

    private final TarefaPort tarefas;
    private final Clock relogio;

    public AlertaService(TarefaPort tarefas, Clock relogio) {
        this.tarefas = tarefas;
        this.relogio = relogio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alerta> listarAlertas(Long pessoaId) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Alerta> alertas = new ArrayList<>();
        for (TarefaResumo t : tarefas.listarAtrasadas(pessoaId)) {
            alertas.add(new Alerta(t, TipoAlerta.TAREFA_ATRASADA, t.prazo()));
        }
        for (TarefaResumo t : tarefas.listarPendentesDoResponsavel(pessoaId, agora.plusHours(HORAS_DE_AVISO))) {
            if (!t.prazo().isBefore(agora)) {
                alertas.add(new Alerta(t, TipoAlerta.PRAZO_PROXIMO, t.prazo()));
            }
        }
        return alertas;
    }
}
