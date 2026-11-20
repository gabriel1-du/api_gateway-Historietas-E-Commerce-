package com.gateway.RestClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gateway.jwt.dto.UsuarioExternoDTO;

@Component 
public class UsuarioClient {

     private static final Logger log = LoggerFactory.getLogger(UsuarioClient.class);


    @Autowired
    private RestClient usuariosRestClient;

    public UsuarioExternoDTO getUsuarioById(Long id_usuario) {
        try {
            return usuariosRestClient.get()
                    .uri("/{id_usuario}", id_usuario)
                    .retrieve()
                    .body(UsuarioExternoDTO.class);
        } catch (Exception e) {
            // Si el servicio no responde o el usuario no existe
            log.error("ERROR AL CONECTAR CON USUARIOS CLIENT: {}", e.getMessage());
            return null;
        }
    }

}
