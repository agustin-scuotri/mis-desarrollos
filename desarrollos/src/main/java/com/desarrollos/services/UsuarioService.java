package com.desarrollos.services;

import com.desarrollos.entities.Usuario;
import com.desarrollos.repositories.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Spring Security: carga el usuario por username ────────────────────────
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.isHabilitado(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()))
        );
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    public List<Usuario> listarTodos() { return repo.findAll(); }

    public Usuario guardar(Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return repo.save(usuario);
    }

    /** Actualiza un usuario existente. Si nuevaPassword no está vacía, la re-encodea. */
    public Usuario actualizar(Usuario usuario, String nuevaPassword) {
        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        }
        return repo.save(usuario);
    }

    public void eliminar(Long id) { repo.deleteById(id); }

    public java.util.Optional<Usuario> buscarPorId(Long id) { return repo.findById(id); }

    public boolean existeUsername(String username) { return repo.existsByUsername(username); }
    public boolean existeEmail(String email)       { return repo.existsByEmail(email); }
    public boolean existeEmailParaOtroUsuario(String email, Long idActual) {
        return repo.findAll().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email) && !u.getId().equals(idActual));
    }
    public boolean existeUsernameParaOtroUsuario(String username, Long idActual) {
        return repo.findAll().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username) && !u.getId().equals(idActual));
    }
}
