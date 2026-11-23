package br.conexa.autenticacao.adaptador;

import br.conexa.autenticacao.nucleo.AutenticacaoService;
import br.conexa.autenticacao.nucleo.PessoaRepositorio;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/** Adaptador de saída para o Spring Security: carrega a Pessoa pelo e-mail. */
@Component
public class PessoaUserDetailsService implements UserDetailsService {

    private final PessoaRepositorio pessoas;

    public PessoaUserDetailsService(PessoaRepositorio pessoas) {
        this.pessoas = pessoas;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return pessoas.findByEmail(AutenticacaoService.normalizarEmail(email))
                .map(p -> new User(p.getEmail(), p.getSenhaHash(), List.of(new SimpleGrantedAuthority("ROLE_PESSOA"))))
                .orElseThrow(() -> new UsernameNotFoundException("Pessoa não encontrada"));
    }
}
