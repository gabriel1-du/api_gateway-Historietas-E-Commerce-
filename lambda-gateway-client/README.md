# lambda-gateway-client

Proyecto Maven **independiente** y liviano para AWS Lambda (Java 21).
Solo depende de `com.amazonaws:aws-lambda-java-core`; el cliente HTTP es el
`java.net.http.HttpClient` nativo del JDK.

No modifica ni es módulo del microservicio Spring Boot principal.

## Endpoints que invoca

| Acción | Método | Ruta del API Gateway |
|---|---|---|
| `conexiones` (por defecto) | GET | `/api/start-up/conexiones` |
| `ultimo` | GET | `/api/start-up/conexiones/ultimo` |
| `check-resume` | GET | `/api/start-up/check-resume` (texto plano con el dibujo) |

Alias admitidos: `estado` → `conexiones`, `resumen` → `check-resume`, `last` → `ultimo`.

## Compilación

```bash
cd lambda-gateway-client
mvn clean package
```

Jar a subir a AWS Lambda:

```
lambda-gateway-client/target/lambda-gateway-client.jar
```

(El shade plugin también deja `original-lambda-gateway-client.jar`; **no** subas ese.)

## Configuración en AWS Lambda

- **Runtime:** Java 21 (`java21` / `provided.al2023` con Java 21).
- **Handler:** `com.gateway.lambda.GatewayLambdaHandler::handleRequest`
- **Variable de entorno:**
  - Clave: `GATEWAY_BASE_URL`
  - Valor: `http://<IP_EC2>:8888` (sin barra final; el código la normaliza)
- **Memoria:** 256 MB es suficiente.
- **Timeout:** 30 s (los timeouts internos son 10 s de conexión y 20 s de petición).
- Si usas **Function URL**, el formato de respuesta ya es compatible (`statusCode`, `headers`, `body`).

## Ejemplos de prueba

### 1. Consola AWS Lambda → pestaña "Probar" (evento JSON)

```json
{
  "accion": "conexiones"
}
```

Llama a `GET /api/start-up/conexiones`.

Si el evento viene vacío (`{}`), también usa `conexiones` por defecto.

### 2. Function URL (navegador / Postman) → último estado guardado

```
https://<function-url>?accion=ultimo
```

Llama a `GET /api/start-up/conexiones/ultimo`.

### 3. Function URL → resumen con el dibujo del payaso

```
https://<function-url>/check-resume
```

Llama a `GET /api/start-up/check-resume` y respeta el `Content-Type: text/plain`
devuelto por el microservicio.

También funciona desde la consola:

```json
{
  "accion": "check-resume"
}
```

## Errores

- Sin `GATEWAY_BASE_URL` → `500` con `{"error": "Falta la variable de entorno GATEWAY_BASE_URL ..."}`.
- Error de red / timeout / IP no accesible → `502` con la URL intentada y el detalle.
- Acción desconocida → `400` con la lista de acciones válidas.
- Si el API Gateway responde (incluso 404/500), su `statusCode` y su `body` se reenvían tal cual.
