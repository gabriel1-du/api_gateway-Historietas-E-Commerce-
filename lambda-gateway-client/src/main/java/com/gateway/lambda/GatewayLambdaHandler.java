package com.gateway.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente HTTP liviano que actua como fachada de los 3 endpoints de
 * ConnectionStatusController del API Gateway desplegado en EC2.
 *
 * No usa Spring, RestClient ni Tomcat. Solo aws-lambda-java-core + java.net.http.
 */
public class GatewayLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String BASE_URL_ENV = "GATEWAY_BASE_URL";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static final String DEFAULT_ACTION = "conexiones";

    /** Las 3 rutas reales del controlador StartUpRequest.ConnectionStatusController. */
    private static final Map<String, String> ENDPOINTS = Map.of(
            "conexiones", "/api/start-up/conexiones",
            "ultimo", "/api/start-up/conexiones/ultimo",
            "check-resume", "/api/start-up/check-resume");

    private static final List<String> ACCIONES_VALIDAS =
            List.of("conexiones", "ultimo", "check-resume");

    /** Alias para hacer la Lambda mas comoda de invocar. */
    private static final Map<String, String> ALIASES = Map.of(
            "estado", "conexiones",
            "resumen", "check-resume",
            "last", "ultimo");

    private static final Pattern ACCION_EN_BODY =
            Pattern.compile("\"accion\"\\s*:\\s*\"([^\"]+)\"");

    /** Reutilizado entre invocaciones: es thread-safe y evita abrir un pool por request. */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        String baseUrl = System.getenv(BASE_URL_ENV);
        if (baseUrl == null || baseUrl.isBlank()) {
            return respuesta(500, "application/json",
                    errorJson("Falta la variable de entorno " + BASE_URL_ENV
                            + ". Configurala como http://<IP_EC2>:<PUERTO> en AWS Lambda."));
        }

        String accion = resolverAccion(event);
        String path = ENDPOINTS.get(accion);
        if (path == null) {
            return respuesta(400, "application/json",
                    errorJson("Accion no valida: '" + accion + "'. Usa una de: " + ACCIONES_VALIDAS));
        }

        String url = normalizarBaseUrl(baseUrl) + path;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // Se respeta el Content-Type del microservicio: JSON para /conexiones,
            // text/plain para /check-resume.
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("application/json; charset=utf-8");

            return respuesta(response.statusCode(), contentType, response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return respuesta(502, "application/json",
                    errorJson("Error de red al invocar " + url + ": " + e.getMessage()));

        } catch (IOException | RuntimeException e) {
            return respuesta(502, "application/json",
                    errorJson("Error de red al invocar " + url + ": "
                            + e.getClass().getSimpleName() + " - " + e.getMessage()));
        }
    }

    /**
     * Orden de prioridad (console "Probar" - Function URL):
     * 1) accion del JSON de entrada
     * 2) queryStringParameters.accion
     * 3) rawPath
     * 4) "accion" dentro del body
     * 5) por defecto -> conexiones
     */
    private String resolverAccion(Map<String, Object> event) {
        String accion = "";

        if (event != null) {
            accion = texto(event.get("accion"));

            if (esVacio(accion)) {
                Object query = event.get("queryStringParameters");
                if (query instanceof Map<?, ?> queryMap) {
                    accion = texto(queryMap.get("accion"));
                }
            }

            if (esVacio(accion)) {
                accion = desdeRawPath(texto(event.get("rawPath")));
            }

            if (esVacio(accion)) {
                accion = desdeBody(texto(event.get("body")));
            }
        }

        if (esVacio(accion)) {
            accion = DEFAULT_ACTION;
        }

        accion = accion.trim().toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(accion, accion);
    }

    private String desdeRawPath(String rawPath) {
        if (esVacio(rawPath)) {
            return "";
        }

        String path = rawPath.toLowerCase(Locale.ROOT);
        if (path.contains("check-resume")) {
            return "check-resume";
        }
        if (path.contains("ultimo")) {
            return "ultimo";
        }
        if (path.contains("conexiones")) {
            return "conexiones";
        }
        return "";
    }

    private String desdeBody(String body) {
        if (esVacio(body)) {
            return "";
        }
        Matcher matcher = ACCION_EN_BODY.matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String normalizarBaseUrl(String baseUrl) {
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private Map<String, Object> respuesta(int statusCode, String contentType, String body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statusCode", statusCode);
        result.put("headers", Map.of("Content-Type", contentType));
        result.put("body", body == null ? "" : body);
        result.put("isBase64Encoded", false);
        return result;
    }

    private String errorJson(String mensaje) {
        return "{\"error\": \"" + escapar(mensaje) + "\"}";
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
