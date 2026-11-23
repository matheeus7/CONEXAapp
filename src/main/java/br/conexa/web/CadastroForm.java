package br.conexa.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CadastroForm {

    @NotBlank(message = "Informe seu nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
    private String email;

    @NotBlank(message = "Informe uma senha.")
    @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
    private String senha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome == null ? null : nome.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
