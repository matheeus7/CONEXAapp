package br.conexa.conexoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.conexa.areas.porta.AreaResumo;
import br.conexa.conexoes.nucleo.Conexao;
import br.conexa.conexoes.nucleo.ConexaoRepositorio;
import br.conexa.conexoes.nucleo.PoliticaDeAcessoService;
import br.conexa.conexoes.porta.AcessoNegadoException;
import br.conexa.conexoes.porta.NivelPermissao;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A matriz de permissões (RF09): dono, Colaborador, Visualizador e quem não tem conexão. */
class PoliticaDeAcessoServiceTest {

    static final Long ANA = 1L, BRUNO = 2L, CARLA = 3L, DIOGO = 4L;
    static final AreaResumo FACULDADE = new AreaResumo(100L, "Faculdade", ANA, LocalDateTime.of(2026, 11, 1, 9, 0));

    ConexaoRepositorio conexoes = mock(ConexaoRepositorio.class);
    PoliticaDeAcessoService politica = new PoliticaDeAcessoService(conexoes);

    @BeforeEach
    void preparar() {
        when(conexoes.findByAreaIdAndPessoaId(100L, BRUNO)).thenReturn(Optional.of(
                new Conexao(100L, BRUNO, NivelPermissao.COLABORADOR, LocalDateTime.now())));
        when(conexoes.findByAreaIdAndPessoaId(100L, CARLA)).thenReturn(Optional.of(
                new Conexao(100L, CARLA, NivelPermissao.VISUALIZADOR, LocalDateTime.now())));
        when(conexoes.findByAreaIdAndPessoaId(100L, DIOGO)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("o dono pode tudo")
    void dono() {
        assertThat(politica.podeVer(ANA, FACULDADE)).isTrue();
        assertThat(politica.podeEscrever(ANA, FACULDADE)).isTrue();
        assertThat(politica.ehDono(ANA, FACULDADE)).isTrue();
        assertThat(politica.podeSerResponsavel(ANA, FACULDADE)).isTrue();
    }

    @Test
    @DisplayName("CT-046 e CT-045 Visualizador lê, mas não escreve, não é dono e não é responsável")
    void visualizador() {
        assertThat(politica.podeVer(CARLA, FACULDADE)).isTrue();
        assertThat(politica.podeEscrever(CARLA, FACULDADE)).isFalse();
        assertThat(politica.ehDono(CARLA, FACULDADE)).isFalse();
        assertThat(politica.podeSerResponsavel(CARLA, FACULDADE)).isFalse();
    }

    @Test
    @DisplayName("CT-047 e CT-048 Colaborador lê, escreve e pode ser responsável, mas não gere a Área")
    void colaborador() {
        assertThat(politica.podeVer(BRUNO, FACULDADE)).isTrue();
        assertThat(politica.podeEscrever(BRUNO, FACULDADE)).isTrue();
        assertThat(politica.podeSerResponsavel(BRUNO, FACULDADE)).isTrue();
        assertThat(politica.ehDono(BRUNO, FACULDADE)).isFalse();
        assertThatThrownBy(() -> politica.exigirDono(BRUNO, FACULDADE)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("CT-038 quem não tem conexão não vê nada")
    void semConexao() {
        assertThat(politica.podeVer(DIOGO, FACULDADE)).isFalse();
        assertThat(politica.podeEscrever(DIOGO, FACULDADE)).isFalse();
        assertThat(politica.podeSerResponsavel(DIOGO, FACULDADE)).isFalse();
        assertThat(politica.podeVer(null, FACULDADE)).isFalse();
        assertThatThrownBy(() -> politica.exigirVer(DIOGO, FACULDADE)).isInstanceOf(AcessoNegadoException.class);
    }
}
