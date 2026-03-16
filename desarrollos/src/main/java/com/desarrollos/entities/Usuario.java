package com.desarrollos.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String rol = "USER"; // USER | ADMIN

    @Column(nullable = false)
    private boolean habilitado = true;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId()                     { return id; }
    public void setId(Long id)              { this.id = id; }

    public String getUsername()             { return username; }
    public void setUsername(String u)       { this.username = u; }

    public String getEmail()                { return email; }
    public void setEmail(String e)          { this.email = e; }

    public String getPassword()             { return password; }
    public void setPassword(String p)       { this.password = p; }

    public String getRol()                  { return rol; }
    public void setRol(String rol)          { this.rol = rol; }

    public boolean isHabilitado()           { return habilitado; }
    public void setHabilitado(boolean h)    { this.habilitado = h; }
}
