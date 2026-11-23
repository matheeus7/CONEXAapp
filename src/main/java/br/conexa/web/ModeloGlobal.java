package br.conexa.web;

import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.PessoaResumo;
import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Disponibiliza a Pessoa autenticada para todas as páginas (nome na navegação, saudação, etc.). */
@ControllerAdvice
public class ModeloGlobal {

    private final AutenticacaoPort autenticacao;

    public ModeloGlobal(AutenticacaoPort autenticacao) {
        this.autenticacao = autenticacao;
    }

    @ModelAttribute("pessoa")
    public PessoaResumo pessoa(Principal principal) {
        if (principal == null) {
            return null;
        }
        return autenticacao.buscarPorEmail(principal.getName()).orElse(null);
    }
}
