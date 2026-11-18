package br.conexa.conexoes.porta;

public enum NivelPermissao {
    VISUALIZADOR("Visualizador"), COLABORADOR("Colaborador");

    private final String rotulo;

    NivelPermissao(String rotulo) { this.rotulo = rotulo; }

    public String getRotulo() { return rotulo; }
}
