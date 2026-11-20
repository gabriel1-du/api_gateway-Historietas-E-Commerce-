package com.gateway.StartUp;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MicroserviceConnectionChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MicroserviceConnectionChecker.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private static final String CONECTADO = "conectado ✓";
    private static final String DESCONECTADO = "desconectado ✗";

    //lista de microservicios
    private final List<Microservice> microservices;
    private final String serverPort;

    // Resultado de cada verificacion, en el mismo orden de la lista de microservicios.
    private final Map<String, Boolean> conexiones = new LinkedHashMap<>();

    public MicroserviceConnectionChecker( //los parametros deben guardar los valores del .env guardados en app.properties
            @Value("${microservicios.usuarios.url}") String usuariosUrl,
            @Value("${microservicios.inventario.url}") String inventarioUrl,
            @Value("${microservicios.compras.url}") String comprasUrl,
            @Value("${microservicios.transporte.url}") String transporteUrl,
            @Value("${server.port}") String serverPort) {

        //Lista de apis que se estan conectando
        this.microservices = List.of(
                new Microservice("Usuarios", usuariosUrl),
                new Microservice("Inventario", inventarioUrl),
                new Microservice("Compras", comprasUrl),
                new Microservice("Transporte", transporteUrl)
        );
        this.serverPort = serverPort;
    }

    /**
     * Nombre con el que se identifica el microservicio y su URL base.
     */
    private record Microservice(String name, String url) {
        private Microservice {
            url = url.trim();
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Verificando conexion con los microservicios externos...");
        microservices.forEach(this::checkConnection);
        apiInfo();
    }

    private void checkConnection(Microservice microservice) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        RestClient client = RestClient.builder()
                .baseUrl(microservice.url())
                .requestFactory(requestFactory)
                .build();

        try {
            int statusCode = client.get()
                    .uri("/")
                    .exchange((request, response) -> response.getStatusCode().value());

            conexiones.put(microservice.name(), true);
            log.info("Se establecio conexion con el microservicio de {} ({}). Estado HTTP: {}",
                    microservice.name(), microservice.url(), statusCode);

        } catch (RestClientResponseException e) {
            // El servicio respondio, pero con un codigo no 2xx en la raiz (404, 401, etc.):
            // eso igual confirma que el microservicio esta arriba y accesible.
            conexiones.put(microservice.name(), true);
            log.info("Se establecio conexion con el microservicio de {} ({}). Estado HTTP: {} (servicio accesible)",
                    microservice.name(), microservice.url(), e.getStatusCode().value());

        } catch (Exception e) {
            conexiones.put(microservice.name(), false);
            log.error("No se pudo establecer conexion con el microservicio de {} ({}): {}",
                    microservice.name(), microservice.url(), e.getMessage());
        }
    }

    //Metodo que muestra mejor datos del arranque del microservicio
    public void apiInfo() {
        StringBuilder info = new StringBuilder();

        // Dibujo ASCII ya existente
        info.append("           _\r\n")
                .append("          {_}\r\n")
                .append("          /*\\\r\n")
                .append("         /_*_\\\r\n")
                .append("        {('o')}\r\n")
                .append("     C{{([^*^])}}D\r\n")
                .append("         [ * ]\r\n")
                .append("        /  Y  \\\r\n")
                .append("       _\\__|__/_\r\n")
                .append("      (___/ \\___)\r\n");

        // Resumen del arranque, justo debajo del dibujo
        info.append("\n")
                .append("Puerto de salida de esta api : ").append(serverPort).append("\n");

        microservices.forEach(microservice -> info
                .append("Conexion a api ")
                .append(microservice.name().toLowerCase())
                .append(" : ")
                .append(Boolean.TRUE.equals(conexiones.get(microservice.name())) ? CONECTADO : DESCONECTADO)
                .append("\n"));

        log.info("{}", info);
    }
}
