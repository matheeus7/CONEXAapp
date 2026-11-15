package br.conexa.web;

import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.tarefas.porta.TarefaPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final DateTimeFormatter DATA_LONGA =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"));

    private final Clock relogio;
    private final TarefaPort tarefas;

    public DashboardController(Clock relogio, TarefaPort tarefas) {
        this.relogio = relogio;
        this.tarefas = tarefas;
    }

    @GetMapping("/")
    public String dashboard(Model modelo) {
        PessoaResumo pessoa = (PessoaResumo) modelo.getAttribute("pessoa");
        LocalDateTime agora = LocalDateTime.now(relogio);
        modelo.addAttribute("atrasadas", tarefas.listarAtrasadas(pessoa.id()));
        modelo.addAttribute("proximas", tarefas.listarPendentesDoResponsavel(pessoa.id(), agora.plusDays(7)).stream()
                .filter(t -> !t.atrasada()).toList());
        modelo.addAttribute("saudacao", saudacao(agora.getHour()));
        modelo.addAttribute("primeiroNome", primeiroNome(pessoa.nome()));
        String data = agora.format(DATA_LONGA);
        modelo.addAttribute("dataLonga", Character.toUpperCase(data.charAt(0)) + data.substring(1));
        return "dashboard";
    }

    static String saudacao(int hora) {
        if (hora < 12) return "Bom dia";
        if (hora < 18) return "Boa tarde";
        return "Boa noite";
    }

    static String primeiroNome(String nome) {
        return nome.trim().split("\\s+")[0];
    }
}
