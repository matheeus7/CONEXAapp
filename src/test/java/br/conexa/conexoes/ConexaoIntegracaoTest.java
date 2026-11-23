package br.conexa.conexoes;

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
import br.conexa.conexoes.porta.ConexaoResumo;
import br.conexa.conexoes.porta.NivelPermissao;
import br.conexa.eventos.porta.EventoPort;
import br.conexa.tarefas.porta.DadosTarefa;
import br.conexa.tarefas.porta.Prioridade;
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

/** CT-033 a CT-054: conexões, níveis de acesso, isolamento, responsável e revogação, de ponta a ponta. */
@SpringBootTest
@ActiveProfiles("test")
class ConexaoIntegracaoTest {

    private static final String SENHA = "senha-forte-1";

    @Autowired WebApplicationContext contexto;
    @Autowired AutenticacaoPort autenticacao;
    @Autowired AreaPort areas;
    @Autowired ConexaoPort conexoes;
    @Autowired TarefaPort tarefas;
    @Autowired EventoPort eventos;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;
    Long ana, bruno, carla, diogo;
    Long faculdade, casa;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        for (String t : new String[]{"conexao", "evento", "tarefa", "area", "minha_vida", "pessoa"}) {
            jdbc.update("delete from " + t);
        }
        ana = cadastrar("Ana Souza", "ana@exemplo.com");
        bruno = cadastrar("Bruno Lima", "bruno@exemplo.com");
        carla = cadastrar("Carla Dias", "carla@exemplo.com");
        diogo = cadastrar("Diogo Reis", "diogo@exemplo.com");
        faculdade = areas.criar(ana, "Faculdade").id();
        casa = areas.criar(ana, "Casa").id();
        conexoes.conectar(ana, faculdade, "bruno@exemplo.com", NivelPermissao.COLABORADOR);
        conexoes.conectar(ana, faculdade, "carla@exemplo.com", NivelPermissao.VISUALIZADOR);
    }

    private Long cadastrar(String nome, String email) {
        return autenticacao.cadastrar(new CadastroPessoa(nome, email, SENHA)).id();
    }

    private MockHttpSession entrar(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/entrar").with(csrf()).param("email", email).param("senha", SENHA))
                .andReturn().getRequest().getSession(false);
    }

    private DadosTarefa tarefa(String titulo, Long responsavel) {
        return new DadosTarefa(titulo, null, LocalDateTime.now().plusDays(2), Prioridade.MEDIA, responsavel);
    }

    private Long conexaoDe(Long pessoaId) {
        return conexoes.listar(ana, faculdade).stream().filter(c -> c.pessoaId().equals(pessoaId))
                .map(ConexaoResumo::id).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("CT-037 e CT-043 Ana conecta pela tela e vê a lista com os níveis")
    void conectarELista() throws Exception {
        conexoes.revogar(ana, conexaoDe(bruno));
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/areas/" + faculdade + "/conexoes").with(csrf()).session(sessao)
                        .param("email", "bruno@exemplo.com").param("nivel", "COLABORADOR"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/areas/" + faculdade + "/conexoes").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bruno Lima")))
                .andExpect(content().string(containsString("Carla Dias")))
                .andExpect(content().string(containsString("Colaborador")))
                .andExpect(content().string(containsString("Visualizador")));
    }

    @Test
    @DisplayName("CT-039, CT-040 e CT-041 e-mail sem conta, auto-conexão e duplicada são recusados")
    void recusas() throws Exception {
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/areas/" + faculdade + "/conexoes").with(csrf()).session(sessao)
                        .param("email", "x@x.com").param("nivel", "VISUALIZADOR"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Não encontramos uma conta")));
        mvc.perform(post("/areas/" + faculdade + "/conexoes").with(csrf()).session(sessao)
                        .param("email", "ana@exemplo.com").param("nivel", "VISUALIZADOR"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("já é o dono")));
        mvc.perform(post("/areas/" + faculdade + "/conexoes").with(csrf()).session(sessao)
                        .param("email", "bruno@exemplo.com").param("nivel", "VISUALIZADOR"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("já está conectada")));
        assertThat(conexoes.listar(ana, faculdade)).hasSize(2);
    }

    @Test
    @DisplayName("CT-038 e CT-042 Bruno vê Faculdade em Compartilhadas comigo e nunca vê Casa")
    void isolamento() throws Exception {
        MockHttpSession sessao = entrar("bruno@exemplo.com");
        mvc.perform(get("/areas").session(sessao))
                .andExpect(content().string(containsString("/areas/" + faculdade)))
                .andExpect(content().string(containsString("de Ana Souza")))
                .andExpect(content().string(not(containsString("/areas/" + casa))));
        mvc.perform(get("/areas/" + faculdade).session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/areas/" + casa).session(sessao)).andExpect(status().isForbidden());
        mvc.perform(get("/areas/" + casa + "/tarefas/nova").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(get("/calendario").session(sessao)).andExpect(status().isOk());
        MockHttpSession deDiogo = entrar("diogo@exemplo.com");
        mvc.perform(get("/areas/" + faculdade).session(deDiogo)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CT-044 e CT-048 Colaborador não gerencia conexões nem a Área")
    void colaboradorSemGestao() throws Exception {
        MockHttpSession sessao = entrar("bruno@exemplo.com");
        mvc.perform(get("/areas/" + faculdade + "/conexoes").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/areas/" + faculdade + "/conexoes").with(csrf()).session(sessao)
                .param("email", "diogo@exemplo.com").param("nivel", "VISUALIZADOR")).andExpect(status().isForbidden());
        mvc.perform(post("/conexoes/" + conexaoDe(carla) + "/revogar").with(csrf()).session(sessao))
                .andExpect(status().isForbidden());
        mvc.perform(post("/areas/" + faculdade).with(csrf()).session(sessao).param("nome", "Outro"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/areas/" + faculdade + "/excluir").with(csrf()).session(sessao)).andExpect(status().isForbidden());
        assertThat(areas.buscar(ana, faculdade).nome()).isEqualTo("Faculdade");
    }

    @Test
    @DisplayName("CT-045 e CT-046 Visualizador lê, mas todas as escritas são recusadas")
    void visualizador() throws Exception {
        Long t = tarefas.criar(ana, faculdade, tarefa("Entregar", null)).id();
        Long e = eventos.criar(ana, faculdade, new br.conexa.eventos.porta.DadosEvento("Aula",
                LocalDateTime.now().plusDays(1), 60)).id();
        MockHttpSession sessao = entrar("carla@exemplo.com");
        mvc.perform(get("/areas/" + faculdade).session(sessao))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Entregar")))
                .andExpect(content().string(containsString("Aula")));
        mvc.perform(post("/areas/" + faculdade + "/tarefas").with(csrf()).session(sessao)
                .param("titulo", "X").param("prazo", "2030-01-01T10:00").param("prioridade", "MEDIA"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/tarefas/" + t).with(csrf()).session(sessao)
                .param("titulo", "Y").param("prazo", "2030-01-01T10:00").param("prioridade", "MEDIA"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/tarefas/" + t + "/concluir").with(csrf()).session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/tarefas/" + t + "/excluir").with(csrf()).session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/eventos").with(csrf()).session(sessao).param("areaId", faculdade.toString())
                .param("titulo", "X").param("inicio", "2030-01-01T10:00").param("duracaoMinutos", "60"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/eventos/" + e + "/excluir").with(csrf()).session(sessao)).andExpect(status().isForbidden());
        assertThat(tarefas.listarDaArea(ana, faculdade)).hasSize(1);
        assertThat(eventos.listarDaArea(ana, faculdade)).hasSize(1);
    }

    @Test
    @DisplayName("CT-047 e CT-049 Colaborador cria, edita, conclui e exclui; a autoria é registrada")
    void colaborador() throws Exception {
        MockHttpSession sessao = entrar("bruno@exemplo.com");
        mvc.perform(post("/areas/" + faculdade + "/tarefas").with(csrf()).session(sessao)
                        .param("titulo", "Do Bruno").param("prazo", "2030-01-01T10:00").param("prioridade", "ALTA")
                        .param("responsavelId", bruno.toString()))
                .andExpect(status().is3xxRedirection());
        Long id = tarefas.listarDaArea(ana, faculdade).get(0).id();
        assertThat(tarefas.buscar(ana, id).responsavelId()).isEqualTo(bruno);

        mvc.perform(post("/tarefas/" + id).with(csrf()).session(sessao)
                        .param("titulo", "Editada").param("prazo", "2030-02-01T10:00").param("prioridade", "BAIXA")
                        .param("responsavelId", bruno.toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("select atualizada_por from tarefa where id = ?", Long.class, id)).isEqualTo(bruno);

        mvc.perform(post("/tarefas/" + id + "/concluir").with(csrf()).session(sessao)).andExpect(status().is3xxRedirection());
        assertThat(tarefas.buscar(ana, id).concluida()).isTrue();
        mvc.perform(post("/tarefas/" + id + "/excluir").with(csrf()).session(sessao)).andExpect(status().is3xxRedirection());
        assertThat(tarefas.listarDaArea(ana, faculdade)).isEmpty();

        mvc.perform(post("/eventos").with(csrf()).session(sessao).param("areaId", faculdade.toString())
                        .param("titulo", "Reunião").param("inicio", "2030-01-01T10:00").param("duracaoMinutos", "60"))
                .andExpect(status().is3xxRedirection());
        assertThat(eventos.listarDaArea(ana, faculdade)).hasSize(1);
    }

    @Test
    @DisplayName("CT-033 a CT-036 responsável: Ana e Bruno aparecem, Carla não; Visualizador forçado é recusado")
    void responsavel() throws Exception {
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(get("/areas/" + faculdade + "/tarefas/nova").session(sessao))
                .andExpect(content().string(containsString("Bruno Lima")))
                .andExpect(content().string(not(containsString("Carla Dias"))));
        mvc.perform(post("/areas/" + faculdade + "/tarefas").with(csrf()).session(sessao)
                        .param("titulo", "Atribuída").param("prazo", "2030-01-01T10:00").param("prioridade", "MEDIA")
                        .param("responsavelId", bruno.toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(tarefas.listarDaArea(ana, faculdade).get(0).responsavelId()).isEqualTo(bruno);
        mvc.perform(post("/areas/" + faculdade + "/tarefas").with(csrf()).session(sessao)
                        .param("titulo", "Forçada").param("prazo", "2030-01-01T10:00").param("prioridade", "MEDIA")
                        .param("responsavelId", carla.toString()))
                .andExpect(status().isForbidden());
        assertThat(tarefas.listarDaArea(ana, faculdade)).hasSize(1);
        mvc.perform(get("/areas/" + casa + "/tarefas/nova").session(sessao))
                .andExpect(content().string(not(containsString("Bruno Lima"))));
    }

    @Test
    @DisplayName("CT-050 mudar o nível vale na próxima requisição")
    void mudancaDeNivel() throws Exception {
        MockHttpSession sessao = entrar("bruno@exemplo.com");
        mvc.perform(post("/areas/" + faculdade + "/tarefas").with(csrf()).session(sessao)
                .param("titulo", "Antes").param("prazo", "2030-01-01T10:00").param("prioridade", "MEDIA"))
                .andExpect(status().is3xxRedirection());
        MockHttpSession deAna = entrar("ana@exemplo.com");
        mvc.perform(post("/conexoes/" + conexaoDe(bruno) + "/nivel").with(csrf()).session(deAna).param("nivel", "VISUALIZADOR"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/areas/" + faculdade + "/tarefas").with(csrf()).session(sessao)
                .param("titulo", "Depois").param("prazo", "2030-01-01T10:00").param("prioridade", "MEDIA"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/areas/" + faculdade).session(sessao)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CT-051 a CT-054 revogar: acesso negado já na próxima requisição, tarefas voltam ao dono, reconectar não as restaura")
    void revogar() throws Exception {
        tarefas.criar(ana, faculdade, tarefa("Uma", bruno));
        tarefas.criar(ana, faculdade, tarefa("Duas", bruno));
        MockHttpSession deBruno = entrar("bruno@exemplo.com");
        mvc.perform(get("/areas/" + faculdade).session(deBruno)).andExpect(status().isOk());

        MockHttpSession deAna = entrar("ana@exemplo.com");
        mvc.perform(post("/conexoes/" + conexaoDe(bruno) + "/revogar").with(csrf()).session(deAna))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/areas/" + faculdade).session(deBruno)).andExpect(status().isForbidden());
        mvc.perform(get("/areas").session(deBruno)).andExpect(content().string(not(containsString("/areas/" + faculdade))));
        assertThat(tarefas.listarDaArea(ana, faculdade)).extracting(t -> t.responsavelId()).containsOnly(ana);

        conexoes.conectar(ana, faculdade, "bruno@exemplo.com", NivelPermissao.COLABORADOR);
        mvc.perform(get("/areas/" + faculdade).session(deBruno)).andExpect(status().isOk());
        assertThat(tarefas.listarDaArea(ana, faculdade)).extracting(t -> t.responsavelId()).containsOnly(ana);
    }

    @Test
    @DisplayName("excluir a Área remove as conexões dela")
    void cascata() throws Exception {
        MockHttpSession sessao = entrar("ana@exemplo.com");
        mvc.perform(post("/areas/" + faculdade + "/excluir").with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("select count(*) from conexao", Integer.class)).isZero();
    }
}
