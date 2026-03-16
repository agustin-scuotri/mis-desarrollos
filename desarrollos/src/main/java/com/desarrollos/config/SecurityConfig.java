package com.desarrollos.config;

import com.desarrollos.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Configuración de Spring Security activa cuando app.security.enabled=true.
 *
 * Extiende VaadinWebSecurity para manejar automáticamente:
 *  - CSRF compatible con WebSocket/Push de Vaadin
 *  - Endpoints internos de Vaadin (/VAADIN/**, etc.)
 *  - Redirección al LoginView cuando el usuario no está autenticado
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        setLoginView(http, LoginView.class);
    }
}
