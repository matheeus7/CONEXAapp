package br.conexa.autenticacao.nucleo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "pessoa")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Pessoa() {
    }

    public Pessoa(String nome, String email, String senhaHash, LocalDateTime criadaEm) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.criadaEm = criadaEm;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public LocalDateTime getCriadaEm() { return criadaEm; }

    public void renomear(String novoNome) {
        this.nome = novoNome;
    }
}
