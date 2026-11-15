package br.conexa.areas.nucleo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "area")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "minha_vida_id", nullable = false)
    private Long minhaVidaId;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Area() {
    }

    public Area(Long minhaVidaId, String nome, LocalDateTime criadaEm) {
        this.minhaVidaId = minhaVidaId;
        this.nome = nome;
        this.criadaEm = criadaEm;
    }

    public Long getId() { return id; }
    public Long getMinhaVidaId() { return minhaVidaId; }
    public String getNome() { return nome; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void renomear(String novoNome) { this.nome = novoNome; }
}
