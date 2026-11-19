package com.gateway.jwt.security;

import com.gateway.jwt.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Helper centralizado para validar permisos JWT en los proxies.
 * Evita duplicar lógica de validación de tokens en cada controlador.
 */
@Component
public class JwtAuthorizationHelper {

    private final JwtService jwtService;

    public JwtAuthorizationHelper(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Verifica si el usuario es administrador según el token.
     */
    public boolean isAdmin(String token) {
        Boolean permisoAdmin = jwtService.extractClaim(token, claims -> claims.get("permiso_admin", Boolean.class));
        return Boolean.TRUE.equals(permisoAdmin);
    }

    /**
     * Verifica si el ID del usuario en el token coincide con el ID proporcionado.
     */
    public boolean isSameUser(String token, Integer userId) {
        Integer tokenUserId = jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));
        return tokenUserId != null && tokenUserId.equals(userId);
    }

    /**
     * Extrae el ID del usuario desde el token.
     */
    public Integer extractUserId(String token) {
        return jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    /**
     * Valida que el token esté presente y sea válido.
     * Retorna null si es válido, o un ResponseEntity con el error si no lo es.
     */
    public ResponseEntity<?> validateTokenPresent(HttpHeaders headers) {
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Token no presente o inválido\"}");
        }
        return null;
    }

    /**
     * Extrae el token limpio (sin prefijo "Bearer ").
     */
    public String extractToken(HttpHeaders headers) {
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        return authHeader != null ? authHeader.replace("Bearer ", "") : null;
    }
}
