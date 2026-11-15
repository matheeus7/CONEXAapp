package br.conexa.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {

    /** Relógio único da aplicação; os testes podem substituí-lo para controlar o tempo. */
    @Bean
    public Clock relogio(@Value("${conexa.fuso-horario:America/Manaus}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}
