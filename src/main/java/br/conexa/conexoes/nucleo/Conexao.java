package br.conexa.conexoes.nucleo;

import br.conexa.conexoes.porta.NivelPermissao;
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
@Table(name = "conexao")
public class Conexao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "pessoa_id", nullable = false)
    private Long pessoaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private NivelPermissao nivel;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Conexao() {
    }

    public Conexao(Long areaId, Long pessoaId, NivelPermissao nivel, LocalDateTime criadaEm) {
        this.areaId = areaId;
        this.pessoaId = pessoaId;
        this.nivel = nivel;
        this.criadaEm = criadaEm;
    }

    public void alterarNivel(NivelPermissao novo) { this.nivel = novo; }

    public Long getId() { return id; }
    public Long getAreaId() { return areaId; }
    public Long getPessoaId() { return pessoaId; }
    public NivelPermissao getNivel() { return nivel; }
}
