package br.conexa.web;

import br.conexa.conexoes.porta.NivelPermissao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ConexaoForm {

    @NotBlank(message = "Informe o e-mail da pessoa.")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
    private String email;

    @NotNull(message = "Escolha o nível de acesso.")
    private NivelPermissao nivel = NivelPermissao.VISUALIZADOR;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }
    public NivelPermissao getNivel() { return nivel; }
    public void setNivel(NivelPermissao nivel) { this.nivel = nivel; }
}
