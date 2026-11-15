package br.conexa.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContaForm {

    @NotBlank(message = "Informe seu nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome == null ? null : nome.trim(); }
}
