package com.gateway.redireccionApis.ApiTransporte.transportistas;

import com.gateway.jwt.security.JwtAuthorizationHelper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/proxy/TransportistasApi")
@RequiredArgsConstructor
public class TransportistasProxyController {

    private final RestTemplate restTemplate;
    private final JwtAuthorizationHelper jwtAuthorizationHelper;

    @Value("${services.transportistas.base-url}")
    private String serviceBaseUrl;

    @Value("${services.transportistas.base-path}")
    private String serviceBasePath;

    @RequestMapping(value = {"", "/**"}, method = RequestMethod.GET)
    public ResponseEntity<?> proxyTransportistasPublic(HttpServletRequest request,
                                                         @RequestHeader HttpHeaders headers) {
        return handleProxy(request, null, headers);
    }

    @RequestMapping(value = {"", "/**"}, method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyTransportistasSecure(HttpServletRequest request,
                                                        @RequestBody(required = false) String body,
                                                        @RequestHeader HttpHeaders headers) {
        return handleProxy(request, body, headers);
    }

    private ResponseEntity<?> handleProxy(HttpServletRequest request, String body, HttpHeaders headers) {
        String originalPath = request.getRequestURI().replace("/api/proxy/TransportistasApi", "");
        String queryString = request.getQueryString();

        var uriBuilder = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(serviceBaseUrl)
                .path(serviceBasePath)
                .path(originalPath);

        if (queryString != null) {
            uriBuilder.query(queryString);
        }

        String targetUrl = uriBuilder.build(true).toUriString();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        System.out.println("TRANSPORTISTAS targetUrl: " + targetUrl + "  METHOD: " + method);

        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.DELETE) {
            ResponseEntity<?> tokenError = jwtAuthorizationHelper.validateTokenPresent(headers);
            if (tokenError != null) {
                return tokenError;
            }

            String token = jwtAuthorizationHelper.extractToken(headers);

            if (!jwtAuthorizationHelper.isAdmin(token)) {
                return ResponseEntity.status(403)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"Operación restringida a administradores\"}");
            }
        }

        HttpHeaders cleanHeaders = new HttpHeaders();
        headers.forEach((key, value) -> {
            if (!key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                cleanHeaders.put(key, value);
            }
        });
        cleanHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body, cleanHeaders);

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
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Error inesperado en el API Gateway\", \"detalle\": \"" + ex.getMessage() + "\"}");
        }
    }
}
