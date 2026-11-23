package br.conexa.conexoes.nucleo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConexaoRepositorio extends JpaRepository<Conexao, Long> {

    Optional<Conexao> findByAreaIdAndPessoaId(Long areaId, Long pessoaId);

    boolean existsByAreaIdAndPessoaId(Long areaId, Long pessoaId);

    List<Conexao> findByAreaIdOrderByCriadaEmAsc(Long areaId);

    List<Conexao> findByPessoaId(Long pessoaId);

    @Modifying
    @Query("delete from Conexao c where c.areaId = :areaId")
    void deletarDaArea(@Param("areaId") Long areaId);
}
