package br.conexa.tarefas;

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
import br.conexa.tarefas.porta.DadosTarefa;
import br.conexa.tarefas.porta.Prioridade;
import br.conexa.tarefas.porta.StatusTarefa;
import br.conexa.tarefas.porta.TarefaPort;
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

/** CT-015, CT-018 a CT-023: Tarefas e exclusão em cascata, de ponta a ponta com banco. */
@SpringBootTest
@ActiveProfiles("test")
class TarefaIntegracaoTest {

    private static final String SENHA = "senha-forte-1";

    @Autowired WebApplicationContext contexto;
    @Autowired AutenticacaoPort autenticacao;
    @Autowired AreaPort areas;
    @Autowired TarefaPort tarefas;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;
    Long ana;
    Long areaId;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        jdbc.update("delete from tarefa");
        jdbc.update("delete from area");
        jdbc.update("delete from minha_vida");
        jdbc.update("delete from pessoa");
        ana = autenticacao.cadastrar(new CadastroPessoa("Ana Souza", "ana@exemplo.com", SENHA)).id();
        areaId = areas.criar(ana, "Faculdade").id();
    }

    private MockHttpSession entrar(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/entrar").with(csrf()).param("email", email).param("senha", SENHA))
                .andReturn().getRequest().getSession(false);
    }

    private DadosTarefa dados(String titulo, LocalDateTime prazo) {
        return new DadosTarefa(titulo, null, prazo, Prioridade.MEDIA);
    }

    @Test
    @DisplayName("CT-018 criar tarefa pela tela: Pendente e responsável = Ana")
    void criarPelaTela() throws Exception {
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/areas/" + areaId + "/tarefas").with(csrf()).session(sessao)
                        .param("titulo", "Entregar trabalho").param("prazo", "2030-12-01T18:00").param("prioridade", "ALTA"))
                .andExpect(status().is3xxRedirection());
        var lista = tarefas.listarDaArea(ana, areaId);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).status()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(lista.get(0).responsavelId()).isEqualTo(ana);
        mvc.perform(get("/areas/" + areaId).session(sessao)).andExpect(content().string(containsString("Entregar trabalho")));
    }

    @Test
    @DisplayName("CT-019 sem prazo, a tela recusa e nada é criado")
    void semPrazo() throws Exception {
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/areas/" + areaId + "/tarefas").with(csrf()).session(sessao)
                        .param("titulo", "Sem prazo").param("prazo", "").param("prioridade", "MEDIA"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Informe o prazo")));
        assertThat(tarefas.listarDaArea(ana, areaId)).isEmpty();
    }

    @Test
    @DisplayName("CT-021 concluir pela tela tira a tarefa dos atrasos")
    void concluirPelaTela() throws Exception {
        Long id = tarefas.criar(ana, areaId, dados("Atrasada", LocalDateTime.now().minusDays(1))).id();
        assertThat(tarefas.listarAtrasadas(ana)).hasSize(1);
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/tarefas/" + id + "/concluir").with(csrf()).session(sessao)).andExpect(status().is3xxRedirection());
        assertThat(tarefas.listarAtrasadas(ana)).isEmpty();
        assertThat(tarefas.buscar(ana, id).concluida()).isTrue();
    }

    @Test
    @DisplayName("CT-023 tarefa vencida aparece como atrasada na Área e no Dashboard")
    void atrasadaNaTelaEDashboard() throws Exception {
        tarefas.criar(ana, areaId, dados("Relatório vencido", LocalDateTime.now().minusDays(1)));
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(get("/areas/" + areaId).session(sessao)).andExpect(content().string(containsString("Atrasada")));
        mvc.perform(get("/").session(sessao)).andExpect(content().string(containsString("Relatório vencido")));
    }

    @Test
    @DisplayName("CT-020 editar pela tela persiste as alterações")
    void editarPelaTela() throws Exception {
        Long id = tarefas.criar(ana, areaId, dados("Antigo", LocalDateTime.now().plusDays(1))).id();
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/tarefas/" + id).with(csrf()).session(sessao)
                        .param("titulo", "Novo").param("prazo", "2031-01-02T09:30").param("prioridade", "BAIXA"))
                .andExpect(status().is3xxRedirection());
        var t = tarefas.buscar(ana, id);
        assertThat(t.titulo()).isEqualTo("Novo");
        assertThat(t.prazo()).isEqualTo(LocalDateTime.of(2031, 1, 2, 9, 30));
        assertThat(t.prioridade()).isEqualTo(Prioridade.BAIXA);
    }

    @Test
    @DisplayName("CT-022 excluir tarefa")
    void excluirTarefa() throws Exception {
        Long id = tarefas.criar(ana, areaId, dados("Descartável", LocalDateTime.now().plusDays(1))).id();
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/tarefas/" + id + "/excluir").with(csrf()).session(sessao)).andExpect(status().is3xxRedirection());
        assertThat(tarefas.listarDaArea(ana, areaId)).isEmpty();
    }

    @Test
    @DisplayName("CT-015 excluir a Área remove as tarefas dela (cascata por evento)")
    void excluirAreaComTarefas() throws Exception {
        tarefas.criar(ana, areaId, dados("A", LocalDateTime.now().plusDays(1)));
        tarefas.criar(ana, areaId, dados("B", LocalDateTime.now().plusDays(2)));
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(get("/areas/" + areaId + "/excluir").session(sessao))
                .andExpect(status().isOk()).andExpect(content().string(containsString("tarefas")));
        mvc.perform(post("/areas/" + areaId + "/excluir").with(csrf()).session(sessao)).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("select count(*) from tarefa", Integer.class)).isZero();
        assertThat(areas.listar(ana)).isEmpty();
    }

    @Test
    @DisplayName("CT-017 outra pessoa não vê nem altera tarefas da Área de Ana")
    void isolamento() throws Exception {
        Long id = tarefas.criar(ana, areaId, dados("Privada", LocalDateTime.now().plusDays(1))).id();
        autenticacao.cadastrar(new CadastroPessoa("Bruno Lima", "bruno@exemplo.com", SENHA));
        MockHttpSession sessao = entrar("bruno@exemplo.com");
        mvc.perform(get("/tarefas/" + id + "/editar").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/tarefas/" + id + "/concluir").with(csrf()).session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/areas/" + areaId + "/tarefas").with(csrf()).session(sessao)
                .param("titulo", "X").param("prazo", "2030-01-01T10:00").param("prioridade", "MEDIA"))
                .andExpect(status().isForbidden());
        assertThat(tarefas.buscar(ana, id).concluida()).isFalse();
    }
}
