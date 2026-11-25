package com.gateway.jwt.security;

public class PublicRoutes {

    // Rutas públicas para GET
    public static final String[] PUBLIC_GET = {
        "/api/ping",

        // Todas las rutas del controlador de estado de conexiones son publicas.
        // El wildcard evita que una nueva subruta quede fuera y caiga en
        // anyRequest().authenticated() devolviendo 403.
        "/api/start-up/**",
        "/api/start-up/conexiones",
        "/api/start-up/conexiones/ultimo",
        "/api/start-up/check-resume"
    };

    // Rutas públicas para POST
    public static final String[] PUBLIC_POST = {
        "/api/auth/login"
    };

}
