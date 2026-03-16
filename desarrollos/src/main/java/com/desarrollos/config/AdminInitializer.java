package com.desarrollos.config;

import com.desarrollos.entities.Usuario;
import com.desarrollos.repositories.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario admin por defecto al iniciar si no existe ninguno.
 * Credenciales iniciales: admin / admin123  (cambiar en producción)
 */
@Component
@Order(2)
public class AdminInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepo.count() == 0) {
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setEmail("admin@sistema.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRol("ADMIN");
            admin.setHabilitado(true);
            usuarioRepo.save(admin);
            System.out.println(">>> Usuario admin creado: admin / admin123");
        }
    }
}
