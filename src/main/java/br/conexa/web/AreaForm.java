package br.conexa.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AreaForm {

    @NotBlank(message = "Informe o nome da Área.")
    @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.")
    private String nome;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome == null ? null : nome.trim(); }
}
