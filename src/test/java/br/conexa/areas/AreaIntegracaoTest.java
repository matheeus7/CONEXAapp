package br.conexa.areas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.conexa.areas.porta.AreaPort;
import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.CadastroPessoa;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** CT-012 a CT-017: criar, renomear, excluir e isolamento entre pessoas, pela interface web. */
@SpringBootTest
@ActiveProfiles("test")
class AreaIntegracaoTest {

    private static final String SENHA = "senha-forte-1";

    @Autowired WebApplicationContext contexto;
    @Autowired AutenticacaoPort autenticacao;
    @Autowired AreaPort areas;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;
    Long ana;
    Long bruno;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        jdbc.update("delete from tarefa");
        jdbc.update("delete from area");
        jdbc.update("delete from minha_vida");
        jdbc.update("delete from pessoa");
        ana = autenticacao.cadastrar(new CadastroPessoa("Ana Souza", "ana@exemplo.com", SENHA)).id();
        bruno = autenticacao.cadastrar(new CadastroPessoa("Bruno Lima", "bruno@exemplo.com", SENHA)).id();
    }

    private MockHttpSession entrarComo(String email) throws Exception {
        MvcResult r = mvc.perform(post("/entrar").with(csrf()).param("email", email).param("senha", SENHA)).andReturn();
        HttpSession sessao = r.getRequest().getSession(false);
        return (MockHttpSession) sessao;
    }

    @Test
    @DisplayName("sem login, /areas redireciona para a tela de entrada")
    void protegida() throws Exception {
        mvc.perform(get("/areas")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("CT-012 criar Área, que aparece em Minha Vida")
    void criar() throws Exception {
        MockHttpSession sessao = entrarComo("ana@exemplo.com");
        mvc.perform(post("/areas").with(csrf()).session(sessao).param("nome", "Casa"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/areas/*"));
        mvc.perform(get("/areas").session(sessao)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Casa")));
        assertThat(areas.listar(ana)).hasSize(1);
        assertThat(areas.listar(bruno)).isEmpty();
    }

    @Test
    @DisplayName("nome vazio ou repetido volta ao formulário com erro")
    void nomeInvalido() throws Exception {
        MockHttpSession sessao = entrarComo("ana@exemplo.com");
        mvc.perform(post("/areas").with(csrf()).session(sessao).param("nome", "  "))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Informe o nome")));
        areas.criar(ana, "Casa");
        mvc.perform(post("/areas").with(csrf()).session(sessao).param("nome", "casa"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("já tem uma Área")));
    }

    @Test
    @DisplayName("CT-013 renomear Área")
    void renomear() throws Exception {
        Long id = areas.criar(ana, "Casa").id();
        MockHttpSession sessao = entrarComo("ana@exemplo.com");
        mvc.perform(post("/areas/" + id).with(csrf()).session(sessao).param("nome", "Lar"))
                .andExpect(status().is3xxRedirection());
        assertThat(areas.buscar(ana, id).nome()).isEqualTo("Lar");
    }

    @Test
    @DisplayName("CT-014/CT-015/CT-016 excluir pede confirmação; cancelar não remove; confirmar remove")
    void excluir() throws Exception {
        Long id = areas.criar(ana, "Casa").id();
        MockHttpSession sessao = entrarComo("ana@exemplo.com");
        mvc.perform(get("/areas/" + id + "/excluir").session(sessao)).andExpect(status().isOk());
        assertThat(areas.listar(ana)).hasSize(1);
        mvc.perform(post("/areas/" + id + "/excluir").with(csrf()).session(sessao)).andExpect(status().is3xxRedirection());
        assertThat(areas.listar(ana)).isEmpty();
    }

    @Test
    @DisplayName("CT-017 outra pessoa recebe 403 ao ver, renomear ou excluir")
    void naoDono() throws Exception {
        Long id = areas.criar(ana, "Faculdade").id();
        MockHttpSession sessao = entrarComo("bruno@exemplo.com");
        mvc.perform(get("/areas/" + id).session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/areas/" + id).with(csrf()).session(sessao).param("nome", "X")).andExpect(status().isForbidden());
        mvc.perform(post("/areas/" + id + "/excluir").with(csrf()).session(sessao)).andExpect(status().isForbidden());
        assertThat(areas.buscar(ana, id).nome()).isEqualTo("Faculdade");
    }

    @Test
    @DisplayName("Área inexistente responde 404")
    void inexistente() throws Exception {
        MockHttpSession sessao = entrarComo("ana@exemplo.com");
        mvc.perform(get("/areas/999999").session(sessao)).andExpect(status().isNotFound());
    }
}
