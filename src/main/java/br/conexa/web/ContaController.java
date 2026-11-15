package br.conexa.web;

import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.PessoaResumo;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContaController {

    private final AutenticacaoPort autenticacao;

    public ContaController(AutenticacaoPort autenticacao) {
        this.autenticacao = autenticacao;
    }

    @GetMapping("/conta")
    public String conta(Model modelo) {
        PessoaResumo pessoa = (PessoaResumo) modelo.getAttribute("pessoa");
        ContaForm form = new ContaForm();
        form.setNome(pessoa.nome());
        modelo.addAttribute("contaForm", form);
        return "conta";
    }

    @PostMapping("/conta")
    public String salvar(@Valid ContaForm form, BindingResult resultado, Model modelo,
                         RedirectAttributes redirecionamento) {
        PessoaResumo pessoa = (PessoaResumo) modelo.getAttribute("pessoa");
        if (resultado.hasErrors()) {
            return "conta";
        }
        autenticacao.atualizarNome(pessoa.id(), form.getNome());
        redirecionamento.addFlashAttribute("aviso", "Nome atualizado.");
        return "redirect:/conta";
    }
}
