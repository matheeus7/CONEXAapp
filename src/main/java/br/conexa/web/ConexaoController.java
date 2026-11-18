package br.conexa.web;

import br.conexa.areas.porta.AreaPort;
import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.conexoes.porta.ConexaoInvalidaException;
import br.conexa.conexoes.porta.ConexaoPort;
import br.conexa.conexoes.porta.ConexaoResumo;
import br.conexa.conexoes.porta.NivelPermissao;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Conexões de uma Área (US12, US13, US14). Só o dono chega aqui; a porta recusa os demais com 403. */
@Controller
public class ConexaoController {

    private final ConexaoPort conexoes;
    private final AreaPort areas;

    public ConexaoController(ConexaoPort conexoes, AreaPort areas) {
        this.conexoes = conexoes;
        this.areas = areas;
    }

    @GetMapping("/areas/{areaId}/conexoes")
    public String listar(@PathVariable Long areaId, Model modelo) {
        modelo.addAttribute("conexaoForm", new ConexaoForm());
        return tela(areaId, modelo);
    }

    @PostMapping("/areas/{areaId}/conexoes")
    public String conectar(@PathVariable Long areaId, @Valid ConexaoForm form, BindingResult resultado, Model modelo,
                           RedirectAttributes redirecionamento) {
        if (resultado.hasErrors()) {
            return tela(areaId, modelo);
        }
        try {
            ConexaoResumo nova = conexoes.conectar(idDe(modelo), areaId, form.getEmail(), form.getNivel());
            redirecionamento.addFlashAttribute("aviso", nova.nome() + " agora está conectada a esta Área.");
            return "redirect:/areas/" + areaId + "/conexoes";
        } catch (ConexaoInvalidaException e) {
            resultado.rejectValue("email", "conexao.invalida", e.getMessage());
            return tela(areaId, modelo);
        }
    }

    @PostMapping("/conexoes/{id}/nivel")
    public String alterarNivel(@PathVariable Long id, @RequestParam NivelPermissao nivel, Model modelo,
                               RedirectAttributes redirecionamento) {
        ConexaoResumo alterada = conexoes.alterarNivel(idDe(modelo), id, nivel);
        redirecionamento.addFlashAttribute("aviso", "Nível de " + alterada.nome() + " alterado para "
                + alterada.nivel().getRotulo() + ".");
        return "redirect:/areas/" + alterada.areaId() + "/conexoes";
    }

    @PostMapping("/conexoes/{id}/revogar")
    public String revogar(@PathVariable Long id, Model modelo, RedirectAttributes redirecionamento) {
        Long donoId = idDe(modelo);
        ConexaoResumo conexao = conexoes.buscar(donoId, id);
        conexoes.revogar(donoId, id);
        redirecionamento.addFlashAttribute("aviso", "Acesso de " + conexao.nome() + " revogado.");
        return "redirect:/areas/" + conexao.areaId() + "/conexoes";
    }

    private String tela(Long areaId, Model modelo) {
        Long donoId = idDe(modelo);
        modelo.addAttribute("area", areas.buscar(donoId, areaId));
        modelo.addAttribute("conexoes", conexoes.listar(donoId, areaId));
        modelo.addAttribute("niveis", NivelPermissao.values());
        return "conexoes";
    }

    private static Long idDe(Model modelo) {
        return ((PessoaResumo) modelo.getAttribute("pessoa")).id();
    }
}
