package br.conexa.autenticacao.nucleo;

import br.conexa.autenticacao.porta.AutenticacaoPort;
import br.conexa.autenticacao.porta.CadastroPessoa;
import br.conexa.autenticacao.porta.EmailJaCadastradoException;
import br.conexa.autenticacao.porta.PessoaCadastradaEvent;
import br.conexa.autenticacao.porta.PessoaResumo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService implements AutenticacaoPort {

    private final PessoaRepositorio pessoas;
    private final PasswordEncoder codificador;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public AutenticacaoService(PessoaRepositorio pessoas, PasswordEncoder codificador,
                               ApplicationEventPublisher eventos, Clock relogio) {
        this.pessoas = pessoas;
        this.codificador = codificador;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public PessoaResumo cadastrar(CadastroPessoa cadastro) {
        String email = normalizarEmail(cadastro.email());
        if (pessoas.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }
        Pessoa nova = new Pessoa(cadastro.nome().trim(), email,
                codificador.encode(cadastro.senha()), LocalDateTime.now(relogio));
        try {
            nova = pessoas.saveAndFlush(nova);
        } catch (DataIntegrityViolationException e) {
            // cadastro simultâneo com o mesmo e-mail: a restrição única do banco decide
            throw new EmailJaCadastradoException();
        }
        eventos.publishEvent(new PessoaCadastradaEvent(nova.getId()));
        return resumo(nova);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PessoaResumo> buscarPorEmail(String email) {
        return pessoas.findByEmail(normalizarEmail(email)).map(AutenticacaoService::resumo);
    }

    @Override
    @Transactional
    public PessoaResumo atualizarNome(Long pessoaId, String novoNome) {
        Pessoa pessoa = pessoas.findById(pessoaId).orElseThrow();
        pessoa.renomear(novoNome.trim());
        return resumo(pessoa);
    }

    private static PessoaResumo resumo(Pessoa p) {
        return new PessoaResumo(p.getId(), p.getNome(), p.getEmail());
    }
}
