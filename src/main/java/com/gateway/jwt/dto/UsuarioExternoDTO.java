package com.gateway.jwt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO del microservicio de usuarios. Ya no es una entidad JPA: el gateway
 * solo deserializa la respuesta HTTP de api_usuarios.
 */
@Data
public class UsuarioExternoDTO {

    @JsonProperty("id_usuario")
    private Integer id;

    @JsonProperty("correo_elec")
    private String email;

    @JsonProperty("contrasena")
    private String contrasena;

    @JsonProperty("permiso_admin")
    private Boolean permisoAdmin;
}
