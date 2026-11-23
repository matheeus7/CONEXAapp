package br.conexa.notificacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.conexa.notificacoes.nucleo.AlertaService;
import br.conexa.notificacoes.porta.Alerta;
import br.conexa.notificacoes.porta.TipoAlerta;
import br.conexa.tarefas.porta.Prioridade;
import br.conexa.tarefas.porta.StatusTarefa;
import br.conexa.tarefas.porta.TarefaPort;
import br.conexa.tarefas.porta.TarefaResumo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CT-070 a CT-073: os alertas de atraso e de prazo em menos de 24 horas. */
@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    static final Long ANA = 1L;
    static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Mock TarefaPort tarefas;
    AlertaService servico;

    @BeforeEach
    void preparar() {
        servico = new AlertaService(tarefas, Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private TarefaResumo tarefa(long id, LocalDateTime prazo) {
        return new TarefaResumo(id, 100L, "Tarefa " + id, null, prazo, Prioridade.MEDIA, StatusTarefa.PENDENTE, ANA,
                prazo.isBefore(AGORA));
    }

    @Test
    @DisplayName("CT-070 prazo em 10 h aparece; CT-072 tarefa vencida aparece como atrasada, primeiro")
    void alertas() {
        TarefaResumo vencida = tarefa(1, AGORA.minusDays(1));
        TarefaResumo logo = tarefa(2, AGORA.plusHours(10));
        when(tarefas.listarAtrasadas(ANA)).thenReturn(List.of(vencida));
        when(tarefas.listarPendentesDoResponsavel(eq(ANA), any(LocalDateTime.class))).thenReturn(List.of(vencida, logo));

        List<Alerta> lista = servico.listarAlertas(ANA);

        assertThat(lista).extracting(Alerta::tipo).containsExactly(TipoAlerta.TAREFA_ATRASADA, TipoAlerta.PRAZO_PROXIMO);
        assertThat(lista).extracting(a -> a.tarefa().id()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("CT-071 e CT-073 sem tarefas atrasadas nem a menos de 24 h, não há alertas")
    void semAlertas() {
        when(tarefas.listarAtrasadas(ANA)).thenReturn(List.of());
        when(tarefas.listarPendentesDoResponsavel(eq(ANA), any(LocalDateTime.class))).thenReturn(List.of());

        assertThat(servico.listarAlertas(ANA)).isEmpty();
    }
}
