package br.conexa.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.conexa.autenticacao.nucleo.AutenticacaoService;
import br.conexa.autenticacao.nucleo.Pessoa;
import br.conexa.autenticacao.nucleo.PessoaRepositorio;
import br.conexa.autenticacao.porta.CadastroPessoa;
import br.conexa.autenticacao.porta.EmailJaCadastradoException;
import br.conexa.autenticacao.porta.PessoaCadastradaEvent;
import br.conexa.autenticacao.porta.PessoaResumo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** CT-087: o núcleo é testado sem banco nem outros componentes, com dublês nas dependências. */
@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock PessoaRepositorio pessoas;
    @Mock PasswordEncoder codificador;
    @Mock ApplicationEventPublisher eventos;

    AutenticacaoService servico;

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC);
        servico = new AutenticacaoService(pessoas, codificador, eventos, relogio);
    }

    @Test
    @DisplayName("normaliza o e-mail, guarda só o hash e publica o evento de cadastro")
    void cadastra() {
        when(pessoas.existsByEmail("ana@exemplo.com")).thenReturn(false);
        when(codificador.encode("senha-forte-1")).thenReturn("$2a$hash");
        when(pessoas.saveAndFlush(any(Pessoa.class))).thenAnswer(inv -> {
            Pessoa p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 7L);
            return p;
        });

        PessoaResumo resumo = servico.cadastrar(new CadastroPessoa("  Ana Souza ", "  ANA@Exemplo.com ", "senha-forte-1"));

        assertThat(resumo).isEqualTo(new PessoaResumo(7L, "Ana Souza", "ana@exemplo.com"));
        ArgumentCaptor<Pessoa> salva = ArgumentCaptor.forClass(Pessoa.class);
        verify(pessoas).saveAndFlush(salva.capture());
        assertThat(salva.getValue().getSenhaHash()).isEqualTo("$2a$hash");
        verify(eventos).publishEvent(new PessoaCadastradaEvent(7L));
    }

    @Test
    @DisplayName("e-mail já cadastrado não salva nem publica evento")
    void duplicado() {
        when(pessoas.existsByEmail("ana@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> servico.cadastrar(new CadastroPessoa("Ana", "ana@exemplo.com", "senha-forte-1")))
                .isInstanceOf(EmailJaCadastradoException.class);

        verify(pessoas, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }
}
