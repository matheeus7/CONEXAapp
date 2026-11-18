package br.conexa.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.conexa.areas.porta.MinhaVidaPort;
import br.conexa.autenticacao.nucleo.Pessoa;
import br.conexa.autenticacao.nucleo.PessoaRepositorio;
import br.conexa.autenticacao.porta.AutenticacaoPort;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Casos de teste CT-001 a CT-011 e CT-081 da Etapa 1 (cadastro, autenticação e Minha Vida). */
@SpringBootTest
@ActiveProfiles("test")
class AutenticacaoIntegracaoTest {

    private static final String SENHA = "senha-forte-1";

    @Autowired WebApplicationContext contexto;
    @Autowired PessoaRepositorio pessoas;
    @Autowired AutenticacaoPort autenticacao;
    @Autowired MinhaVidaPort minhaVida;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        jdbc.update("delete from conexao");
        jdbc.update("delete from evento");
        jdbc.update("delete from tarefa");
        jdbc.update("delete from area");
        jdbc.update("delete from minha_vida");
        jdbc.update("delete from pessoa");
    }

    private org.springframework.test.web.servlet.ResultActions cadastrar(String nome, String email, String senha) throws Exception {
        return mvc.perform(post("/cadastro").with(csrf())
                .param("nome", nome).param("email", email).param("senha", senha));
    }

    private MockHttpSession entrarComo(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(post("/entrar").with(csrf()).param("email", email).param("senha", senha)).andReturn();
        HttpSession sessao = r.getRequest().getSession(false);
        return sessao == null ? null : (MockHttpSession) sessao;
    }

    @Test
    @DisplayName("CT-001 cadastro válido cria a conta e autentica a Pessoa")
    void ct001() throws Exception {
        MvcResult r = cadastrar("Ana Souza", "ana@exemplo.com", SENHA)
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/")).andReturn();
        assertThat(pessoas.existsByEmail("ana@exemplo.com")).isTrue();
        MockHttpSession sessao = (MockHttpSession) r.getRequest().getSession(false);
        mvc.perform(get("/").session(sessao)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ana")));
    }

    @Test
    @DisplayName("CT-002 e-mail duplicado é recusado e nenhuma conta nova é criada")
    void ct002() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", SENHA);
        cadastrar("Outra Ana", "ANA@exemplo.com ", SENHA)
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Este e-mail já está em uso.")));
        assertThat(pessoas.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("CT-003 senha com 7 caracteres é recusada")
    void ct003() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", "1234567")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "senha"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("A senha deve ter de 8 a 72 caracteres.")));
        assertThat(pessoas.count()).isZero();
    }

    @Test
    @DisplayName("CT-004 e-mail em formato inválido é recusado")
    void ct004() throws Exception {
        cadastrar("Ana", "ana@", SENHA)
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "email"));
        assertThat(pessoas.count()).isZero();
    }

    @Test
    @DisplayName("CT-005 formulário em branco mostra erro em cada campo")
    void ct005() throws Exception {
        cadastrar("", "", "")
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "nome", "email", "senha"));
        assertThat(pessoas.count()).isZero();
    }

    @Test
    @DisplayName("CT-006 login correto leva ao Dashboard")
    void ct006() throws Exception {
        autenticacao.cadastrar(new br.conexa.autenticacao.porta.CadastroPessoa("Ana", "ana@exemplo.com", SENHA));
        mvc.perform(post("/entrar").with(csrf()).param("email", "ana@exemplo.com").param("senha", SENHA))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("CT-007 senha errada mostra mensagem genérica")
    void ct007() throws Exception {
        autenticacao.cadastrar(new br.conexa.autenticacao.porta.CadastroPessoa("Ana", "ana@exemplo.com", SENHA));
        mvc.perform(post("/entrar").with(csrf()).param("email", "ana@exemplo.com").param("senha", "errada-123"))
                .andExpect(redirectedUrl("/entrar?erro"));
        mvc.perform(get("/entrar").param("erro", ""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("E-mail ou senha incorretos.")));
    }

    @Test
    @DisplayName("CT-008 e-mail inexistente recebe o mesmo resultado do CT-007")
    void ct008() throws Exception {
        mvc.perform(post("/entrar").with(csrf()).param("email", "ninguem@exemplo.com").param("senha", SENHA))
                .andExpect(redirectedUrl("/entrar?erro"));
    }

    @Test
    @DisplayName("CT-009 após o logout, páginas internas exigem login")
    void ct009() throws Exception {
        autenticacao.cadastrar(new br.conexa.autenticacao.porta.CadastroPessoa("Ana", "ana@exemplo.com", SENHA));
        MockHttpSession sessao = entrarComo("ana@exemplo.com", SENHA);
        mvc.perform(get("/").session(sessao)).andExpect(status().isOk());
        mvc.perform(post("/sair").with(csrf()).session(sessao)).andExpect(redirectedUrl("/entrar?saiu"));
        mvc.perform(get("/").session(sessao)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/entrar"));
    }

    @Test
    @DisplayName("CT-010 acesso sem login é redirecionado ao login")
    void ct010() throws Exception {
        for (String url : new String[] {"/", "/conta"}) {
            mvc.perform(get(url)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost/entrar"));
        }
    }

    @Test
    @DisplayName("CT-011 a Minha Vida existe logo após o cadastro")
    void ct011() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", SENHA);
        Pessoa ana = pessoas.findByEmail("ana@exemplo.com").orElseThrow();
        assertThat(minhaVida.existePara(ana.getId())).isTrue();
    }

    @Test
    @DisplayName("CT-081 a senha é guardada com hash BCrypt, nunca em texto puro")
    void ct081() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", SENHA);
        String guardado = jdbc.queryForObject("select senha_hash from pessoa where email = 'ana@exemplo.com'", String.class);
        assertThat(guardado).startsWith("$2").isNotEqualTo(SENHA).doesNotContain(SENHA);
    }

    @Test
    @DisplayName("Requisição sem token CSRF é recusada")
    void semCsrf() throws Exception {
        mvc.perform(post("/cadastro").param("nome", "Ana").param("email", "a@b.com").param("senha", SENHA))
                .andExpect(status().isForbidden());
        assertThat(pessoas.count()).isZero();
    }

    @Test
    @DisplayName("Renomear atualiza o nome exibido")
    void renomear() throws Exception {
        MvcResult r = cadastrar("Ana", "ana@exemplo.com", SENHA).andReturn();
        MockHttpSession sessao = (MockHttpSession) r.getRequest().getSession(false);
        mvc.perform(post("/conta").with(csrf()).session(sessao).param("nome", "Ana Souza"))
                .andExpect(redirectedUrl("/conta"));
        mvc.perform(get("/conta").session(sessao))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ana Souza")));
        mvc.perform(post("/conta").with(csrf()).session(sessao).param("nome", " "))
                .andExpect(model().attributeHasFieldErrors("contaForm", "nome"));
    }

    @Test
    @DisplayName("Páginas públicas abrem sem login")
    void publicas() throws Exception {
        mvc.perform(get("/entrar")).andExpect(status().isOk());
        mvc.perform(get("/cadastro")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/css/estilo.css")).andExpect(status().isOk());
    }
}
