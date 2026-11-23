package br.conexa.radar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.conexa.areas.porta.AreaPort;
import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.CadastroPessoa;
import br.conexa.conexoes.porta.ConexaoPort;
import br.conexa.conexoes.porta.NivelPermissao;
import br.conexa.eventos.porta.DadosEvento;
import br.conexa.eventos.porta.EventoPort;
import br.conexa.notificacoes.porta.NotificacaoPort;
import br.conexa.notificacoes.porta.TipoAlerta;
import br.conexa.radar.porta.RadarPort;
import br.conexa.tarefas.porta.DadosTarefa;
import br.conexa.tarefas.porta.Prioridade;
import br.conexa.tarefas.porta.TarefaPort;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** CT-055 a CT-079 com banco: Radar, alertas e Dashboard de ponta a ponta. */
@SpringBootTest
@ActiveProfiles("test")
class RadarIntegracaoTest {

    private static final String SENHA = "senha-forte-1";

    @Autowired WebApplicationContext contexto;
    @Autowired AutenticacaoPort autenticacao;
    @Autowired AreaPort areas;
    @Autowired ConexaoPort conexoes;
    @Autowired TarefaPort tarefas;
    @Autowired EventoPort eventos;
    @Autowired RadarPort radar;
    @Autowired NotificacaoPort notificacoes;
    @Autowired Clock relogio;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;
    Long ana, bruno, faculdade;
    LocalDateTime agora;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        for (String t : new String[]{"conexao", "evento", "tarefa", "area", "minha_vida", "pessoa"}) {
            jdbc.update("delete from " + t);
        }
        ana = autenticacao.cadastrar(new CadastroPessoa("Ana Souza", "ana@exemplo.com", SENHA)).id();
        bruno = autenticacao.cadastrar(new CadastroPessoa("Bruno Lima", "bruno@exemplo.com", SENHA)).id();
        faculdade = areas.criar(ana, "Faculdade").id();
        agora = LocalDateTime.now(relogio);
    }

    private MockHttpSession entrar(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/entrar").with(csrf()).param("email", email).param("senha", SENHA))
                .andReturn().getRequest().getSession(false);
    }

    private Long tarefa(Prioridade prioridade, LocalDateTime prazo, Long responsavel) {
        return tarefas.criar(ana, faculdade, new DadosTarefa("Tarefa " + prioridade, null, prazo, prioridade, responsavel)).id();
    }

    @Test
    @DisplayName("CT-055 a CT-058 Índice 0 sem itens; 20 para Alta atrasada; 12 para Média amanhã; 32 juntas")
    void indice() {
        assertThat(radar.calcularIndice(ana).valor()).isZero();
        tarefa(Prioridade.ALTA, agora.minusDays(1), null);
        assertThat(radar.calcularIndice(ana).valor()).isEqualTo(20);
        tarefa(Prioridade.MEDIA, agora.plusDays(1), null);
        assertThat(radar.calcularIndice(ana).valor()).isEqualTo(32);
    }

    @Test
    @DisplayName("CT-060, CT-061 e CT-062 prazo distante e concluída não entram; Baixa em 5 dias entra com 4")
    void quemEntra() {
        tarefa(Prioridade.ALTA, agora.plusDays(10), null);
        Long concluida = tarefa(Prioridade.ALTA, agora.minusDays(1), null);
        tarefas.concluir(ana, concluida);
        assertThat(radar.calcularIndice(ana).valor()).isZero();
        tarefa(Prioridade.BAIXA, agora.plusDays(5), null);
        assertThat(radar.calcularIndice(ana).valor()).isEqualTo(4);
    }

    @Test
    @DisplayName("CT-063 e CT-076 tarefa delegada entra no Índice e nos alertas do Bruno, não nos da Ana")
    void delegada() {
        conexoes.conectar(ana, faculdade, "bruno@exemplo.com", NivelPermissao.COLABORADOR);
        tarefa(Prioridade.MEDIA, agora.plusHours(5), bruno);

        assertThat(radar.calcularIndice(ana).valor()).isZero();
        assertThat(notificacoes.listarAlertas(ana)).isEmpty();
        assertThat(radar.calcularIndice(bruno).valor()).isEqualTo(12);
        assertThat(notificacoes.listarAlertas(bruno)).hasSize(1);
    }

    @Test
    @DisplayName("CT-064 e CT-065 evento nos próximos dias entra; evento de ontem e de daqui a 10 dias, não")
    void eventos() {
        eventos.criar(ana, faculdade, new DadosEvento("Ontem", agora.minusDays(1), 60));
        eventos.criar(ana, faculdade, new DadosEvento("Daqui a 10 dias", agora.plusDays(10), 60));
        assertThat(radar.calcularIndice(ana).valor()).isZero();
        eventos.criar(ana, faculdade, new DadosEvento("Amanhã", agora.plusDays(1), 60));
        assertThat(radar.calcularIndice(ana).valor()).isEqualTo(12);
    }

    @Test
    @DisplayName("CT-066 e CT-067 conflito não altera o Índice e aparece na lista de conflitos")
    void conflito() throws Exception {
        LocalDateTime amanha = agora.plusDays(1).withMinute(0).withSecond(0).withNano(0);
        eventos.criar(ana, faculdade, new DadosEvento("A", amanha, 60));
        Long b = eventos.criar(ana, faculdade, new DadosEvento("B", amanha.plusMinutes(30), 60)).id();
        int comConflito = radar.calcularIndice(ana).valor();
        assertThat(radar.detectarConflitos(ana)).hasSize(2);
        mvc.perform(get("/radar").session(entrar("ana@exemplo.com")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Conflitos de horário")))
                .andExpect(content().string(containsString("Tarefas atrasadas")));

        eventos.editar(ana, b, new DadosEvento("B", amanha.plusHours(3), 60));

        assertThat(radar.calcularIndice(ana).valor()).isEqualTo(comConflito);
        assertThat(radar.detectarConflitos(ana)).isEmpty();
    }

    @Test
    @DisplayName("CT-070 a CT-074 alertas: menos de 24 h e atrasada aparecem; mais de 24 h não; concluir remove")
    void alertas() {
        Long logo = tarefa(Prioridade.MEDIA, agora.plusHours(10), null);
        tarefa(Prioridade.MEDIA, agora.plusHours(30), null);
        Long vencida = tarefa(Prioridade.ALTA, agora.minusHours(2), null);

        var alertas = notificacoes.listarAlertas(ana);
        assertThat(alertas).hasSize(2);
        assertThat(alertas.get(0).tipo()).isEqualTo(TipoAlerta.TAREFA_ATRASADA);
        assertThat(alertas.get(0).tarefa().id()).isEqualTo(vencida);
        assertThat(alertas.get(1).tarefa().id()).isEqualTo(logo);

        tarefas.concluir(ana, logo);
        assertThat(notificacoes.listarAlertas(ana)).hasSize(1);
    }

    @Test
    @DisplayName("CT-077 e CT-078 o Dashboard mostra Índice, alertas, tarefa de hoje e Áreas sem navegar")
    void dashboard() throws Exception {
        tarefas.criar(ana, faculdade, new DadosTarefa("Entrega de hoje", null,
                agora.toLocalDate().atTime(23, 59), Prioridade.ALTA, null));
        tarefa(Prioridade.ALTA, agora.minusDays(1), null);
        mvc.perform(get("/").session(entrar("ana@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Índice de Carga")))
                .andExpect(content().string(containsString("Alertas")))
                .andExpect(content().string(containsString("Entrega de hoje")))
                .andExpect(content().string(containsString("Faculdade")))
                .andExpect(content().string(containsString("Atrasada")));
    }

    @Test
    @DisplayName("CT-079 sem Áreas, o Dashboard orienta a criar a primeira")
    void estadoVazio() throws Exception {
        mvc.perform(get("/").session(entrar("bruno@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Criar primeira Área")))
                .andExpect(content().string(not(containsString("Índice de Carga"))));
    }

    @Test
    @DisplayName("CT-068 linguagem não clínica: a tela do Radar declara que não é avaliação médica, psicológica ou clínica")
    void linguagemNaoClinica() throws Exception {
        mvc.perform(get("/radar").session(entrar("ana@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("não é uma avaliação médica, psicológica ou clínica")))
                .andExpect(content().string(not(containsString("burnout"))))
                .andExpect(content().string(not(containsString("estresse"))))
                .andExpect(content().string(not(containsString("diagnóstico"))));
    }
}
