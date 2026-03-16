package com.desarrollos.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de bypass: se activa cuando app.security.enabled=false.
 *
 * Permite el acceso a toda la aplicación sin necesidad de login.
 * Útil durante el desarrollo para no perder tiempo con autenticación.
 *
 * Para activar/desactivar: cambiar app.security.enabled en application.yaml
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
public class SecurityBypassConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configurar infraestructura de Vaadin (CSRF, Push, etc.) y permitir todo sin login
        http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
            configurer.anyRequest(auth -> auth.permitAll())
        );
        return http.build();
    }
}
