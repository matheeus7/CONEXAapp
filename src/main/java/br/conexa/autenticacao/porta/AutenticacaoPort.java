package br.conexa.autenticacao.porta;

import java.util.Optional;

/** Porta pública do componente Autenticação (RF01, RF02). Os demais componentes só enxergam isto. */
public interface AutenticacaoPort {

    /** Cria a conta e publica {@link PessoaCadastradaEvent}. Lança {@link EmailJaCadastradoException}. */
    PessoaResumo cadastrar(CadastroPessoa cadastro);

    Optional<PessoaResumo> buscarPorEmail(String email);

    PessoaResumo atualizarNome(Long pessoaId, String novoNome);
}
