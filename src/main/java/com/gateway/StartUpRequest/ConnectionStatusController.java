package com.gateway.StartUpRequest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Endpoint que entrega el estado de conexion del gateway con los demas
 * microservicios que hace puente.
 */
@RestController
@RequestMapping("/api/start-up")
@RequiredArgsConstructor
public class ConnectionStatusController {

    private final ConnectionStatusService connectionStatusService;

    @GetMapping("/conexiones")
    public ConnectionStatusResponse obtenerConexiones() {
        return connectionStatusService.obtenerEstado();
    }

    @GetMapping("/conexiones/ultimo")
    public ConnectionStatusResponse ultimoEstado() {
        return connectionStatusService.ultimoEstado();
    }

    /**
     * Misma salida que el banner de arranque, pero consultable por HTTP.
     */
    @GetMapping(value = "/check-resume", produces = MediaType.TEXT_PLAIN_VALUE)
    public String checkResume() {
        return connectionStatusService.generarResumen();
    }
}
