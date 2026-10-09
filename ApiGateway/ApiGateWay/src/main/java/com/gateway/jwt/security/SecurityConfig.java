package com.gateway.jwt.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpMethod;
import static com.gateway.jwt.security.PublicRoutes.*;
import static com.gateway.redireccionApis.ApiUsuarios.regiones.RegionesPublicRoutes.REGIONES_PUBLIC_GET;
import static com.gateway.redireccionApis.ApiUsuarios.comunas.ComunasPublicRoutes.COMUNAS_PUBLIC_GET;
import static com.gateway.redireccionApis.ApiUsuarios.usuarios.UsuariosPublicRoutes.USUARIOS_PUBLIC_GET;
import static com.gateway.redireccionApis.ApiUsuarios.usuarios.UsuariosPublicRoutes.USUARIOS_PUBLIC_POST;
import static com.gateway.redireccionApis.ApiInventarios.autor.AutorPublicRoutes.AUTOR_PUBLIC_GET;
import static com.gateway.redireccionApis.ApiInventarios.genero.GeneroPublicRoutes.GENERO_PUBLIC_GET;
import static com.gateway.redireccionApis.ApiInventarios.historieta.HistorietaPublicRoutes.HISTORIETA_PUBLIC_GET;
import static com.gateway.redireccionApis.ApiInventarios.serie.SeriePublicRoutes.SERIE_PUBLIC_GET;
import static com.gateway.redireccionApis.ApiInventarios.tipo.TipoPublicRoutes.TIPO_PUBLIC_GET;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth

                // URL públicas JWT
                .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()

                // URL públicas API Usuarios (GET y POST)
                .requestMatchers(HttpMethod.GET, USUARIOS_PUBLIC_GET).permitAll()
                .requestMatchers(HttpMethod.POST, USUARIOS_PUBLIC_POST).permitAll()

                // URL públicas API Regiones
                .requestMatchers(HttpMethod.GET, REGIONES_PUBLIC_GET).permitAll()

                // URL públicas API Comunas
                .requestMatchers(HttpMethod.GET, COMUNAS_PUBLIC_GET).permitAll()

                // URL públicas API Inventarios
                .requestMatchers(HttpMethod.GET, AUTOR_PUBLIC_GET).permitAll()
                .requestMatchers(HttpMethod.GET, GENERO_PUBLIC_GET).permitAll()
                .requestMatchers(HttpMethod.GET, HISTORIETA_PUBLIC_GET).permitAll()
                .requestMatchers(HttpMethod.GET, SERIE_PUBLIC_GET).permitAll()
                .requestMatchers(HttpMethod.GET, TIPO_PUBLIC_GET).permitAll()

                // Cualquier otra requiere autenticación
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider(passwordEncoder()))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(BCryptPasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Accept-Encoding"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
