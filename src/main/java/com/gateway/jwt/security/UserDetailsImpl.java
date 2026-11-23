package com.gateway.jwt.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;

/**
 * UserDetails construido unicamente con los claims del token JWT.
 * Ya no consulta la base de datos: el token emitido por api_usuarios
 * contiene el correo, permiso_admin y userId.
 */
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final String email;
    private final Boolean permisoAdmin;
    private final Integer id;

    public Integer getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = Boolean.TRUE.equals(permisoAdmin) ? "ROLE_ADMIN" : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        // El hash nunca sale de api_usuarios; el gateway no valida contrasenas.
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
