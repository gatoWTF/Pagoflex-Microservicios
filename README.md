# PagoFlex — Microservicios

Microservicios Spring Boot 4.1 (Java 21, Maven, JPA, MySQL) con Eureka Server y API Gateway (Spring Cloud 2025.1) que consume la app PagoFlex con Retrofit.

App Android: [gatoWTF/PagoFlex](https://github.com/gatoWTF/PagoFlex).

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| `eureka-server` | 8761 | — | Registro y descubrimiento de servicios |
| `api-gateway` | 8080 | — | Entrada única de la app |
| `roles-service` | 8081 | `pagoflex_roles` | Roles de las cuentas |
| `usuarios-service` | 8082 | `pagoflex_usuarios` | Cuentas, inicio de sesión y perfil |
| `compromisos-service` | 8083 | `pagoflex_compromisos` | Compromisos, pagos, comprobantes y reportes |

## Cómo encenderlos

1. Abrir **Laragon** y presionar **Iniciar todo** (MySQL en el puerto 3306, usuario `root` sin contraseña). Las bases se crean solas.
2. Doble clic en `encender-todo.bat`. Cada servicio queda en su propia ventana y está listo cuando muestra `Started`.
3. Para apagarlos, cerrar sus ventanas.

También se pueden encender abriendo esta carpeta en **VS Code** y aceptando las tareas automáticas.

## Documentación de los endpoints (Swagger)

Con los servicios encendidos:

- roles-service: `http://localhost:8081/swagger-ui/index.html`
- usuarios-service: `http://localhost:8082/swagger-ui/index.html`
- compromisos-service: `http://localhost:8083/swagger-ui/index.html`

## Pruebas y cobertura

En la carpeta de cada servicio, con Java 21 o superior:

```powershell
.\mvnw.cmd test
```

JaCoCo genera el informe en `target/site/jacoco/index.html` y la prueba falla si la cobertura de líneas baja del 80%.
