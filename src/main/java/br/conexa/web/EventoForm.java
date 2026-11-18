package br.conexa.web;

import br.conexa.eventos.porta.DadosEvento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class EventoForm {

    @NotNull(message = "Escolha a Área do evento.")
    private Long areaId;

    @NotBlank(message = "Informe o título do evento.")
    @Size(max = 120, message = "O título deve ter no máximo 120 caracteres.")
    private String titulo;

    @NotNull(message = "Informe a data e a hora do evento.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime inicio;

    @NotNull(message = "Informe a duração em minutos.")
    @Min(value = 1, message = "A duração deve ser de pelo menos 1 minuto.")
    @Max(value = 1440, message = "A duração deve ser de no máximo 24 horas (1440 minutos).")
    private Integer duracaoMinutos = 60;

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo == null ? null : titulo.trim(); }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }

    public DadosEvento paraDados() {
        return new DadosEvento(titulo, inicio, duracaoMinutos);
    }
}
