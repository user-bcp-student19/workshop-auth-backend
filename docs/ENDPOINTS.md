# API de autenticacion

URL base local: `http://localhost:8080` (configurable por entorno). Las
respuestas de la API se sirven como JSON, salvo donde se indica que el cuerpo
de error está vacío.

## `POST /auth/login`

### 1. Que hace

Autentica a un usuario del backend de laboratorio y, si las credenciales son
válidas, devuelve sus datos públicos y un token de acceso firmado.

### 2. Autenticacion

No requiere autenticación previa. No enviar `Authorization`.

Enviar `Content-Type: application/json`; se recomienda
`Accept: application/json`.

### 3. Request

| campo | tipo | obligatorio | descripcion |
|---|---|---|---|
| `username` | string | sí | Nombre de usuario. Se rechaza si es nulo o queda vacío después de `trim()`. El valor sin recortar se usa para buscar al usuario. |
| `password` | string | sí | Contraseña. Se rechaza si es nula o queda vacía después de `trim()`; el valor original se compara con el hash almacenado. |
| `client` | string | sí | Identifica al cliente que solicita el login. Se rechaza si falta, es null o queda vacío después de `trim()`. Se conserva el valor original aceptado en el claim del token; no se devuelve como campo de primer nivel. |

Ejemplo completo con el usuario semilla local documentado en `README.md`:

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

### 4. Response exitosa

**Estado:** `200 OK`, `Content-Type: application/json`.

| campo | tipo | descripcion |
|---|---|---|
| `token` | string | JWT compacto firmado con HS256. Incluye los claims `sub`, `client`, `role`, `iat` y `exp`. |
| `expiresIn` | integer | Duración del token en segundos; actualmente `3600`. |
| `user` | object | Datos del usuario autenticado. |
| `user.username` | string | Nombre de usuario. |
| `user.displayName` | string | Nombre visible; el recurso escapa caracteres HTML antes de devolverlo. |
| `user.role` | string | Rol asociado al usuario, por ejemplo `STUDENT`. |
| `debug` | string | Valor actual fijo: `authenticated-with-legacy-flow`. |

Ejemplo completo capturado en una ejecución local:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzdHVkZW50Iiwicm9sZSI6IlNUVURFTiIsImV4cCI6MTc5MjQ0OTk0OCwiaWF0IjoxNzkyNDQ2MzQ4fQ.tisTNZ3Q275lAeGHj3QmJ9GxSGflUHBo7skJyylM5Ew",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

El token del ejemplo fue firmado con una clave temporal local y expira; es
El valor completo del token se omite del ejemplo; no copiar tokens de logs o
respuestas como credenciales.

### 5. Respuestas de error

**`400 Bad Request` — solicitud JSON nula**

Ocurre si el cuerpo JSON es `null`.

```json
{"message":"Request is null"}
```

**`400 Bad Request` — nombre de usuario ausente o vacío**

Ocurre si `username` es nulo, falta o contiene solo espacios.

```json
{"message":"Username is required"}
```

**`400 Bad Request` — contraseña ausente o vacía**

Ocurre si `password` es nula, falta o contiene solo espacios.

```json
{"message":"Password is required"}
```

**`400 Bad Request` — cliente ausente o vacío**

Ocurre después de validar username y password si `client` falta, es null o
contiene solo espacios. No se consulta la base ni se emite token.

```json
{"message":"Client is required"}
```

Si `client` tiene un tipo JSON que no puede deserializarse como string, se
conserva el error del framework de deserialización.

**`400 Bad Request` — JSON mal formado**

Ocurre antes de invocar el recurso cuando el cuerpo no se puede deserializar.
El servidor devuelve un cuerpo vacío (0 bytes), no JSON.

**`401 Unauthorized` — credenciales no autenticadas**

Ocurre si no se encuentra el usuario o no coincide la contraseña. Un error de
consulta SQLite también puede verse como `401`: `LegacyDatabase` captura el
error de consulta y devuelve `null` al recurso.

```json
{"message":"Invalid user student or password"}
```

**`403 Forbidden` — preflight CORS de origen no permitido**

Ocurre en una solicitud CORS preflight (`OPTIONS`) si el origen no está en la
lista configurada. El cuerpo está vacío (0 bytes); no se devuelve JSON.

**`405 Method Not Allowed` — método no registrado para login**

Ocurre, por ejemplo, al enviar `GET /auth/login` en lugar de `POST`. El cuerpo
está vacío (0 bytes).

**`406 Not Acceptable` — tipo de respuesta no aceptable**

Ocurre si `Accept` excluye `application/json` (por ejemplo,
`Accept: text/plain`). El cuerpo está vacío (0 bytes).

**`415 Unsupported Media Type` — tipo de solicitud no compatible**

Ocurre si el cuerpo no se envía como `application/json`. El cuerpo está vacío
(0 bytes).

**`500 Internal Server Error` — excepción durante el login**

