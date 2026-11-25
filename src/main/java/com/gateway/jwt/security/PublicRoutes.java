package com.gateway.jwt.security;

public class PublicRoutes {

    // Rutas públicas para GET
    public static final String[] PUBLIC_GET = {
        "/api/ping",
        "/api/start-up/conexiones",
        "/api/start-up/conexiones/ultimo",
        "/api/start-up/check-resume"
    };

    // Rutas públicas para POST
    public static final String[] PUBLIC_POST = {
        "/api/auth/login"
    };

}
