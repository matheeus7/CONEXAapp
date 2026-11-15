package br.conexa.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DashboardControllerTest {

    @Test
    void saudacaoPorPeriodo() {
        assertThat(DashboardController.saudacao(6)).isEqualTo("Bom dia");
        assertThat(DashboardController.saudacao(12)).isEqualTo("Boa tarde");
        assertThat(DashboardController.saudacao(17)).isEqualTo("Boa tarde");
        assertThat(DashboardController.saudacao(18)).isEqualTo("Boa noite");
    }

    @Test
    void primeiroNome() {
        assertThat(DashboardController.primeiroNome("  Ana Souza ")).isEqualTo("Ana");
        assertThat(DashboardController.primeiroNome("Bruno")).isEqualTo("Bruno");
    }
}
