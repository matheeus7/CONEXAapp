package br.conexa.web;

import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.areas.porta.NomeDeAreaInvalidoException;
import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.tarefas.porta.TarefaPort;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Minha Vida e Áreas (US04, US05). Erros de acesso e de "não encontrada" são tratados em {@link ErrosWeb}. */
@Controller
public class AreaController {

    private final AreaPort areas;
    private final TarefaPort tarefas;

    public AreaController(AreaPort areas, TarefaPort tarefas) {
        this.areas = areas;
        this.tarefas = tarefas;
    }

    @GetMapping("/areas")
    public String minhaVida(Model modelo) {
        modelo.addAttribute("areas", areas.listar(idDe(modelo)));
        return "areas";
    }

    @GetMapping("/areas/nova")
    public String nova(Model modelo) {
        modelo.addAttribute("areaForm", new AreaForm());
        return "area-form";
    }

    @PostMapping("/areas")
    public String criar(@Valid AreaForm form, BindingResult resultado, Model modelo, RedirectAttributes redirecionamento) {
        if (resultado.hasErrors()) {
            return "area-form";
        }
        try {
            AreaResumo area = areas.criar(idDe(modelo), form.getNome());
            redirecionamento.addFlashAttribute("aviso", "Área criada.");
            return "redirect:/areas/" + area.id();
        } catch (NomeDeAreaInvalidoException e) {
            resultado.rejectValue("nome", "nome.invalido", e.getMessage());
            return "area-form";
        }
    }

    @GetMapping("/areas/{id}")
    public String detalhe(@PathVariable Long id, Model modelo) {
        Long pessoaId = idDe(modelo);
        modelo.addAttribute("area", areas.buscar(pessoaId, id));
        modelo.addAttribute("tarefas", tarefas.listarDaArea(pessoaId, id));
        return "area";
    }

    @GetMapping("/areas/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        AreaResumo area = areas.buscar(idDe(modelo), id);
        AreaForm form = new AreaForm();
        form.setNome(area.nome());
        modelo.addAttribute("area", area);
        modelo.addAttribute("areaForm", form);
        return "area-form";
    }

    @PostMapping("/areas/{id}")
    public String renomear(@PathVariable Long id, @Valid AreaForm form, BindingResult resultado, Model modelo,
                           RedirectAttributes redirecionamento) {
        Long pessoaId = idDe(modelo);
        if (resultado.hasErrors()) {
            modelo.addAttribute("area", areas.buscar(pessoaId, id));
            return "area-form";
        }
        try {
            areas.renomear(pessoaId, id, form.getNome());
            redirecionamento.addFlashAttribute("aviso", "Área renomeada.");
            return "redirect:/areas/" + id;
        } catch (NomeDeAreaInvalidoException e) {
            resultado.rejectValue("nome", "nome.invalido", e.getMessage());
            modelo.addAttribute("area", areas.buscar(pessoaId, id));
            return "area-form";
        }
    }

    @GetMapping("/areas/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model modelo) {
        Long pessoaId = idDe(modelo);
        modelo.addAttribute("area", areas.buscar(pessoaId, id));
        modelo.addAttribute("qtdTarefas", tarefas.listarDaArea(pessoaId, id).size());
        return "area-excluir";
    }

    @PostMapping("/areas/{id}/excluir")
    public String excluir(@PathVariable Long id, Model modelo, RedirectAttributes redirecionamento) {
        areas.excluir(idDe(modelo), id);
        redirecionamento.addFlashAttribute("aviso", "Área excluída.");
        return "redirect:/areas";
    }

    private static Long idDe(Model modelo) {
        return ((PessoaResumo) modelo.getAttribute("pessoa")).id();
    }
}
