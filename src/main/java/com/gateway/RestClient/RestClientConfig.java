package com.gateway.RestClient;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    //variable para conectar a la api usuarios
    @Value("${microservicios.usuarios.url}")
    private String usuariosUrl;

    @Bean
    public RestClient usuariosRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(usuariosUrl.trim())
                .requestFactory(factory)
                .build();
    }
}
