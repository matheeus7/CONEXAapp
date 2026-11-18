package br.conexa.conexoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.conexa.areas.porta.AreaExcluidaEvent;
import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.conexoes.nucleo.Conexao;
import br.conexa.conexoes.nucleo.ConexaoRepositorio;
import br.conexa.conexoes.nucleo.ConexaoService;
import br.conexa.conexoes.nucleo.PoliticaDeAcessoService;
import br.conexa.conexoes.porta.AcessoNegadoException;
import br.conexa.conexoes.porta.ConexaoInvalidaException;
import br.conexa.conexoes.porta.ConexaoResumo;
import br.conexa.conexoes.porta.ConexaoRevogadaEvent;
import br.conexa.conexoes.porta.NivelPermissao;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/** CT-087 aplicado a Conexões: núcleo testado com dublês. */
@ExtendWith(MockitoExtension.class)
class ConexaoServiceTest {

    static final Long ANA = 1L, BRUNO = 2L, CARLA = 3L;
    static final Long AREA = 100L;
    static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Mock ConexaoRepositorio conexoes;
    @Mock AreaPort areas;
    @Mock AutenticacaoPort autenticacao;
    @Mock ApplicationEventPublisher eventos;

    ConexaoService servico;
    AreaResumo faculdade = new AreaResumo(AREA, "Faculdade", ANA, AGORA);
    PessoaResumo bruno = new PessoaResumo(BRUNO, "Bruno Lima", "bruno@exemplo.com");

    @BeforeEach
    void preparar() {
        var politica = new PoliticaDeAcessoService(mock(ConexaoRepositorio.class));
        servico = new ConexaoService(conexoes, areas, autenticacao, politica, eventos,
                Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("CT-037 conectar pessoa cadastrada cria a conexão na hora")
    void conectar() {
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);
        when(autenticacao.buscarPorEmail("bruno@exemplo.com")).thenReturn(Optional.of(bruno));
        when(conexoes.existsByAreaIdAndPessoaId(AREA, BRUNO)).thenReturn(false);
        when(conexoes.save(any(Conexao.class))).thenAnswer(i -> i.getArgument(0));

        ConexaoResumo resumo = servico.conectar(ANA, AREA, "bruno@exemplo.com", NivelPermissao.COLABORADOR);

        assertThat(resumo.pessoaId()).isEqualTo(BRUNO);
        assertThat(resumo.nivel()).isEqualTo(NivelPermissao.COLABORADOR);
    }

    @Test
    @DisplayName("CT-039 e-mail sem conta é recusado")
    void emailDesconhecido() {
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);
        when(autenticacao.buscarPorEmail("x@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.conectar(ANA, AREA, "x@x.com", NivelPermissao.VISUALIZADOR))
                .isInstanceOf(ConexaoInvalidaException.class);
        verify(conexoes, never()).save(any());
    }

    @Test
    @DisplayName("CT-040 auto-conexão é recusada")
    void autoConexao() {
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);
        when(autenticacao.buscarPorEmail("ana@exemplo.com"))
                .thenReturn(Optional.of(new PessoaResumo(ANA, "Ana Souza", "ana@exemplo.com")));

        assertThatThrownBy(() -> servico.conectar(ANA, AREA, "ana@exemplo.com", NivelPermissao.VISUALIZADOR))
                .isInstanceOf(ConexaoInvalidaException.class);
    }

    @Test
    @DisplayName("CT-041 conexão duplicada é recusada")
    void duplicada() {
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);
        when(autenticacao.buscarPorEmail("bruno@exemplo.com")).thenReturn(Optional.of(bruno));
        when(conexoes.existsByAreaIdAndPessoaId(AREA, BRUNO)).thenReturn(true);

        assertThatThrownBy(() -> servico.conectar(ANA, AREA, "bruno@exemplo.com", NivelPermissao.VISUALIZADOR))
                .isInstanceOf(ConexaoInvalidaException.class);
        verify(conexoes, never()).save(any());
    }

    @Test
    @DisplayName("CT-044 quem não é dono não conecta nem revoga")
    void naoDonoNaoGerencia() {
        when(areas.buscar(BRUNO, AREA)).thenReturn(faculdade);
        Conexao dele = new Conexao(AREA, BRUNO, NivelPermissao.COLABORADOR, AGORA);
        ReflectionTestUtils.setField(dele, "id", 9L);
        when(conexoes.findById(9L)).thenReturn(Optional.of(dele));

        assertThatThrownBy(() -> servico.conectar(BRUNO, AREA, "carla@exemplo.com", NivelPermissao.VISUALIZADOR))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> servico.revogar(BRUNO, 9L)).isInstanceOf(AcessoNegadoException.class);
        verify(conexoes, never()).delete(any());
    }

    @Test
    @DisplayName("CT-050 o dono muda o nível da conexão")
    void alterarNivel() {
        Conexao conexao = new Conexao(AREA, BRUNO, NivelPermissao.COLABORADOR, AGORA);
        ReflectionTestUtils.setField(conexao, "id", 9L);
        when(conexoes.findById(9L)).thenReturn(Optional.of(conexao));
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);
        when(autenticacao.buscarPorId(BRUNO)).thenReturn(Optional.of(bruno));

        ConexaoResumo resumo = servico.alterarNivel(ANA, 9L, NivelPermissao.VISUALIZADOR);

        assertThat(resumo.nivel()).isEqualTo(NivelPermissao.VISUALIZADOR);
        assertThat(conexao.getNivel()).isEqualTo(NivelPermissao.VISUALIZADOR);
    }

    @Test
    @DisplayName("CT-052 revogar publica o evento que devolve as tarefas ao dono")
    void revogar() {
        Conexao conexao = new Conexao(AREA, BRUNO, NivelPermissao.COLABORADOR, AGORA);
        ReflectionTestUtils.setField(conexao, "id", 9L);
        when(conexoes.findById(9L)).thenReturn(Optional.of(conexao));
        when(areas.buscar(ANA, AREA)).thenReturn(faculdade);

        servico.revogar(ANA, 9L);

        verify(eventos).publishEvent(new ConexaoRevogadaEvent(AREA, BRUNO, ANA));
        verify(conexoes).delete(conexao);
    }

    @Test
    @DisplayName("só Colaboradores entram na lista de candidatos a responsável")
    void colaboradores() {
        when(conexoes.findByAreaIdOrderByCriadaEmAsc(AREA)).thenReturn(List.of(
                new Conexao(AREA, BRUNO, NivelPermissao.COLABORADOR, AGORA),
                new Conexao(AREA, CARLA, NivelPermissao.VISUALIZADOR, AGORA)));

        assertThat(servico.colaboradoresDaArea(AREA)).containsExactly(BRUNO);
    }

    @Test
    @DisplayName("Observer: Área excluída remove as conexões dela")
    void aoExcluirArea() {
        servico.aoExcluirArea(new AreaExcluidaEvent(AREA));

        verify(conexoes).deletarDaArea(AREA);
    }
}
