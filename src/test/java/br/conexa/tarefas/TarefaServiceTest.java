package br.conexa.tarefas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.conexa.areas.porta.AreaExcluidaEvent;
import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.nucleo.PoliticaDeAcessoService;
import br.conexa.conexoes.porta.AcessoNegadoException;
import br.conexa.tarefas.nucleo.Tarefa;
import br.conexa.tarefas.nucleo.TarefaRepositorio;
import br.conexa.tarefas.nucleo.TarefaService;
import br.conexa.tarefas.porta.DadosTarefa;
import br.conexa.tarefas.porta.Prioridade;
import br.conexa.tarefas.porta.StatusTarefa;
import br.conexa.tarefas.porta.TarefaInvalidaException;
import br.conexa.tarefas.porta.TarefaResumo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** CT-087 aplicado a Tarefas: núcleo testado com dublês nas portas e no repositório. */
@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    static final Long ANA = 1L;
    static final Long BRUNO = 2L;
    static final Long AREA = 100L;
    static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Mock TarefaRepositorio tarefas;
    @Mock AreaPort areas;

    TarefaService servico;
    AreaResumo faculdade = new AreaResumo(AREA, "Faculdade", ANA, AGORA.minusDays(5));

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servico = new TarefaService(tarefas, areas, new PoliticaDeAcessoService(), relogio);
    }

    private DadosTarefa dados(String titulo, LocalDateTime prazo) {
        return new DadosTarefa(titulo, null, prazo, Prioridade.ALTA);
    }

    private Tarefa tarefaDaAna(LocalDateTime prazo) {
        Tarefa t = new Tarefa(AREA, dados("Entregar trabalho", prazo), ANA, ANA, AGORA.minusDays(1));
        ReflectionTestUtils.setField(t, "id", 7L);
        return t;
    }

    @Test
    @DisplayName("CT-018 sem responsável explícito, fica Pendente e com responsável = criador")
    void criar() {
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);
        when(tarefas.save(any(Tarefa.class))).thenAnswer(i -> i.getArgument(0));

        TarefaResumo criada = servico.criar(ANA, AREA, dados("  Entregar trabalho ", AGORA.plusDays(2)));

        assertThat(criada.titulo()).isEqualTo("Entregar trabalho");
        assertThat(criada.status()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(criada.responsavelId()).isEqualTo(ANA);
        assertThat(criada.atrasada()).isFalse();
    }

    @Test
    @DisplayName("CT-019 prazo obrigatório; título vazio também é recusado")
    void obrigatorios() {
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);

        assertThatThrownBy(() -> servico.criar(ANA, AREA, dados("Sem prazo", null)))
                .isInstanceOf(TarefaInvalidaException.class);
        assertThatThrownBy(() -> servico.criar(ANA, AREA, dados("  ", AGORA.plusDays(1))))
                .isInstanceOf(TarefaInvalidaException.class);
        verify(tarefas, never()).save(any());
    }

    @Test
    @DisplayName("CT-020 editar altera título, prazo e prioridade")
    void editar() {
        Tarefa t = tarefaDaAna(AGORA.plusDays(1));
        when(tarefas.findById(7L)).thenReturn(Optional.of(t));
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);

        TarefaResumo editada = servico.editar(ANA, 7L,
                new DadosTarefa("Novo título", "detalhe", AGORA.plusDays(3), Prioridade.BAIXA));

        assertThat(editada.titulo()).isEqualTo("Novo título");
        assertThat(editada.prazo()).isEqualTo(AGORA.plusDays(3));
        assertThat(editada.prioridade()).isEqualTo(Prioridade.BAIXA);
    }

    @Test
    @DisplayName("CT-021 concluir muda o status e tira a tarefa dos atrasos")
    void concluir() {
        Tarefa t = tarefaDaAna(AGORA.minusDays(1));
        assertThat(t.estaAtrasada(AGORA)).isTrue();
        when(tarefas.findById(7L)).thenReturn(Optional.of(t));
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);

        TarefaResumo concluida = servico.concluir(ANA, 7L);

        assertThat(concluida.status()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(concluida.atrasada()).isFalse();
    }

    @Test
    @DisplayName("CT-022 excluir remove a tarefa")
    void excluir() {
        Tarefa t = tarefaDaAna(AGORA.plusDays(1));
        when(tarefas.findById(7L)).thenReturn(Optional.of(t));
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);

        servico.excluir(ANA, 7L);

        verify(tarefas).delete(t);
    }

    @Test
    @DisplayName("CT-023 pendente com prazo vencido é atrasada; no limite não é")
    void atrasada() {
        assertThat(tarefaDaAna(AGORA.minusDays(1)).estaAtrasada(AGORA)).isTrue();
        assertThat(tarefaDaAna(AGORA).estaAtrasada(AGORA)).isFalse();
        assertThat(tarefaDaAna(AGORA.plusMinutes(1)).estaAtrasada(AGORA)).isFalse();
    }

    @Test
    @DisplayName("quem não é dono não cria nem altera tarefas da Área")
    void semPermissao() {
        Tarefa t = tarefaDaAna(AGORA.plusDays(1));
        when(areas.buscar(BRUNO, AREA)).thenReturn(faculdade);
        when(tarefas.findById(7L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> servico.criar(BRUNO, AREA, dados("X", AGORA.plusDays(1))))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> servico.concluir(BRUNO, 7L)).isInstanceOf(AcessoNegadoException.class);
        verify(tarefas, never()).save(any());
    }

    @Test
    @DisplayName("Observer: Área excluída remove as tarefas dela")
    void aoExcluirArea() {
        servico.aoExcluirArea(new AreaExcluidaEvent(AREA));

        verify(tarefas).deletarDaArea(AREA);
    }
}