Ocurre si el recurso lanza una excepción; por ejemplo, si la clave de firma
configurada tiene menos de 32 bytes. La respuesta expone el mensaje y la clase
de la excepción:

```json
{
  "exception": "java.lang.IllegalStateException",
  "message": "Authentication token secret must contain at least 32 bytes"
}
```

Para `AUTH_TOKEN_SECRET` menor de 32 bytes, se devuelve
`IllegalStateException` con el mensaje documentado. Si la variable no está
configurada, la emisión falla con un mensaje fijo que no incluye la clave.
Otros errores del recurso conservan el mensaje y clase actuales; el detalle de
excepciones no forma parte del cambio de claims.

### 6. Notas de seguridad

- El endpoint no requiere sesión ni token para emitir un token.
- El JWT está firmado, no cifrado: sus claims son legibles. Tiene `exp` y la
  duración devuelta es de una hora.
- El secreto de firma se lee de `AUTH_TOKEN_SECRET` en los perfiles dev/prod y
- El secreto de firma se lee de `AUTH_TOKEN_SECRET` y debe tener al menos 32
  bytes UTF-8. No hay fallback de secreto en producción. No incluir secretos
  reales en solicitudes, ejemplos ni control de versiones.
- El baseline describe CORS por orígenes configurados, pero
  `application.properties` actualmente permite cualquier origen (`/.*/`).
  Esta discrepancia queda pendiente fuera del alcance de HU-01; no se declara
  corregida por este cambio.
- El mensaje `401` incluye el nombre de usuario recibido y el `500` devuelve
  detalles internos; ambos son comportamientos actuales del código.
- Los campos, estados y cuerpos descritos se verifican en
- Los campos y estados se verifican en `AuthResource.java`, la orquestación en
  `AuthenticationService.java`, la emisión en `SignedTokenIssuer.java` y la
  configuración de tests/compilación en `pom.xml`.

## `GET /q/health`

### 1. Que hace

Permite a un operador o plataforma de despliegue comprobar que el proceso
Quarkus está respondiendo como saludable.

### 2. Autenticacion

No requiere autenticación. No enviar `Authorization`.

### 3. Request

| campo | tipo | obligatorio | descripcion |
|---|---|---|---|
| cuerpo | ninguno | no | La comprobación no recibe campos ni cuerpo JSON. |

Ejemplo completo de solicitud HTTP (GET sin cuerpo):

```http
GET /q/health HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### 4. Response exitosa

**Estado:** `200 OK`, `Content-Type: application/json` cuando el estado de
salud es `UP`.

| campo | tipo | descripcion |
|---|---|---|
| `status` | string | Estado agregado de salud. En la configuración actual es `UP`. |
| `checks` | array de object | Comprobaciones registradas. El backend actual no registra checks propios y devuelve una lista vacía. |

Ejemplo observado en la instancia actual:

```json
{
  "status": "UP",
  "checks": []
}
```

### 5. Respuestas de error

**`403 Forbidden` — preflight CORS de origen no permitido**

Ocurre si una solicitud CORS preflight `OPTIONS /q/health` viene de un origen
que no está configurado. El cuerpo está vacío (0 bytes).

No se observó una respuesta de error propia para `GET /q/health`; no hay checks
de salud del backend que puedan devolver estado `DOWN` en la configuración
actual.

### 6. Notas de seguridad

- La comprobación es pública y no verifica autenticación ni autorización.
- El endpoint lo proporciona `quarkus-smallrye-health` (dependencia en
  `pom.xml:40`), no un recurso Java del backend.
- En la instancia inspeccionada, `POST /q/health` también devolvió `200` y
  `application/json`, pese a que la integración documentada debe usar GET.
  **Pregunta abierta:** confirmar si ese comportamiento por método debe
  conservarse o si se espera restringir health a GET.
- `GET /q/health` devolvió JSON incluso con `Accept: text/plain`; no hay
  negociación de contenido documentada por un recurso propio.
- La política CORS y sus orígenes son globales y están en
  `src/main/resources/application.properties:4-9`.

## Documentos y fuentes consultados

- `README.md` — URL local, credenciales semilla y rutas anunciadas.
- `docs/GOOD_PRACTICES.md` y `REGLAS.md` — requisitos de seguridad y del
  laboratorio.
- `src/main/java/com/bcp/workshop/auth/AuthResource.java` — request, respuestas,
  validación y campos JSON de login.
- `src/main/java/com/bcp/workshop/auth/LoginRequest.java` — campos de entrada.
- `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` — autenticación y
  usuario de laboratorio.
- `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` — firma, claims,
  secreto y expiración del token.
- `src/main/resources/application.properties` y `pom.xml` — CORS, configuración
  y dependencia de health.
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y
  `src/test/java/com/bcp/workshop/auth/AuthCorsTest.java` — contrato esperado y
  CORS.
- Respuestas HTTP observadas localmente durante la actualización de este
  documento.