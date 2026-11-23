package com.gateway.RestClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.gateway.jwt.dto.AuthResponse;
import com.gateway.jwt.dto.LoginRequest;
import com.gateway.jwt.dto.UsuarioExternoDTO;

@Component
public class UsuarioClient {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String USUARIOS_PATH = "/api/usuariosApi";

    private static final Logger log = LoggerFactory.getLogger(UsuarioClient.class);

    private final RestClient usuariosRestClient;

    public UsuarioClient(@Qualifier("usuariosRestClient") RestClient usuariosRestClient) {
        this.usuariosRestClient = usuariosRestClient;
    }

    /**
     * Delega el login al microservicio de usuarios: es quien valida la contrasena
     * y genera el token. El gateway solo reenvia la peticion y la respuesta.
     */
    public AuthResponse login(LoginRequest request) {
        try {
            return usuariosRestClient.post()
                    .uri(LOGIN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AuthResponse.class);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                // Lo maneja el GlobalExceptionHandler del gateway (401 "Credenciales inválidas")
                log.warn("Credenciales rechazadas por api_usuarios: {}", e.getStatusText());
                throw new BadCredentialsException("Credenciales inválidas");
            }
            log.error("Error del microservicio de usuarios en {}: {} - {}",
                    LOGIN_PATH, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error del microservicio de usuarios al iniciar sesion");

        } catch (ResourceAccessException e) {
            log.error("No se pudo conectar con api_usuarios: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Microservicio de usuarios no disponible");
        }
    }

    public UsuarioExternoDTO getUsuarioByEmail(String correo_elec) {
        try {
            return usuariosRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(USUARIOS_PATH + "/email")
                            .queryParam("correo_elec", correo_elec)
                            .build())
                    .retrieve()
                    .body(UsuarioExternoDTO.class);
        } catch (Exception e) {
            // Si el servicio no responde o el usuario no existe
            log.error("ERROR AL CONECTAR CON USUARIOS CLIENT: {}", e.getMessage());
            return null;
        }
    }
}
