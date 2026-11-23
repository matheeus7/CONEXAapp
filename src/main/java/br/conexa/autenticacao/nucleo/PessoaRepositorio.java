package br.conexa.autenticacao.nucleo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepositorio extends JpaRepository<Pessoa, Long> {

    Optional<Pessoa> findByEmail(String email);

    boolean existsByEmail(String email);
}
