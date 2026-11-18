package br.conexa.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.conexa.areas.porta.AreaExcluidaEvent;
import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.nucleo.PoliticaDeAcessoService;
import br.conexa.conexoes.porta.AcessoNegadoException;
import br.conexa.eventos.nucleo.Evento;
import br.conexa.eventos.nucleo.EventoRepositorio;
import br.conexa.eventos.nucleo.EventoService;
import br.conexa.eventos.porta.DadosEvento;
import br.conexa.eventos.porta.EventoInvalidoException;
import br.conexa.eventos.porta.EventoResumo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** CT-087 aplicado a Eventos: núcleo testado com dublês nas portas e no repositório. */
@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    static final Long ANA = 1L;
    static final Long BRUNO = 2L;
    static final LocalDateTime DIA = LocalDateTime.of(2026, 11, 10, 0, 0);

    @Mock EventoRepositorio eventos;
    @Mock AreaPort areas;

    EventoService servico;
    AreaResumo faculdade = new AreaResumo(100L, "Faculdade", ANA, DIA);
    AreaResumo casa = new AreaResumo(101L, "Casa", ANA, DIA);

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(DIA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servico = new EventoService(eventos, areas, new PoliticaDeAcessoService(org.mockito.Mockito.mock(br.conexa.conexoes.nucleo.ConexaoRepositorio.class)), relogio);
    }

    private Evento evento(long id, long areaId, int horaInicio, int minInicio, int horaFim, int minFim) {
        Evento e = new Evento(areaId, "Evento " + id, DIA.withHour(horaInicio).withMinute(minInicio),
                DIA.withHour(horaFim).withMinute(minFim), ANA, DIA);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private void aPessoaTemEventos(Evento... lista) {
        when(areas.listarAcessiveis(ANA)).thenReturn(List.of(faculdade, casa));
        when(eventos.findByAreaIdInOrderByInicioAsc(anyCollection())).thenReturn(List.of(lista));
    }

    @Test
    @DisplayName("Evento: sobreposição conflita; fim = início não conflita (CT-029, CT-031)")
    void conflitaCom() {
        assertThat(evento(1, 100, 10, 0, 11, 0).conflitaCom(evento(2, 100, 10, 30, 11, 30))).isTrue();
        assertThat(evento(2, 100, 10, 30, 11, 30).conflitaCom(evento(1, 100, 10, 0, 11, 0))).isTrue();
        assertThat(evento(1, 100, 10, 0, 11, 0).conflitaCom(evento(3, 100, 11, 0, 12, 0))).isFalse();
        assertThat(evento(3, 100, 11, 0, 12, 0).conflitaCom(evento(1, 100, 10, 0, 11, 0))).isFalse();
    }

    @Test
    @DisplayName("CT-026 criar evento: fim = início + duração")
    void criar() {
        List<Evento> guardados = new ArrayList<>();
        when(areas.buscar(ANA, 100L)).thenReturn(faculdade);
        when(areas.listarAcessiveis(ANA)).thenReturn(List.of(faculdade));
        when(eventos.save(any(Evento.class))).thenAnswer(i -> {
            Evento e = i.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 50L);
            guardados.add(e);
            return e;
        });
        when(eventos.findByAreaIdInOrderByInicioAsc(anyCollection())).thenAnswer(i -> guardados);

        EventoResumo criado = servico.criar(ANA, 100L, new DadosEvento("  Prova ", DIA.withHour(14), 90));

        assertThat(criado.titulo()).isEqualTo("Prova");
        assertThat(criado.fim()).isEqualTo(DIA.withHour(15).withMinute(30));
        assertThat(criado.areaNome()).isEqualTo("Faculdade");
        assertThat(criado.emConflito()).isFalse();
    }

    @Test
    @DisplayName("CT-027 Área obrigatória; título, início e duração inválidos são recusados")
    void validacoes() {
        when(areas.buscar(ANA, 100L)).thenReturn(faculdade);

        assertThatThrownBy(() -> servico.criar(ANA, null, new DadosEvento("X", DIA, 30)))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> servico.criar(ANA, 100L, new DadosEvento("  ", DIA, 30)))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> servico.criar(ANA, 100L, new DadosEvento("X", null, 30)))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> servico.criar(ANA, 100L, new DadosEvento("X", DIA, 0)))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> servico.criar(ANA, 100L, new DadosEvento("X", DIA, 1441)))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> servico.criar(ANA, 100L, new DadosEvento("X", DIA, null)))
                .isInstanceOf(EventoInvalidoException.class);
        verify(eventos, never()).save(any());
    }

    @Test
    @DisplayName("CT-029 e CT-030 conflito, inclusive entre Áreas diferentes: ambos sinalizados")
    void conflitoEntreAreas() {
        aPessoaTemEventos(evento(1, 100, 10, 0, 11, 0), evento(2, 101, 10, 30, 11, 30));

        List<EventoResumo> lista = servico.listarDoPeriodo(ANA, DIA, DIA.plusDays(1));

        assertThat(lista).hasSize(2).allMatch(EventoResumo::emConflito);
    }

    @Test
    @DisplayName("CT-031 eventos que só se tocam não são sinalizados")
    void eventosQueSeTocam() {
        aPessoaTemEventos(evento(1, 100, 10, 0, 11, 0), evento(2, 101, 11, 0, 12, 0));

        assertThat(servico.listarDoPeriodo(ANA, DIA, DIA.plusDays(1))).noneMatch(EventoResumo::emConflito);
    }

    @Test
    @DisplayName("o período só devolve eventos que o cruzam")
    void periodo() {
        aPessoaTemEventos(evento(1, 100, 10, 0, 11, 0), evento(2, 101, 18, 0, 19, 0));

        List<EventoResumo> manha = servico.listarDoPeriodo(ANA, DIA.withHour(9), DIA.withHour(12));

        assertThat(manha).extracting(EventoResumo::id).containsExactly(1L);
    }

    @Test
    @DisplayName("quem não é dono não cria evento na Área")
    void semPermissao() {
        when(areas.buscar(BRUNO, 100L)).thenReturn(faculdade);

        assertThatThrownBy(() -> servico.criar(BRUNO, 100L, new DadosEvento("X", DIA, 30)))
                .isInstanceOf(AcessoNegadoException.class);
        verify(eventos, never()).save(any());
    }

    @Test
    @DisplayName("Observer: Área excluída remove os eventos dela")
    void aoExcluirArea() {
        servico.aoExcluirArea(new AreaExcluidaEvent(100L));

        verify(eventos).deletarDaArea(100L);
    }
}
