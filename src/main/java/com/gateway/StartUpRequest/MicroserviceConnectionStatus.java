package com.gateway.StartUpRequest;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estado de la conexion con uno de los microservicios que hace puente el gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicroserviceConnectionStatus {

    private String nombre;
    private String url;
    private boolean conectado;

    /** Texto listo para mostrar: "conectado ✓" o "desconectado ✗". */
    private String estado;

    /** Codigo HTTP obtenido en la raiz del microservicio (puede ser 404 y seguir arriba). */
    private Integer codigoHttp;

    /** Mensaje de error cuando no se pudo conectar; null si esta conectado. */
    private String mensajeError;

    private LocalDateTime verificadoEn;
}
