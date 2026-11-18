package br.conexa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SegurancaConfig {

    /** RNF02: hash adaptativo e salgado (BCrypt); a senha nunca é guardada em texto puro. */
    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http,
                                       @Value("${conexa.exigir-https:false}") boolean exigirHttps) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/entrar", "/cadastro", "/css/**", "/js/**", "/error", "/error/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/entrar")
                .loginProcessingUrl("/entrar")
                .usernameParameter("email")
                .passwordParameter("senha")
                .defaultSuccessUrl("/", true)
                .failureUrl("/entrar?erro"))
            .logout(l -> l
                .logoutUrl("/sair")
                .logoutSuccessUrl("/entrar?saiu")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .headers(h -> h
                .contentSecurityPolicy(c -> c.policyDirectives(
                    "default-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                    + "font-src https://fonts.gstatic.com; img-src 'self' data:; script-src 'self'; "
                    + "frame-ancestors 'none'; form-action 'self'; base-uri 'self'"))
                .frameOptions(f -> f.deny()))
            .sessionManagement(Customizer.withDefaults());
        if (exigirHttps) {
            http.requiresChannel(c -> c.anyRequest().requiresSecure());
        }
        return http.build();
    }
}
