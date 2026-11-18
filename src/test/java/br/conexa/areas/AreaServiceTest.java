package br.conexa.areas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.conexa.areas.nucleo.Area;
import br.conexa.areas.nucleo.AreaRepositorio;
import br.conexa.areas.nucleo.AreaService;
import br.conexa.areas.nucleo.MinhaVida;
import br.conexa.areas.nucleo.MinhaVidaRepositorio;
import br.conexa.areas.porta.AreaExcluidaEvent;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.areas.porta.NomeDeAreaInvalidoException;
import br.conexa.conexoes.nucleo.PoliticaDeAcessoService;
import br.conexa.conexoes.porta.AcessoNegadoException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/** CT-087 aplicado a Áreas: o núcleo é testado com dublês, sem banco. */
@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    static final Long ANA = 1L;
    static final Long BRUNO = 2L;
    static final Long VIDA_DA_ANA = 10L;
    static final Long AREA = 100L;

    @Mock AreaRepositorio areas;
    @Mock MinhaVidaRepositorio minhasVidas;
    @Mock ApplicationEventPublisher eventos;

    AreaService servico;
    Area faculdade;

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);
        servico = new AreaService(areas, minhasVidas, new PoliticaDeAcessoService(org.mockito.Mockito.mock(br.conexa.conexoes.nucleo.ConexaoRepositorio.class)), eventos, relogio);
        faculdade = new Area(VIDA_DA_ANA, "Faculdade", LocalDateTime.of(2026, 11, 1, 9, 0));
        ReflectionTestUtils.setField(faculdade, "id", AREA);
    }

    private MinhaVida vidaDaAna() {
        MinhaVida vida = new MinhaVida(ANA, LocalDateTime.of(2026, 11, 1, 8, 0));
        ReflectionTestUtils.setField(vida, "id", VIDA_DA_ANA);
        return vida;
    }

    @Test
    @DisplayName("CT-012 criar Área associa à Minha Vida da pessoa")
    void criar() {
        when(minhasVidas.findByPessoaId(ANA)).thenReturn(Optional.of(vidaDaAna()));
        when(areas.existsByMinhaVidaIdAndNomeIgnoreCase(VIDA_DA_ANA, "Casa")).thenReturn(false);
        when(areas.save(any(Area.class))).thenAnswer(i -> i.getArgument(0));

        AreaResumo criada = servico.criar(ANA, "  Casa ");

        ArgumentCaptor<Area> capturada = ArgumentCaptor.forClass(Area.class);
        verify(areas).save(capturada.capture());
        assertThat(capturada.getValue().getMinhaVidaId()).isEqualTo(VIDA_DA_ANA);
        assertThat(criada.nome()).isEqualTo("Casa");
    }

    @Test
    @DisplayName("nome vazio, longo ou repetido é recusado")
    void nomeInvalido() {
        when(minhasVidas.findByPessoaId(ANA)).thenReturn(Optional.of(vidaDaAna()));
        when(areas.existsByMinhaVidaIdAndNomeIgnoreCase(VIDA_DA_ANA, "Casa")).thenReturn(true);

        assertThatThrownBy(() -> servico.criar(ANA, "   ")).isInstanceOf(NomeDeAreaInvalidoException.class);
        assertThatThrownBy(() -> servico.criar(ANA, "x".repeat(81))).isInstanceOf(NomeDeAreaInvalidoException.class);
        assertThatThrownBy(() -> servico.criar(ANA, "Casa")).isInstanceOf(NomeDeAreaInvalidoException.class);
        verify(areas, never()).save(any());
    }

    @Test
    @DisplayName("CT-013 o dono renomeia a Área")
    void renomear() {
        when(areas.findById(AREA)).thenReturn(Optional.of(faculdade));
        when(minhasVidas.findById(VIDA_DA_ANA)).thenReturn(Optional.of(vidaDaAna()));
        when(areas.existsByMinhaVidaIdAndNomeIgnoreCase(VIDA_DA_ANA, "Estudos")).thenReturn(false);

        assertThat(servico.renomear(ANA, AREA, "Estudos").nome()).isEqualTo("Estudos");
        assertThat(faculdade.getNome()).isEqualTo("Estudos");
    }

    @Test
    @DisplayName("CT-014 o dono exclui a Área e o evento é publicado")
    void excluir() {
        when(areas.findById(AREA)).thenReturn(Optional.of(faculdade));
        when(minhasVidas.findById(VIDA_DA_ANA)).thenReturn(Optional.of(vidaDaAna()));

        servico.excluir(ANA, AREA);

        verify(areas).delete(faculdade);
        verify(eventos).publishEvent(new AreaExcluidaEvent(AREA));
    }

    @Test
    @DisplayName("CT-017 quem não é dono não renomeia nem exclui")
    void naoDono() {
        when(areas.findById(AREA)).thenReturn(Optional.of(faculdade));
        when(minhasVidas.findById(VIDA_DA_ANA)).thenReturn(Optional.of(vidaDaAna()));

        assertThatThrownBy(() -> servico.renomear(BRUNO, AREA, "Outro")).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> servico.excluir(BRUNO, AREA)).isInstanceOf(AcessoNegadoException.class);
        assertThat(faculdade.getNome()).isEqualTo("Faculdade");
        verify(areas, never()).delete(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }
}
