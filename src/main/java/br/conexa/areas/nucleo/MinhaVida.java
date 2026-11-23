package br.conexa.areas.nucleo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "minha_vida")
public class MinhaVida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pessoa_id", nullable = false, unique = true)
    private Long pessoaId;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected MinhaVida() {
    }

    public MinhaVida(Long pessoaId, LocalDateTime criadaEm) {
        this.pessoaId = pessoaId;
        this.criadaEm = criadaEm;
    }

    public Long getId() { return id; }
    public Long getPessoaId() { return pessoaId; }
}
