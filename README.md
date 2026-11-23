# ms-catalogo · Ferretería Los Maestros

Microservicio de **catálogo de productos y categorías** y **dueño del stock** de la tienda online de Ferretería Los Maestros (DSY1104 Desarrollo Fullstack II, Duoc UC).

Es uno de los tres microservicios del sistema:

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| `ms-usuarios` | 8081 | Registro, login y emisión de JWT |
| **`ms-catalogo`** | **8082** | **Categorías, productos y stock** |
| `ms-ventas` | 8083 | Compras y órdenes (descuenta stock en este servicio) |

El front (React) está en [ferreteria-los-maestros](https://github.com/bcarrascom/ferreteria-los-maestros). En producción, nginx recibe todas las peticiones y manda `/api/v1/productos` y `/api/v1/categorias` a este servicio.

## Stack

Java 17 · Spring Boot 4.1.2 (Spring Security 7, Hibernate 7, Jackson 3) · MySQL 8 · springdoc-openapi 3.2.0 · jjwt 0.12.6 · JUnit 5, Mockito y H2 para los tests.

## Estructura

```
cl.duoc.ferreteria.catalogo
├── controller/   endpoints REST, sin lógica (CategoriaController, ProductoController, StockInternoController)
├── service/      reglas de negocio (CategoriaService, ProductoService, StockService)
├── model/        entidades JPA (Categoria, Producto)
├── repository/   acceso a datos, incluido el descuento atómico de stock
├── dto/          entradas validadas y respuestas (nunca se devuelve una entidad)
├── security/     validación de JWT, reglas por rol y clave interna
├── config/       Swagger, carga inicial de datos, registro del interceptor
└── exception/    excepciones de negocio y formato único de error
```

## Variables de entorno

Se leen de un archivo `.env` en la raíz (no se sube al repo). Copiar `.env.example` y completar.

| Variable | Ejemplo | Nota |
|---|---|---|
| `DB_HOST` | `localhost` | `mysql` dentro de docker compose |
| `DB_PORT` | `3306` | |
| `DB_USER` | `ferreteria` | |
| `DB_PASSWORD` | | Contraseña del usuario de MySQL |
| `JWT_SECRET` | | Base64 de 32 bytes o más (`openssl rand -base64 32`). **El mismo en los 3 servicios** |
| `INTERNAL_API_KEY` | | **La misma en ms-catalogo y ms-ventas** |

`JWT_SECRET` e `INTERNAL_API_KEY` no tienen valor por defecto: si faltan, el servicio no arranca.

## Cómo levantarlo

### En local

1. MySQL en Docker. Con el docker compose del equipo, desde `ferreteria-los-maestros/despliegue/`:
   ```bash
   docker compose up -d mysql
   ```
   O un contenedor suelto:
   ```bash
   docker run -d --name ferreteria-mysql -p 3306:3306 -v ferreteria-mysql-datos:/var/lib/mysql \
     -e MYSQL_ROOT_PASSWORD=<root> -e MYSQL_DATABASE=db_catalogo \
     -e MYSQL_USER=ferreteria -e MYSQL_PASSWORD=<la de DB_PASSWORD> mysql:8.4
   ```
2. Crear `.env` desde `.env.example`.
3. Arrancar el servicio:
   ```bash
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```

Al arrancar por primera vez, si la tabla de productos está vacía, se cargan las 9 categorías y los 85 productos de `src/main/resources/datos/productos.json`. Si ya hay datos, no se toca nada.

### Con Docker

```bash
docker build -t ms-catalogo .
docker run --env-file .env -e DB_HOST=host.docker.internal -p 8082:8082 ms-catalogo
```

El `Dockerfile` es multi-etapa: compila con el JDK y la imagen final solo lleva el JRE y el jar. En el despliegue (EC2) el servicio se levanta con el `docker-compose.yml` de `ferreteria-los-maestros/despliegue/`.

## Swagger

**http://localhost:8082/swagger-ui.html**

Para los endpoints de administrador: iniciar sesión en ms-usuarios (`POST /api/v1/auth/login`, por ejemplo con `admin@ferreteria.cl` / `Maestros2026`, usuario de demostración), copiar el `accessToken` y pegarlo en el botón **Authorize**. Los endpoints internos piden el header `X-API-KEY` en el mismo formulario de cada petición.

## Endpoints

Todas las respuestas son JSON en camelCase. Los errores usan un formato único:

```json
{ "timestamp": "2026-11-22T10:15:30", "status": 409, "error": "Conflict",
  "mensaje": "Ya existe un producto con código MC001", "ruta": "/api/v1/productos" }
```

(`detalles` aparece solo en los 400 de validación, con el error de cada campo).

### Categorías

| Método | Ruta | Acceso | Respuestas |
|---|---|---|---|
| GET | `/api/v1/categorias` | Público | 200 |
| GET | `/api/v1/categorias/{id}` | Público | 200, 404 |
| POST | `/api/v1/categorias` | ADMINISTRADOR | 201, 400, 401, 403, 409 |
| PUT | `/api/v1/categorias/{id}` | ADMINISTRADOR | 200, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/categorias/{id}` | ADMINISTRADOR | 204, 401, 403, 404, 409 |

### Productos

| Método | Ruta | Acceso | Respuestas |
|---|---|---|---|
| GET | `/api/v1/productos?categoria={id}&q={texto}` | Público (token opcional) | 200 |
| GET | `/api/v1/productos/{codigo}` | Público (token opcional) | 200, 404 |
| POST | `/api/v1/productos` | ADMINISTRADOR | 201, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/productos/{codigo}` | ADMINISTRADOR | 200, 400, 401, 403, 404 |
| DELETE | `/api/v1/productos/{codigo}` | ADMINISTRADOR | 204, 401, 403, 404 |

`precioCompra` solo aparece si el token es de ADMINISTRADOR. `stockBajo` es `true` cuando `stock <= stockMinimo`.

### Interno (solo ms-ventas)

No se expone por nginx y exige el header `X-API-KEY`.

| Método | Ruta | Respuestas |
|---|---|---|
| POST | `/api/v1/interno/stock/descuento` | 200, 400, 401, 404, 409 |
| POST | `/api/v1/interno/stock/reposicion` | 204, 400, 401 |

Cuerpo de ambos: `{"items": [{"codigo": "MC001", "cantidad": 2}]}`.

Un token inválido o expirado responde **401 en cualquier endpoint**, también en los públicos, para que el front renueve la sesión.

## Reglas de negocio

- El código de producto es único y se guarda en mayúsculas (409 si existe). No se puede cambiar.
- Precio de venta mayor a 0; precio de compra, stock y stock mínimo mayores o iguales a 0.
- La categoría del producto debe existir (404). No se elimina una categoría que tiene productos (409).
- **Descuento atómico de stock**: `UPDATE ... WHERE stock >= cantidad` dentro de una transacción. Si un ítem no alcanza, no se descuenta ninguno (todo o nada). Dos compras simultáneas de la última unidad nunca la venden dos veces.
- El descuento devuelve el precio de venta actual de cada producto: es la única fuente de precios de la boleta.
- La reposición omite los productos que ya no existen.

## Seguridad

- ms-catalogo no emite tokens: verifica la firma del JWT con el secreto compartido, exige el emisor `ferreteria-ms-usuarios` y el tipo `access`, y lee el rol.
- 401 = no se sabe quién eres (sin token, o token inválido o vencido). 403 = el token es válido pero el rol no tiene permiso.
- La clave interna se compara en tiempo constante (`MessageDigest.isEqual`), antes de leer el cuerpo de la petición.
- No hay CORS: front y API salen del mismo origen a través de nginx.

## Tests

```bash
./mvnw test
```

143 tests, con H2 en memoria y secretos de prueba (no necesitan MySQL ni el `.env`):

| Test | Qué prueba |
|---|---|
| `CategoriaServiceTest`, `ProductoServiceTest`, `StockServiceTest` | Reglas de negocio, con Mockito y sin base de datos |
| `ProductoEntradaTest` | Validaciones de los datos de un producto |
| `ProductoRepositoryTest` | Descuento y reposición atómicos, búsqueda con filtros |
| `CategoriaControllerTest`, `ProductoControllerTest`, `StockInternoControllerTest` | Integración por HTTP con los 85 productos, incluido el rollback real del descuento |
| `SeguridadBaseTest`, `ClaveInternaValidadorTest` | 401 y 403, tokens vencidos, de otro emisor o de refresh, y clave interna |
| `CargaDatosInicialesTest` | Carga de las 9 categorías y 85 productos sin duplicar |
| `OpenApiContratoTest` | Swagger tiene los 12 endpoints del contrato con todos sus códigos de respuesta |
