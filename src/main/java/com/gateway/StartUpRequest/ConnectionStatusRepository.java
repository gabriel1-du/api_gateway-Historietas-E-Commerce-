package com.gateway.StartUpRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

/**
 * Repositorio en memoria del ultimo resultado de las verificaciones.
 *
 * No usa JPA a proposito: los estados son datos de runtime, no entidades de
 * negocio, y el gateway ya no depende de base de datos. El mapa se reemplaza
 * de forma atomica para que las lecturas del endpoint nunca vean datos a medias.
 */
@Repository
public class ConnectionStatusRepository {

    private volatile Map<String, MicroserviceConnectionStatus> estados = Map.of();

    /** Guarda (o reemplaza) el resultado completo de una verificacion. */
    public void saveAll(List<MicroserviceConnectionStatus> estadosNuevos) {
        Map<String, MicroserviceConnectionStatus> copia = new LinkedHashMap<>();
        estadosNuevos.forEach(estado -> copia.put(estado.getNombre(), estado));
        this.estados = copia;
    }

    /** Devuelve todos los estados, conservando el orden en que se verificaron. */
    public List<MicroserviceConnectionStatus> findAll() {
        return new ArrayList<>(estados.values());
    }

    public Optional<MicroserviceConnectionStatus> findByNombre(String nombre) {
        return Optional.ofNullable(estados.get(nombre));
    }
}
