package com.desarrollos.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

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
public class SecurityBypassConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Primero configurar la infraestructura de Vaadin (CSRF, Push, etc.)
        super.configure(http);
        // Luego sobreescribir con permiso total — sin login requerido
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    }
}
