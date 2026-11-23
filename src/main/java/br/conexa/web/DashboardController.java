package br.conexa.web;

import br.conexa.areas.porta.AreaPort;
import br.conexa.autenticacao.porta.PessoaResumo;
import br.conexa.notificacoes.porta.NotificacaoPort;
import br.conexa.radar.porta.RadarPort;
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
    private final AreaPort areas;
    private final RadarPort radar;
    private final NotificacaoPort notificacoes;

    public DashboardController(Clock relogio, TarefaPort tarefas, AreaPort areas, RadarPort radar,
                               NotificacaoPort notificacoes) {
        this.relogio = relogio;
        this.tarefas = tarefas;
        this.areas = areas;
        this.radar = radar;
        this.notificacoes = notificacoes;
    }

    @GetMapping("/")
    public String dashboard(Model modelo) {
        PessoaResumo pessoa = (PessoaResumo) modelo.getAttribute("pessoa");
        LocalDateTime agora = LocalDateTime.now(relogio);
        modelo.addAttribute("indice", radar.calcularIndice(pessoa.id()));
        modelo.addAttribute("alertas", notificacoes.listarAlertas(pessoa.id()));
        modelo.addAttribute("hoje", tarefas.listarPendentesDoResponsavel(pessoa.id(), agora.toLocalDate().plusDays(1).atStartOfDay())
                .stream().filter(t -> t.prazo().toLocalDate().equals(agora.toLocalDate())).toList());
        modelo.addAttribute("minhasAreas", areas.listar(pessoa.id()));
        modelo.addAttribute("compartilhadas", areas.listarCompartilhadas(pessoa.id()));
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
