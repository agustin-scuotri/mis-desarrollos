package com.desarrollos.config;

import com.desarrollos.views.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security activa cuando app.security.enabled=true.
 *
 * Usa VaadinSecurityConfigurer (Vaadin 25+) para manejar automáticamente:
 *  - CSRF compatible con WebSocket/Push de Vaadin
 *  - Endpoints internos de Vaadin (/VAADIN/**, etc.)
 *  - Redirección al LoginView cuando el usuario no está autenticado
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
            configurer.loginView(LoginView.class)
        );
        return http.build();
    }
}
