package com.gateway.redireccionApis.ApiUsuarios.usuarios;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.jwt.service.JwtService;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/proxy/usuariosApi")
@RequiredArgsConstructor
public class UsuariosProxyController {

    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    @Value("${services.usuarios.base-url}")
    private String usuariosBaseUrl;

    @Value("${services.usuarios.base-path}")
    private String usuariosBasePath;

    @RequestMapping(value = {"", "/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyUsuarios(HttpServletRequest request,
                                           @RequestBody(required = false) byte[] body,
                                           @RequestHeader HttpHeaders headers) {
        return handleProxy(request, body, headers);
    }

    private ResponseEntity<?> handleProxy(HttpServletRequest request, byte[] body, HttpHeaders headers) {
        String originalPath = request.getRequestURI().replace("/api/proxy/usuariosApi", "");
        String queryString = request.getQueryString();

        var uriBuilder = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(usuariosBaseUrl)
                .path(usuariosBasePath)
                .path(originalPath);

        if (queryString != null) {
            uriBuilder.query(queryString);
        }

        String targetUrl = uriBuilder.build(true).toUriString();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        System.out.println("USUARIOS targetUrl: " + targetUrl + "  METHOD: " + method);

        // PUT y DELETE requieren verificación de permisos
        if (method == HttpMethod.PUT || method == HttpMethod.DELETE) {
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"Token no presente o inválido\"}");
            }

            String token = authHeader.replace("Bearer ", "");

            // Extraer claims del token
            Integer tokenUserId = jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));
            Boolean isAdmin = jwtService.extractClaim(token, claims -> claims.get("permiso_admin", Boolean.class));

            // Si es admin, puede hacer cualquier operación
            if (Boolean.TRUE.equals(isAdmin)) {
                return forwardRequest(request, targetUrl, method, body, headers);
            }

            // Si no es admin, verificar que el id del usuario coincida con el del token
            // El id del usuario está en la URL (ej: /api/proxy/usuariosApi/123)
            String path = request.getRequestURI().replace("/api/proxy/usuariosApi", "");
            String[] pathParts = path.split("/");
            String urlUserId = null;
            for (String part : pathParts) {
                if (part.matches("\\d+")) {
                    urlUserId = part;
                    break;
                }
            }

            if (urlUserId == null || tokenUserId == null || !urlUserId.equals(tokenUserId.toString())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"No tienes permiso para realizar esta operación\"}");
            }
        }

        return forwardRequest(request, targetUrl, method, body, headers);
    }

    private ResponseEntity<?> forwardRequest(HttpServletRequest request, String targetUrl, HttpMethod method,
                                             byte[] body, HttpHeaders headers) {
        HttpHeaders cleanHeaders = new HttpHeaders();
        headers.forEach((key, value) -> {
            if (!key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                cleanHeaders.put(key, value);
            }
        });

        HttpEntity<byte[]> entity = new HttpEntity<>(body, cleanHeaders);

        try {
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, method, entity, String.class);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Error inesperado en el API Gateway\", \"detalle\": \"" + ex.getMessage() + "\"}");
        }
    }
}
