package com.gateway.RestClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration 
public class RestClientConfig {

    //variable para conectar a la api usuarios
    @Value("${microservicios.usuarios.url}")
    private String usuariosUrl;

    @Bean
    public RestClient usuariosRestClient() {
        return RestClient.builder()
                .baseUrl(usuariosUrl.trim())
                .build();
    }

    

}
