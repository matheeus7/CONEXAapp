package br.conexa.web;

import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.CadastroPessoa;
import br.conexa.autenticacao.porta.EmailJaCadastradoException;
import br.conexa.autenticacao.porta.PessoaResumo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AutenticacaoPort autenticacao;
    private final HttpSessionSecurityContextRepository repositorioDeContexto = new HttpSessionSecurityContextRepository();

    public AuthController(AutenticacaoPort autenticacao) {
        this.autenticacao = autenticacao;
    }

    @GetMapping("/entrar")
    public String entrar(Authentication autenticada) {
        return estaLogada(autenticada) ? "redirect:/" : "entrar";
    }

    @GetMapping("/cadastro")
    public String formularioCadastro(Authentication autenticada, Model modelo) {
        if (estaLogada(autenticada)) {
            return "redirect:/";
        }
        modelo.addAttribute("cadastroForm", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid CadastroForm form, BindingResult resultado,
                            HttpServletRequest requisicao, HttpServletResponse resposta,
                            RedirectAttributes redirecionamento) {
        if (!resultado.hasErrors()) {
            try {
                PessoaResumo pessoa = autenticacao.cadastrar(
                        new CadastroPessoa(form.getNome(), form.getEmail(), form.getSenha()));
                iniciarSessao(pessoa, requisicao, resposta);
                redirecionamento.addFlashAttribute("aviso", "Conta criada. Sua Minha Vida já está pronta.");
                return "redirect:/";
            } catch (EmailJaCadastradoException e) {
                resultado.rejectValue("email", "email.duplicado", e.getMessage());
            }
        }
        form.setSenha(null); // nunca devolve a senha ao navegador
        return "cadastro";
    }

    /** Autentica a Pessoa recém-cadastrada (RF01) trocando o identificador de sessão. */
    private void iniciarSessao(PessoaResumo pessoa, HttpServletRequest requisicao, HttpServletResponse resposta) {
        if (requisicao.getSession(false) != null) {
            requisicao.changeSessionId();
        }
        Authentication autenticacao = UsernamePasswordAuthenticationToken.authenticated(
                pessoa.email(), null, List.of(new SimpleGrantedAuthority("ROLE_PESSOA")));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        repositorioDeContexto.saveContext(contexto, requisicao, resposta);
    }

    private static boolean estaLogada(Authentication a) {
        return a != null && a.isAuthenticated() && !"anonymousUser".equals(a.getPrincipal());
    }
}
