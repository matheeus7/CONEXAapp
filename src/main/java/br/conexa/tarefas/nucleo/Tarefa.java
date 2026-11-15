package br.conexa.tarefas.nucleo;

import br.conexa.tarefas.porta.DadosTarefa;
import br.conexa.tarefas.porta.Prioridade;
import br.conexa.tarefas.porta.StatusTarefa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarefa")
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime prazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusTarefa status;

    @Column(name = "criador_id", nullable = false)
    private Long criadorId;

    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;

    @Column(name = "atualizada_por", nullable = false)
    private Long atualizadaPor;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected Tarefa() {
    }

    public Tarefa(Long areaId, DadosTarefa dados, Long criadorId, Long responsavelId, LocalDateTime agora) {
        this.areaId = areaId;
        this.titulo = dados.titulo();
        this.descricao = dados.descricao();
        this.prazo = dados.prazo();
        this.prioridade = dados.prioridade();
        this.status = StatusTarefa.PENDENTE;
        this.criadorId = criadorId;
        this.responsavelId = responsavelId;
        this.atualizadaPor = criadorId;
        this.atualizadaEm = agora;
    }

    public void editar(DadosTarefa dados, Long pessoaId, LocalDateTime agora) {
        this.titulo = dados.titulo();
        this.descricao = dados.descricao();
        this.prazo = dados.prazo();
        this.prioridade = dados.prioridade();
        registrar(pessoaId, agora);
    }

    public void concluir(Long pessoaId, LocalDateTime agora) {
        this.status = StatusTarefa.CONCLUIDA;
        registrar(pessoaId, agora);
    }

    /** Atrasada: pendente e com prazo anterior a agora (US08). */
    public boolean estaAtrasada(LocalDateTime agora) {
        return status == StatusTarefa.PENDENTE && prazo.isBefore(agora);
    }

    private void registrar(Long pessoaId, LocalDateTime agora) {
        this.atualizadaPor = pessoaId;
        this.atualizadaEm = agora;
    }

    public Long getId() { return id; }
    public Long getAreaId() { return areaId; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public LocalDateTime getPrazo() { return prazo; }
    public Prioridade getPrioridade() { return prioridade; }
    public StatusTarefa getStatus() { return status; }
    public Long getCriadorId() { return criadorId; }
    public Long getResponsavelId() { return responsavelId; }
}
