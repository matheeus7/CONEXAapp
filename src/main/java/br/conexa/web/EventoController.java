package br.conexa.web;

import br.conexa.areas.porta.AreaPort;
import br.conexa.areas.porta.AreaResumo;
import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.conexoes.porta.PoliticaDeAcesso;
import br.conexa.eventos.porta.EventoInvalidoException;
import br.conexa.eventos.porta.EventoPort;
import br.conexa.eventos.porta.EventoResumo;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Calendário e Eventos (US09, US10, US11). */
@Controller
public class EventoController {

    private static final DateTimeFormatter TITULO_MES =
            DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));

    private final EventoPort eventos;
    private final AreaPort areas;
    private final Clock relogio;
    private final PoliticaDeAcesso politica;

    public EventoController(EventoPort eventos, AreaPort areas, Clock relogio, PoliticaDeAcesso politica) {
        this.politica = politica;
        this.eventos = eventos;
        this.areas = areas;
        this.relogio = relogio;
    }

    @GetMapping("/calendario")
    public String calendario(@RequestParam(required = false) String mes, Model modelo) {
        LocalDate hoje = LocalDate.now(relogio);
        YearMonth atual = interpretar(mes, hoje);
        List<EventoResumo> doMes = eventos.listarDoPeriodo(idDe(modelo),
                atual.atDay(1).atStartOfDay(), atual.plusMonths(1).atDay(1).atStartOfDay());
        String titulo = atual.atDay(1).format(TITULO_MES);
        modelo.addAttribute("tituloMes", Character.toUpperCase(titulo.charAt(0)) + titulo.substring(1));
        modelo.addAttribute("semanas", CalendarioMes.semanas(atual, hoje, doMes));
        modelo.addAttribute("eventosDoMes", doMes);
        modelo.addAttribute("haConflito", doMes.stream().anyMatch(EventoResumo::emConflito));
        modelo.addAttribute("mesAnterior", atual.minusMonths(1).toString());
        modelo.addAttribute("mesSeguinte", atual.plusMonths(1).toString());
        return "calendario";
    }

    @GetMapping("/eventos/novo")
    public String novo(@RequestParam(required = false) Long areaId, Model modelo) {
        EventoForm form = new EventoForm();
        form.setAreaId(areaId);
        return formulario(form, null, modelo);
    }

    @PostMapping("/eventos")
    public String criar(@Valid EventoForm form, BindingResult resultado, Model modelo,
                        RedirectAttributes redirecionamento) {
        if (resultado.hasErrors()) {
            return formulario(form, null, modelo);
        }
        try {
            EventoResumo criado = eventos.criar(idDe(modelo), form.getAreaId(), form.paraDados());
            return concluido(criado, "Evento criado.", redirecionamento);
        } catch (EventoInvalidoException e) {
            resultado.reject("evento.invalido", e.getMessage());
            return formulario(form, null, modelo);
        }
    }

    @GetMapping("/eventos/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        EventoResumo evento = eventos.buscar(idDe(modelo), id);
        EventoForm form = new EventoForm();
        form.setAreaId(evento.areaId());
        form.setTitulo(evento.titulo());
        form.setInicio(evento.inicio());
        form.setDuracaoMinutos((int) evento.duracaoMinutos());
        return formulario(form, evento, modelo);
    }

    @PostMapping("/eventos/{id}")
    public String salvar(@PathVariable Long id, @Valid EventoForm form, BindingResult resultado, Model modelo,
                         RedirectAttributes redirecionamento) {
        Long pessoaId = idDe(modelo);
        EventoResumo atual = eventos.buscar(pessoaId, id);
        form.setAreaId(atual.areaId());
        if (resultado.hasErrors()) {
            return formulario(form, atual, modelo);
        }
        try {
            EventoResumo editado = eventos.editar(pessoaId, id, form.paraDados());
            return concluido(editado, "Evento atualizado.", redirecionamento);
        } catch (EventoInvalidoException e) {
            resultado.reject("evento.invalido", e.getMessage());
            return formulario(form, atual, modelo);
        }
    }

    @PostMapping("/eventos/{id}/excluir")
    public String excluir(@PathVariable Long id, Model modelo, RedirectAttributes redirecionamento) {
        Long pessoaId = idDe(modelo);
        EventoResumo evento = eventos.buscar(pessoaId, id);
        eventos.excluir(pessoaId, id);
        redirecionamento.addFlashAttribute("aviso", "Evento excluído.");
        return "redirect:/calendario?mes=" + YearMonth.from(evento.inicio());
    }

    private String concluido(EventoResumo evento, String mensagem, RedirectAttributes redirecionamento) {
        redirecionamento.addFlashAttribute("aviso", evento.emConflito()
                ? mensagem + " Atenção: ele conflita com outro evento seu."
                : mensagem);
        return "redirect:/calendario?mes=" + YearMonth.from(evento.inicio());
    }

    private String formulario(EventoForm form, EventoResumo evento, Model modelo) {
        Long pessoaId = idDe(modelo);
        List<AreaResumo> disponiveis = areas.listarAcessiveis(pessoaId).stream()
                .filter(a -> politica.podeEscrever(pessoaId, a)).toList();
        modelo.addAttribute("eventoForm", form);
        modelo.addAttribute("evento", evento);
        modelo.addAttribute("areas", disponiveis);
        return "evento-form";
    }

    private static YearMonth interpretar(String mes, LocalDate hoje) {
        if (mes != null) {
            try {
                return YearMonth.parse(mes);
            } catch (DateTimeParseException ignorado) {
                // cai no mês atual
            }
        }
        return YearMonth.from(hoje);
    }

    private static Long idDe(Model modelo) {
        return ((PessoaResumo) modelo.getAttribute("pessoa")).id();
    }
}
