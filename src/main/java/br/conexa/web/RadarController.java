package br.conexa.web;

import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.radar.porta.RadarPort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Radar Individual (US15): Índice de Carga, tarefas atrasadas e conflitos de horário. */
@Controller
public class RadarController {

    private final RadarPort radar;

    public RadarController(RadarPort radar) {
        this.radar = radar;
    }

    @GetMapping("/radar")
    public String radar(Model modelo) {
        Long pessoaId = ((PessoaResumo) modelo.getAttribute("pessoa")).id();
        modelo.addAttribute("indice", radar.calcularIndice(pessoaId));
        modelo.addAttribute("atrasadas", radar.listarAtrasadas(pessoaId));
        modelo.addAttribute("conflitos", radar.detectarConflitos(pessoaId));
        return "radar";
    }
}
