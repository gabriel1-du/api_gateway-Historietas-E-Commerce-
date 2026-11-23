package com.gateway.jwt.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class LoginRequest {

    // Nombres del gateway. Se aceptan tambien los del DTO loginBody de usuarios.
    @JsonAlias("correo_elec")
    private String email;

    @JsonAlias("contrasena")
    private String password;
}
