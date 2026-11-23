package br.conexa.radar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.nucleo.ConexaoRepositorio;
import br.conexa.conexoes.nucleo.PoliticaDeAcessoService;
import br.conexa.eventos.porta.EventoPort;
import br.conexa.eventos.porta.EventoResumo;
import br.conexa.radar.nucleo.RadarService;
import br.conexa.radar.porta.IndiceCarga;
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

/** CT-055 a CT-069: a regra de pontos do Radar, com dublês nas portas de Tarefas, Eventos e Áreas. */
@ExtendWith(MockitoExtension.class)
class RadarServiceTest {

    static final Long ANA = 1L, DONA_DE_FORA = 9L;
    static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Mock TarefaPort tarefas;
    @Mock EventoPort eventos;
    @Mock AreaPort areas;

    RadarService radar;
    AreaResumo minhaArea = new AreaResumo(100L, "Faculdade", ANA, AGORA.minusDays(30));
    AreaResumo areaDeLeitura = new AreaResumo(200L, "Da Carla", DONA_DE_FORA, AGORA.minusDays(30));

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        radar = new RadarService(tarefas, eventos, areas, new PoliticaDeAcessoService(mock(ConexaoRepositorio.class)),
                relogio, 4);
    }

    private TarefaResumo tarefa(Prioridade prioridade, LocalDateTime prazo) {
        return new TarefaResumo(1L, 100L, "Tarefa", null, prazo, prioridade, StatusTarefa.PENDENTE, ANA,
                prazo.isBefore(AGORA));
    }

    private EventoResumo evento(long id, long areaId, LocalDateTime inicio, boolean conflito) {
        return new EventoResumo(id, areaId, "Área", "Evento " + id, inicio, inicio.plusHours(1), conflito);
    }

    private void tarefasAtivas(TarefaResumo... lista) {
        when(tarefas.listarPendentesDoResponsavel(eq(ANA), any(LocalDateTime.class))).thenReturn(List.of(lista));
    }

    private void eventosAtivos(EventoResumo... lista) {
        when(areas.listarAcessiveis(ANA)).thenReturn(List.of(minhaArea, areaDeLeitura));
        when(eventos.listarDoPeriodo(eq(ANA), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(lista));
    }

    @Test
    @DisplayName("CT-055 sem itens ativos, o Índice é 0")
    void semItens() {
        IndiceCarga indice = radar.calcularIndice(ANA);

        assertThat(indice.valor()).isZero();
        assertThat(indice.itens()).isEmpty();
    }

    @Test
    @DisplayName("CT-056 uma tarefa Alta atrasada: 3 + 2 = 5 pontos, Índice 20")
    void altaAtrasada() {
        tarefasAtivas(tarefa(Prioridade.ALTA, AGORA.minusDays(1)));

        IndiceCarga indice = radar.calcularIndice(ANA);

        assertThat(indice.somaPontos()).isEqualTo(5);
        assertThat(indice.valor()).isEqualTo(20);
    }

    @Test
    @DisplayName("CT-057 uma tarefa Média com prazo amanhã: 2 + 1 = 3 pontos, Índice 12")
    void mediaAmanha() {
        tarefasAtivas(tarefa(Prioridade.MEDIA, AGORA.plusDays(1)));

        assertThat(radar.calcularIndice(ANA).valor()).isEqualTo(12);
    }

    @Test
    @DisplayName("CT-058 as duas juntas: Índice 32")
    void soma() {
        tarefasAtivas(tarefa(Prioridade.ALTA, AGORA.minusDays(1)), tarefa(Prioridade.MEDIA, AGORA.plusDays(1)));

        assertThat(radar.calcularIndice(ANA).valor()).isEqualTo(32);
    }

    @Test
    @DisplayName("CT-059 o Índice nunca passa de 100")
    void teto() {
        TarefaResumo[] dez = new TarefaResumo[10];
        for (int i = 0; i < 10; i++) {
            dez[i] = tarefa(Prioridade.ALTA, AGORA.minusDays(1));
        }
        tarefasAtivas(dez);

        IndiceCarga indice = radar.calcularIndice(ANA);

        assertThat(indice.somaPontos()).isEqualTo(50);
        assertThat(indice.valor()).isEqualTo(100);
    }

    @Test
    @DisplayName("CT-061 tarefa Baixa com prazo em 5 dias: 1 + 0 = 1 ponto, Índice 4")
    void baixaEmCincoDias() {
        tarefasAtivas(tarefa(Prioridade.BAIXA, AGORA.plusDays(5)));

        assertThat(radar.calcularIndice(ANA).valor()).isEqualTo(4);
    }

    @Test
    @DisplayName("CT-064 um evento nos próximos 2 dias: 2 + 1 = 3 pontos, Índice 12")
    void evento() {
        eventosAtivos(evento(1, 100L, AGORA.plusDays(1), false));

        assertThat(radar.calcularIndice(ANA).valor()).isEqualTo(12);
    }

    @Test
    @DisplayName("evento em 4 dias tem urgência 0; evento em Área só de leitura não entra")
    void eventoDistanteEAreaSemEscrita() {
        eventosAtivos(evento(1, 100L, AGORA.plusDays(4), false), evento(2, 200L, AGORA.plusDays(1), false));

        IndiceCarga indice = radar.calcularIndice(ANA);

        assertThat(indice.itens()).hasSize(1);
        assertThat(indice.somaPontos()).isEqualTo(2);
    }

    @Test
    @DisplayName("CT-066 conflito de horário não altera o Índice, mas aparece na lista de conflitos")
    void conflitoNaoAlteraIndice() {
        eventosAtivos(evento(1, 100L, AGORA.plusDays(1), true), evento(2, 100L, AGORA.plusDays(1).plusMinutes(30), true));
        int comConflito = radar.calcularIndice(ANA).valor();
        assertThat(radar.detectarConflitos(ANA)).hasSize(2);

        eventosAtivos(evento(1, 100L, AGORA.plusDays(1), false), evento(2, 100L, AGORA.plusDays(1).plusHours(3), false));

        assertThat(radar.calcularIndice(ANA).valor()).isEqualTo(comConflito).isEqualTo(24);
        assertThat(radar.detectarConflitos(ANA)).isEmpty();
    }

    @Test
    @DisplayName("CT-069 determinismo: os mesmos dados dão o mesmo resultado")
    void determinismo() {
        tarefasAtivas(tarefa(Prioridade.ALTA, AGORA.minusDays(1)), tarefa(Prioridade.BAIXA, AGORA.plusDays(3)));

        assertThat(radar.calcularIndice(ANA)).isEqualTo(radar.calcularIndice(ANA));
    }
}
