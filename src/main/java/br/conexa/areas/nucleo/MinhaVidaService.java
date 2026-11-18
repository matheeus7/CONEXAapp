package br.conexa.areas.nucleo;

import br.conexa.areas.porta.MinhaVidaPort;
import br.conexa.autenticacao.porta.PessoaCadastradaEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MinhaVidaService implements MinhaVidaPort {

    private final MinhaVidaRepositorio repositorio;
    private final Clock relogio;

    public MinhaVidaService(MinhaVidaRepositorio repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    /**
     * Observer: ao cadastrar uma Pessoa, cria sua Minha Vida (RF03). Roda de forma síncrona, na mesma
     * transação do cadastro: se a criação falhar, o cadastro inteiro é desfeito.
     */
    @EventListener
    @Transactional
    public void aoCadastrarPessoa(PessoaCadastradaEvent evento) {
        if (!repositorio.existsByPessoaId(evento.pessoaId())) {
            repositorio.save(new MinhaVida(evento.pessoaId(), LocalDateTime.now(relogio)));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePara(Long pessoaId) {
        return repositorio.existsByPessoaId(pessoaId);
    }
}
