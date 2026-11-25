package com.gateway.StartUpRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Logica de verificacion de los microservicios con los que hace puente el gateway.
 * La misma idea que MicroserviceConnectionChecker, pero invocable bajo demanda
 * desde el controlador.
 */
@Service
public class ConnectionStatusService {

    private static final Logger log = LoggerFactory.getLogger(ConnectionStatusService.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);
    private static final String CONECTADO = "conectado ✓";
    private static final String DESCONECTADO = "desconectado ✗";
    private static final int MAX_ERROR = 300;

    private final ConnectionStatusRepository repository;
    private final List<Microservice> microservices;
    private final String serverPort;

    public ConnectionStatusService(
            ConnectionStatusRepository repository,
            @Value("${microservicios.usuarios.url}") String usuariosUrl,
            @Value("${microservicios.inventario.url}") String inventarioUrl,
            @Value("${microservicios.compras.url}") String comprasUrl,
            @Value("${microservicios.transporte.url}") String transporteUrl,
            @Value("${server.port}") String serverPort) {

        this.repository = repository;
        this.serverPort = serverPort;
        this.microservices = List.of(
                new Microservice("Usuarios", usuariosUrl),
                new Microservice("Inventario", inventarioUrl),
                new Microservice("Compras", comprasUrl),
                new Microservice("Transporte", transporteUrl));
    }

    private record Microservice(String nombre, String url) {
        private Microservice {
            url = url.trim();
        }
    }

    /**
     * Verifica todos los microservicios AHORA, guarda el resultado en el
     * repositorio en memoria y lo devuelve.
     */
    public ConnectionStatusResponse obtenerEstado() {
        LocalDateTime verificadoEn = LocalDateTime.now();

        List<MicroserviceConnectionStatus> estados = microservices.stream()
                .map(microservice -> verificar(microservice, verificadoEn))
                .toList();

        repository.saveAll(estados);

        return ConnectionStatusResponse.builder()
                .puertoGateway(serverPort)
                .verificadoEn(verificadoEn)
                .microservicios(estados)
                .build();
    }

    /**
     * Devuelve el ultimo resultado guardado, sin volver a conectarse.
     * Util si se consulta justo despues del arranque.
     */
    public ConnectionStatusResponse ultimoEstado() {
        return ConnectionStatusResponse.builder()
                .puertoGateway(serverPort)
                .verificadoEn(LocalDateTime.now())
                .microservicios(repository.findAll())
                .build();
    }

    /**
     * Genera el mismo resumen que se muestra en el arranque (dibujo del payaso,
     * puerto de salida y estado de cada microservicio), pero calculado al momento.
     */
    public String generarResumen() {
        ConnectionStatusResponse estado = obtenerEstado();

        StringBuilder resumen = new StringBuilder();

        resumen.append("           _\n")
                .append("          {_}\n")
                .append("          /*\\\n")
                .append("         /_*_\\\n")
                .append("        {('o')}\n")
                .append("     C{{([^*^])}}D\n")
                .append("         [ * ]\n")
                .append("        /  Y  \\\n")
                .append("       _\\__|__/_\n")
                .append("      (___/ \\___)\n");

        resumen.append("\n")
                .append("Puerto de salida de esta api : ").append(estado.getPuertoGateway()).append("\n");

        estado.getMicroservicios().forEach(microservice -> resumen
                .append("Conexion a api ")
                .append(microservice.getNombre().toLowerCase())
                .append(" : ")
                .append(microservice.getEstado())
                .append("\n"));

        return resumen.toString();
    }

    private MicroserviceConnectionStatus verificar(Microservice microservice, LocalDateTime verificadoEn) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        RestClient client = RestClient.builder()
                .baseUrl(microservice.url())
                .requestFactory(factory)
                .build();

        try {
            // Cualquier respuesta HTTP (incluso 404/500) prueba que el servicio
            // esta arriba y accesible, igual que en MicroserviceConnectionChecker.
            Integer codigoHttp = client.get()
                    .uri("/")
                    .exchange((request, response) -> response.getStatusCode().value());

            return estado(microservice, true, codigoHttp, null, verificadoEn);

        } catch (RestClientResponseException e) {
            return estado(microservice, true, e.getStatusCode().value(), null, verificadoEn);

        } catch (Exception e) {
            log.error("No se pudo conectar con {}: {}", microservice.nombre(), e.getMessage());
            return estado(microservice, false, null, resumir(e.getMessage()), verificadoEn);
        }
    }

    private MicroserviceConnectionStatus estado(
            Microservice microservice, boolean conectado, Integer codigoHttp,
            String mensajeError, LocalDateTime verificadoEn) {

        return MicroserviceConnectionStatus.builder()
                .nombre(microservice.nombre())
                .url(microservice.url())
                .conectado(conectado)
                .estado(conectado ? CONECTADO : DESCONECTADO)
                .codigoHttp(codigoHttp)
                .mensajeError(mensajeError)
                .verificadoEn(verificadoEn)
                .build();
    }

    private String resumir(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Sin detalle";
        }
        return mensaje.length() <= MAX_ERROR ? mensaje : mensaje.substring(0, MAX_ERROR) + "...";
    }
}
