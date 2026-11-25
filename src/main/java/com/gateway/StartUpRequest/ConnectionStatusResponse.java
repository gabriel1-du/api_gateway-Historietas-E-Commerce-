package com.gateway.StartUpRequest;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta del endpoint de estado de conexiones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionStatusResponse {

    private String puertoGateway;
    private LocalDateTime verificadoEn;
    private List<MicroserviceConnectionStatus> microservicios;
}
