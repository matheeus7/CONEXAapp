package br.conexa.areas.nucleo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinhaVidaRepositorio extends JpaRepository<MinhaVida, Long> {

    boolean existsByPessoaId(Long pessoaId);

    Optional<MinhaVida> findByPessoaId(Long pessoaId);
}
