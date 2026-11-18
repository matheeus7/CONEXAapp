package br.conexa.web;

import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.tarefas.porta.Prioridade;
import br.conexa.tarefas.porta.TarefaInvalidaException;
import br.conexa.tarefas.porta.TarefaPort;
import br.conexa.tarefas.porta.TarefaResumo;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Tarefas dentro de uma Área (US06, US07, US08). */
@Controller
public class TarefaController {

    private final TarefaPort tarefas;
    private final AreaPort areas;

    public TarefaController(TarefaPort tarefas, AreaPort areas) {
        this.tarefas = tarefas;
        this.areas = areas;
    }

    @GetMapping("/areas/{areaId}/tarefas/nova")
    public String nova(@PathVariable Long areaId, Model modelo) {
        Long pessoaId = idDe(modelo);
        TarefaForm form = new TarefaForm();
        form.setResponsavelId(pessoaId);
        modelo.addAttribute("area", areas.buscar(pessoaId, areaId));
        modelo.addAttribute("responsaveis", tarefas.responsaveisPossiveis(pessoaId, areaId));
        modelo.addAttribute("tarefaForm", form);
        modelo.addAttribute("prioridades", Prioridade.values());
        return "tarefa-form";
    }

    @PostMapping("/areas/{areaId}/tarefas")
    public String criar(@PathVariable Long areaId, @Valid TarefaForm form, BindingResult resultado, Model modelo,
                        RedirectAttributes redirecionamento) {
        Long pessoaId = idDe(modelo);
        if (resultado.hasErrors()) {
            return formComErros(areaId, pessoaId, null, modelo);
        }
        try {
            tarefas.criar(pessoaId, areaId, form.paraDados());
            redirecionamento.addFlashAttribute("aviso", "Tarefa criada.");
            return "redirect:/areas/" + areaId;
        } catch (TarefaInvalidaException e) {
            resultado.reject("tarefa.invalida", e.getMessage());
            return formComErros(areaId, pessoaId, null, modelo);
        }
    }

    @GetMapping("/tarefas/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        TarefaResumo tarefa = tarefas.buscar(idDe(modelo), id);
        TarefaForm form = new TarefaForm();
        form.setTitulo(tarefa.titulo());
        form.setDescricao(tarefa.descricao());
        form.setPrazo(tarefa.prazo());
        form.setPrioridade(tarefa.prioridade());
        form.setResponsavelId(tarefa.responsavelId());
        modelo.addAttribute("responsaveis", tarefas.responsaveisPossiveis(idDe(modelo), tarefa.areaId()));
        modelo.addAttribute("area", areas.buscar(idDe(modelo), tarefa.areaId()));
        modelo.addAttribute("tarefa", tarefa);
        modelo.addAttribute("tarefaForm", form);
        modelo.addAttribute("prioridades", Prioridade.values());
        return "tarefa-form";
    }

    @PostMapping("/tarefas/{id}")
    public String salvar(@PathVariable Long id, @Valid TarefaForm form, BindingResult resultado, Model modelo,
                         RedirectAttributes redirecionamento) {
        Long pessoaId = idDe(modelo);
        TarefaResumo atual = tarefas.buscar(pessoaId, id);
        if (resultado.hasErrors()) {
            return formComErros(atual.areaId(), pessoaId, atual, modelo);
        }
        try {
            tarefas.editar(pessoaId, id, form.paraDados());
            redirecionamento.addFlashAttribute("aviso", "Tarefa atualizada.");
            return "redirect:/areas/" + atual.areaId();
        } catch (TarefaInvalidaException e) {
            resultado.reject("tarefa.invalida", e.getMessage());
            return formComErros(atual.areaId(), pessoaId, atual, modelo);
        }
    }

    @PostMapping("/tarefas/{id}/concluir")
    public String concluir(@PathVariable Long id, Model modelo, RedirectAttributes redirecionamento) {
        TarefaResumo tarefa = tarefas.concluir(idDe(modelo), id);
        redirecionamento.addFlashAttribute("aviso", "Tarefa concluída.");
        return "redirect:/areas/" + tarefa.areaId();
    }

    @PostMapping("/tarefas/{id}/excluir")
    public String excluir(@PathVariable Long id, Model modelo, RedirectAttributes redirecionamento) {
        Long pessoaId = idDe(modelo);
        TarefaResumo tarefa = tarefas.buscar(pessoaId, id);
        tarefas.excluir(pessoaId, id);
        redirecionamento.addFlashAttribute("aviso", "Tarefa excluída.");
        return "redirect:/areas/" + tarefa.areaId();
    }

    private String formComErros(Long areaId, Long pessoaId, TarefaResumo tarefa, Model modelo) {
        AreaResumo area = areas.buscar(pessoaId, areaId);
        modelo.addAttribute("area", area);
        modelo.addAttribute("tarefa", tarefa);
        modelo.addAttribute("responsaveis", tarefas.responsaveisPossiveis(pessoaId, areaId));
        modelo.addAttribute("prioridades", Prioridade.values());
        return "tarefa-form";
    }

    private static Long idDe(Model modelo) {
        return ((PessoaResumo) modelo.getAttribute("pessoa")).id();
    }
}
