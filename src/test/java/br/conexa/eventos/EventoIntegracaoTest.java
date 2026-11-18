package br.conexa.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.conexa.areas.porta.AreaPort;
import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.CadastroPessoa;
import br.conexa.eventos.porta.DadosEvento;
import br.conexa.eventos.porta.EventoPort;
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

/** CT-026 a CT-032 e a cascata da exclusão da Área, de ponta a ponta com banco. */
@SpringBootTest
@ActiveProfiles("test")
class EventoIntegracaoTest {

    private static final String SENHA = "senha-forte-1";
    private static final LocalDateTime DIA = LocalDateTime.of(2030, 3, 12, 0, 0);

    @Autowired WebApplicationContext contexto;
    @Autowired AutenticacaoPort autenticacao;
    @Autowired AreaPort areas;
    @Autowired EventoPort eventos;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;
    Long ana;
    Long faculdade;
    Long casa;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        jdbc.update("delete from conexao");
        jdbc.update("delete from evento");
        jdbc.update("delete from tarefa");
        jdbc.update("delete from area");
        jdbc.update("delete from minha_vida");
        jdbc.update("delete from pessoa");
        ana = autenticacao.cadastrar(new CadastroPessoa("Ana Souza", "ana@exemplo.com", SENHA)).id();
        faculdade = areas.criar(ana, "Faculdade").id();
        casa = areas.criar(ana, "Casa").id();
    }

    private MockHttpSession entrar(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/entrar").with(csrf()).param("email", email).param("senha", SENHA))
                .andReturn().getRequest().getSession(false);
    }

    private DadosEvento dados(String titulo, int hora, int minuto, int duracao) {
        return new DadosEvento(titulo, DIA.withHour(hora).withMinute(minuto), duracao);
    }

    @Test
    @DisplayName("CT-026 e CT-032 criar pela tela: o evento aparece no calendário do mês")
    void criarPelaTela() throws Exception {
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/eventos").with(csrf()).session(sessao).param("areaId", faculdade.toString())
                        .param("titulo", "Prova de Cálculo").param("inicio", "2030-03-12T14:00").param("duracaoMinutos", "90"))
                .andExpect(status().is3xxRedirection());
        assertThat(eventos.listarDaArea(ana, faculdade)).hasSize(1);
        mvc.perform(get("/calendario").param("mes", "2030-03").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Prova de Cálculo")))
                .andExpect(content().string(containsString("data-dia=\"2030-03-12\"")));
    }

    @Test
    @DisplayName("CT-027 sem Área, a tela recusa e nada é criado")
    void semArea() throws Exception {
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/eventos").with(csrf()).session(sessao).param("areaId", "")
                        .param("titulo", "Sem área").param("inicio", "2030-03-12T14:00").param("duracaoMinutos", "60"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Escolha a Área")));
        assertThat(jdbc.queryForObject("select count(*) from evento", Integer.class)).isZero();
    }

    @Test
    @DisplayName("CT-028 editar e excluir pela tela")
    void editarEExcluir() throws Exception {
        Long id = eventos.criar(ana, faculdade, dados("Reunião", 9, 0, 60)).id();
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/eventos/" + id).with(csrf()).session(sessao).param("areaId", faculdade.toString())
                        .param("titulo", "Reunião de grupo").param("inicio", "2030-03-12T10:00").param("duracaoMinutos", "45"))
                .andExpect(status().is3xxRedirection());
        var editado = eventos.buscar(ana, id);
        assertThat(editado.titulo()).isEqualTo("Reunião de grupo");
        assertThat(editado.duracaoMinutos()).isEqualTo(45);

        mvc.perform(post("/eventos/" + id + "/excluir").with(csrf()).session(sessao)).andExpect(status().is3xxRedirection());
        assertThat(eventos.listarDaArea(ana, faculdade)).isEmpty();
    }

    @Test
    @DisplayName("CT-029 e CT-030 conflito entre Áreas: ambos sinalizados no calendário")
    void conflito() throws Exception {
        eventos.criar(ana, faculdade, dados("Aula", 10, 0, 60));
        eventos.criar(ana, casa, dados("Dentista", 10, 30, 60));
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(get("/calendario").param("mes", "2030-03").session(sessao))
                .andExpect(content().string(containsString("Conflito de horário")));
        assertThat(eventos.listarDaArea(ana, faculdade)).allMatch(e -> e.emConflito());
        assertThat(eventos.listarDaArea(ana, casa)).allMatch(e -> e.emConflito());
    }

    @Test
    @DisplayName("CT-031 eventos que só se tocam não geram conflito")
    void seTocam() throws Exception {
        eventos.criar(ana, faculdade, dados("Aula", 10, 0, 60));
        eventos.criar(ana, casa, dados("Almoço", 11, 0, 60));
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(get("/calendario").param("mes", "2030-03").session(sessao))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Conflito de horário"))));
    }

    @Test
    @DisplayName("excluir a Área remove os eventos dela (cascata por evento)")
    void cascata() throws Exception {
        eventos.criar(ana, faculdade, dados("Aula", 10, 0, 60));
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(get("/areas/" + faculdade + "/excluir").session(sessao))
                .andExpect(status().isOk()).andExpect(content().string(containsString("evento")));
        mvc.perform(post("/areas/" + faculdade + "/excluir").with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("select count(*) from evento", Integer.class)).isZero();
    }

    @Test
    @DisplayName("outra pessoa não vê nem altera eventos da Área de Ana")
    void isolamento() throws Exception {
        Long id = eventos.criar(ana, faculdade, dados("Privado", 10, 0, 60)).id();
        autenticacao.cadastrar(new CadastroPessoa("Bruno Lima", "bruno@exemplo.com", SENHA));
        MockHttpSession sessao = entrar("bruno@exemplo.com");
        mvc.perform(get("/eventos/" + id + "/editar").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/eventos/" + id + "/excluir").with(csrf()).session(sessao)).andExpect(status().isForbidden());
        mvc.perform(get("/calendario").param("mes", "2030-03").session(sessao))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Privado"))));
        assertThat(eventos.buscar(ana, id).titulo()).isEqualTo("Privado");
    }
}
