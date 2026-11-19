package com.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);  // 3 s para establecer la conexion
        factory.setReadTimeout(10000);    // 10 s esperando la respuesta

        RestTemplate restTemplate = new RestTemplate(factory);

        // Añadir un interceptor global a la Gateway
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("Accept-Encoding", "identity");
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}
