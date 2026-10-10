# API Gateway — Spring Boot

> API Gateway centralizado para sistema de microservicios. Gestiona autenticación JWT, control de acceso y redirección de peticiones hacia los distintos microservicios.

---

## Índice

- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Autenticación y JWT](#autenticación-y-jwt)
- [ProxyControllers y PublicRoutes](#proxycontrollers-y-publicroutes)
- [Microservicios Conectados](#microservicios-conectados)
- [Tabla de Permisos](#tabla-de-permisos)
- [Configuración](#configuración)
- [Inicio Rápido](#inicio-rápido)

---

## Tecnologías Utilizadas

| Tecnología | Descripción |
|------------|-------------|
| **Java 17** | Lenguaje principal |
| **Spring Boot 3.4.5** | Framework base |
| **Spring Security** | Autenticación y autorización |
| **JJWT (0.11.5)** | Generación y validación de tokens JWT |
| **Spring Data JPA** | Acceso a base de datos (entidad Usuario) |
| **MySQL** | Base de datos relacional |
| **Lombok** | Reducción de código boilerplate |
| **Spring Configuration Processor** | Interpolación de variables en `application.properties` |

---

## Estructura del Proyecto

```
ApiGateWay/src/main/java/com/gateway/
├── AuthApiSpringBootApplication.java    → Clase principal Spring Boot
├── config/
│   ├── AppConfig.java                   → Bean RestTemplate con interceptor anti-compresión
│   └── OpenApiConfig.java               → Configuración OpenAPI/Swagger
├── jwt/                                 → Módulo de autenticación
│   ├── controller/
│   │   └── AuthController.java          → Endpoint POST /api/auth/login
│   ├── dto/
│   │   ├── LoginRequest.java            → Body de login (email + password)
│   │   └── AuthResponse.java            → Respuesta con token JWT
│   ├── model/
│   │   └── Usuario.java                → Entidad JPA (tabla USUARIOS)
│   ├── repository/
│   │   └── UsuarioRepository.java       → JPA Repository (findByEmail)
│   ├── security/
│   │   ├── JwtFilter.java              → Filtro que extrae y valida el token
│   │   ├── JwtUtil.java                → Generación y parseo de JWT
│   │   ├── JwtAuthorizationHelper.java  → Helper para validar admins en proxies
│   │   ├── SecurityConfig.java          → Cadenas de seguridad y rutas públicas
│   │   ├── PublicRoutes.java           → Rutas JWT públicas (login, ping)
│   │   ├── UserDetailsImpl.java        → Adaptador UserDetails desde Usuario
│   │   ├── UserDetailsServiceImpl.java  → Carga usuario desde BD
│   │   └── GlobalExceptionHandler.java → Manejador global de errores
│   └── service/
│       ├── AuthService.java             → Lógica de login
│       └── JwtService.java              → Extracción de claims
└── redireccionApis/                    → Redirecciones a microservicios
    ├── ApiUsuarios/                     → usuarios, regiones, comunas
    ├── ApiInventarios/                  → autor, genero, historieta, serie, tipo
    ├── ApiCompras/                      → boletas, carritos, historial, listaDeseos, mediosPago, productoCart, productoListaDeseos
    └── ApiTransporte/                   → empresaTransporte, pedidos, transportistas
```

Cada subcarpeta de `redireccionApis` contiene dos archivos por entidad:

| Archivo | Responsabilidad |
|---------|-----------------|
| `XxxPublicRoutes.java` | Define qué rutas GET (u otras) son públicas sin token |
| `XxxProxyController.java` | Recibe la petición, valida permisos y reenvía al microservicio |

---

## Autenticación y JWT

### Flujo de Login

```
[Cliente]
    │
    │  POST /api/auth/login
    │  Body: { "email": "...", "password": "..." }
    ▼
[AuthController]
    │
    ▼
[AuthService]
    │  1. AuthenticationManager valida credenciales (BCrypt)
    │  2. Busca el usuario en BD
    │  3. Genera JWT con claims: userId, username (email), permiso_admin
    ▼
[Cliente] ← Recibe { "token": "eyJhbGciOiJIUzUxMiJ9..." }
```

### Claims del Token

| Claim | Tipo | Descripción |
|-------|------|-------------|
| `sub` | String | Email del usuario (subject) |
| `username` | String | Email del usuario |
| `userId` | Integer | `id_usuario` de la BD |
| `permiso_admin` | Boolean | Indica si es administrador |

### Uso del Token

```
GET http://localhost:8888/api/proxy/regionesApi
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

---

## ProxyControllers y PublicRoutes

### PublicRoutes

Clase simple con arrays de strings que definen qué URLs no requieren autenticación:

```java
public class RegionesPublicRoutes {
    public static final String[] REGIONES_PUBLIC_GET = {
        "/api/proxy/regionesApi/**"
    };
}
```

Se registran en `SecurityConfig` con `.permitAll()`.

### ProxyController

Recibe la petición del cliente y:

1. **Reconstruye la URL** del microservicio destino (base-url + base-path + path original + query string)
2. **Valida permisos** según el método HTTP y la entidad
3. **Reenvía la petición** con `RestTemplate` (headers + body)
4. **Retorna la respuesta** del microservicio al cliente

```java
// Ejemplo: redirección
Cliente → Gateway (localhost:8888/api/proxy/regionesApi)
           ↓
         Microservicio (localhost:8080/api/regionesApi)
```

---

## Microservicios Conectados

| Grupo | Variable | Puerto | Entidades |
|-------|----------|--------|-----------|
| Usuarios | `microservicios.usuarios.url` | 8080 | usuarios, regiones, comunas |
| Inventario | `microservicios.inventario.url` | 8083 | autor, genero, historieta, serie, tipo |
| Compras | `microservicios.compras.url` | 8081 | boletas, carritos, historialCompras, listaDeseos, mediosPago, productoCart, productoListaDeseos |
| Transporte | `microservicios.transporte.url` | 8084 | empresaTransporte, pedidos, transportistas |

---

## Tabla de Permisos

### ApiUsuarios

| Entidad | Endpoint | GET | POST | PUT | DELETE |
|---------|----------|-----|------|-----|--------|
| Usuarios | `/api/proxy/usuariosApi` | ✅ Todos | ✅ Todos | 🔒 Admin o mismo usuario | 🔒 Admin o mismo usuario |
| Regiones | `/api/proxy/regionesApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Comunas | `/api/proxy/comunasApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |

### ApiInventarios

| Entidad | Endpoint | GET | POST | PUT | DELETE |
|---------|----------|-----|------|-----|--------|
| Autor | `/api/proxy/autorHistApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Género | `/api/proxy/generoHistApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Historieta | `/api/proxy/historietaApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Serie | `/api/proxy/serieHistApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Tipo | `/api/proxy/tipoHistApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |

### ApiCompras

| Entidad | Endpoint | GET | POST | PUT | DELETE |
|---------|----------|-----|------|-----|--------|
| Boletas | `/api/proxy/boletasApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Carritos | `/api/proxy/carritosApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Historial Compras | `/api/proxy/historialComprasApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Lista Deseos | `/api/proxy/ListaDeseosApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Medios de Pago | `/api/proxy/mediosPagoApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Producto Cart | `/api/proxy/ProductoCartApi` | ✅ Todos | ✅ Todos | ✅ Todos | ✅ Todos |
| Producto Lista Deseos | `/api/proxy/productoListaDeseosApi` | ✅ Todos | ✅ Todos | ✅ Todos | ✅ Todos |

### ApiTransporte

| Entidad | Endpoint | GET | POST | PUT | DELETE |
|---------|----------|-----|------|-----|--------|
| Empresa Transporte | `/api/proxy/EmpresaTransporteApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Pedidos | `/api/proxy/PedidosApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |
| Transportistas | `/api/proxy/TransportistasApi` | ✅ Todos | 🔒 Admin | 🔒 Admin | 🔒 Admin |

> **Nota:** PUT y DELETE en `/api/proxy/usuariosApi` solo están permitidos si el `id` del usuario en la URL coincide con el `userId` del token, o si el token tiene `permiso_admin = true`.

---

## Configuración

### application.properties

```properties
server.port=8888

# Base de datos MySQL (para autenticación)
spring.datasource.url=jdbc:mysql://localhost:3306/usuarios_db
spring.datasource.username=root
spring.datasource.password=hola123

# JWT
jwt.secret=clave_super_segura_...

# URLs base de microservicios (variables reutilizables)
microservicios.usuarios.url=http://localhost:8080
microservicios.inventario.url=http://localhost:8083
microservicios.compras.url=http://localhost:8081
microservicios.transporte.url=http://localhost:8084
```

### Dependencias principales (pom.xml)

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `mysql-connector-j`
- `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.11.5)
- `lombok`
- `spring-boot-configuration-processor`

---

## Inicio Rápido

### Requisitos

- Java 17+
- Maven (o usar `./mvnw`)
- MySQL corriendo en `localhost:3306` con base `usuarios_db`

### Pasos

```bash
# 1. Compilar
./mvnw compile

# 2. Ejecutar
./mvnw spring-boot:run

# 3. Probar login
curl -X POST http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"correo@ejemplo.com","password":"tu_contraseña"}'
```

### Crear usuario admin en BD

```sql
INSERT INTO USUARIOS (p_nombre, s_nombre, p_apellido, s_apellido, correo_elec, num_telefono, rut_dv, rut_cuerpo, contrasena, permiso_admin, id_region, id_comuna)
VALUES ('Admin', 'Admin', 'Sistema', 'Sistema', 'admin@test.com', '12345678', '0', '12345678', '$2a$10$...hash_bcrypt...', TRUE, 1, 1);
```

> El campo `contrasena` debe ser un hash BCrypt. Usa [bcrypt-generator.com](https://bcrypt-generator.com) para generarlo.

---

## Arquitectura del Flujo

```
[Cliente]
    │
    │  HTTP + JWT (opcional según ruta)
    ▼
┌─────────────────────────────────┐
│         API Gateway             │
│         (localhost:8888)        │
├─────────────────────────────────┤
│  JwtFilter → Valida token       │
│  SecurityConfig → Permisos      │
│  ProxyController → Redirecciona │
└─────────────────────────────────┘
    │           │           │           │
    ▼           ▼           ▼           ▼
[8080]      [8083]      [8081]      [8084]
Usuarios    Inventario  Compras     Transporte
```
