package br.conexa.eventos.nucleo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(name = "atualizada_por", nullable = false)
    private Long atualizadaPor;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected Evento() {
    }

    public Evento(Long areaId, String titulo, LocalDateTime inicio, LocalDateTime fim, Long pessoaId, LocalDateTime agora) {
        this.areaId = areaId;
        this.titulo = titulo;
        this.inicio = inicio;
        this.fim = fim;
        this.atualizadaPor = pessoaId;
        this.atualizadaEm = agora;
    }

    public void alterar(String titulo, LocalDateTime inicio, LocalDateTime fim, Long pessoaId, LocalDateTime agora) {
        this.titulo = titulo;
        this.inicio = inicio;
        this.fim = fim;
        this.atualizadaPor = pessoaId;
        this.atualizadaEm = agora;
    }

    /** Conflito: intervalos que se sobrepõem. Quem só se toca (fim de um = início do outro) não conflita. */
    public boolean conflitaCom(Evento outro) {
        return this.inicio.isBefore(outro.fim) && outro.inicio.isBefore(this.fim);
    }

    public Long getId() { return id; }
    public Long getAreaId() { return areaId; }
    public String getTitulo() { return titulo; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }
}
