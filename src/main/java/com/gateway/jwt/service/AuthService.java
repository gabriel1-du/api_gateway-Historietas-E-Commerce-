package com.gateway.jwt.service;

import com.gateway.RestClient.UsuarioClient;
import com.gateway.jwt.dto.AuthResponse;
import com.gateway.jwt.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioClient usuarioClient;

    public AuthResponse login(LoginRequest request) {
        // El microservicio de usuarios valida las credenciales y genera el token.
        // El gateway solo actua como fachada: reenvia la peticion y devuelve el token.
        return usuarioClient.login(request);
    }
}
