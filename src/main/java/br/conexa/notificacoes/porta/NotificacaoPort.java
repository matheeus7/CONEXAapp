package br.conexa.notificacoes.porta;

import java.util.List;

/** Porta pública das Notificações internas (RF12): só alertas dentro da aplicação. */
public interface NotificacaoPort {

    /** Tarefas da pessoa atrasadas ou com prazo em menos de 24 horas, atrasadas primeiro. */
    List<Alerta> listarAlertas(Long pessoaId);
}
