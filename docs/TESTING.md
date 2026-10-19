# Pruebas de endpoints

Esta matriz corresponde al contrato actual de `docs/ENDPOINTS.md`. Los
resultados esperados no fijan el comportamiento abierto de `POST /q/health` ni
convierten fallos internos de SQLite en una respuesta deseada.

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `POST /auth/login` | `logsInWithWorkshopUser` | Camino feliz | HTTP 200; username, displayName, role, token string, expiresIn 3600 y debug presentes. |
| `POST /auth/login` | `acceptsRequestWithoutOptionalClient` | Borde | HTTP 200 sin el campo opcional `client`. |
| `POST /auth/login` | `storesSeedPasswordAsSaltedHash` | Borde | La contraseña seed está almacenada como hash PBKDF2 con salt, no en texto plano. |
| `POST /auth/login` | `rejectsNullRequestBody` | Error 400 | `{"message":"Request is null"}`. |
| `POST /auth/login` | `rejectsMissingUsername` | Error 400 | `{"message":"Username is required"}`. |
| `POST /auth/login` | `rejectsBlankUsername` | Error 400 | `{"message":"Username is required"}`. |
| `POST /auth/login` | `rejectsMissingPassword` | Error 400 | `{"message":"Password is required"}`. |
| `POST /auth/login` | `rejectsBlankPassword` | Error 400 | `{"message":"Password is required"}`. |
| `POST /auth/login` | `rejectsMalformedJsonWithEmptyBody` | Error 400 | Cuerpo vacío. |
| `POST /auth/login` | `rejectsInvalidCredentialsWithUnauthorizedStatus` | Error 401 | HTTP 401 y mensaje de credenciales inválidas para contraseña incorrecta. |
| `POST /auth/login` | `rejectsUnknownUserWithUnauthorizedStatus` | Error 401 | HTTP 401 y mensaje con el username inexistente. |
| `POST /auth/login` | `allowsConfiguredOriginForLoginPreflight` | Borde / CORS | El preflight del origen configurado recibe HTTP 200 y `Access-Control-Allow-Origin`. |
| `POST /auth/login` | `rejectsUnconfiguredOriginForLoginPreflight` | Error 403 | Preflight de origen no permitido; cuerpo vacío. |
| `POST /auth/login` | `rejectsUnsupportedHttpMethodWithEmptyBody` | Error 405 | `GET /auth/login`; cuerpo vacío. |
| `POST /auth/login` | `rejectsUnacceptableResponseTypeWithEmptyBody` | Error 406 | `Accept: text/plain`; cuerpo vacío. |
| `POST /auth/login` | `rejectsUnsupportedRequestTypeWithEmptyBody` | Error 415 | `Content-Type: text/plain`; cuerpo vacío. |
| `POST /auth/login` | `returnsInternalServerErrorWhenSigningSecretIsTooShort` | Error 500 | HTTP 500 con `message` y `exception` al usar una clave de test menor a 32 bytes. |
| `POST /auth/login` | `migratesExistingPasswordsOnceAndVerifiesCredentials` | Borde / persistencia | Migra una contraseña heredada, conserva el login válido, rechaza la incorrecta y no repite la migración al reinicializar. |
| `POST /auth/login` | `createsDifferentSaltedHashesForSamePassword` | Borde / seguridad | Dos hashes para la misma contraseña usan salts distintos. |
| `POST /auth/login` | `verifiesOnlyTheMatchingPassword` | Borde / seguridad | Verifica la contraseña correcta y rechaza otra. |
| `POST /auth/login` | `rejectsMalformedHashesAndNullPasswords` | Error / seguridad | `verify` devuelve false para hashes malformados, parámetros fuera de rango y entradas nulas; `hash(null)` lanza `IllegalArgumentException`. |
| `POST /auth/login` | `createsSignedTokenWithExplicitClaimsAndNoEmbeddedSecret` | Camino feliz / seguridad | El token tiene tres segmentos, firma HS256 verificable, claims sub/role/iat/exp y no contiene la clave. |
| `POST /auth/login` | `signingKeyChangesTokenSignature` | Borde / seguridad | Cambiar la clave modifica la firma. |
| `POST /auth/login` | `rejectsMissingOrWeakSigningKeys` | Error / seguridad | Una clave menor a 32 bytes es rechazada. |
| `GET /q/health` | `reportsUpWithoutAuthentication` | Camino feliz | HTTP 200, JSON y `status: UP` sin header Authorization. |
| `GET /q/health` | `reportsEmptyChecksWhenNoHealthChecksAreRegistered` | Borde | HTTP 200 y `checks: []` cuando no hay checks registrados. |
| `GET /q/health` | `rejectsUnconfiguredOriginForHealthPreflight` | Error 403 | Preflight de origen no permitido; cuerpo vacío. |

## Código ejecutable de las pruebas

El código fuente ejecutable está en estos archivos; los nombres de métodos de la
tabla corresponden a los casos que contienen:

- [AuthResourceTest.java](../src/test/java/com/bcp/workshop/auth/AuthResourceTest.java) — contrato, casos borde y errores 400/401/405/406/415.
- [AuthCorsTest.java](../src/test/java/com/bcp/workshop/auth/AuthCorsTest.java) — preflight CORS permitido y rechazado para login y health.
- [HealthEndpointTest.java](../src/test/java/com/bcp/workshop/auth/HealthEndpointTest.java) — health público y lista de checks vacía.
- [AuthResourceWeakSecretTest.java](../src/test/java/com/bcp/workshop/auth/AuthResourceWeakSecretTest.java) — respuesta HTTP 500 con perfil aislado de clave débil.
- [LegacyDatabaseTest.java](../src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java) — migración SQLite e idempotencia.
- [PasswordHasherTest.java](../src/test/java/com/bcp/workshop/auth/PasswordHasherTest.java) — salt, verificación y hashes inválidos.
- [LegacyTokenUtilTest.java](../src/test/java/com/bcp/workshop/auth/LegacyTokenUtilTest.java) — estructura, claims y firma del token.

## Comandos y resultados ejecutados

Directorio de trabajo: raíz del repositorio `workshop-auth-backend/`.

| comando | código de salida | resultado |
|---|---:|---|
| `mvn '-Dtest=AuthResourceTest,AuthCorsTest,HealthEndpointTest,AuthResourceWeakSecretTest' test` (primer intento) | 1 | Compilación detenida por un literal JSON mal escapado en `AuthResourceTest`; no se ejecutaron pruebas. |
| `mvn '-Dtest=AuthResourceTest,AuthCorsTest,HealthEndpointTest,AuthResourceWeakSecretTest,LegacyTokenUtilTest,LegacyDatabaseTest,PasswordHasherTest' test` | 0 | 27 pruebas; 0 fallos, 0 errores, 0 omitidas. |
| `mvn verify` | 0 | 27 pruebas; `BUILD SUCCESS`; reporte JaCoCo generado. |

### Medición de cobertura

Método: cobertura de líneas JaCoCo, calculada como
`covered / (covered + missed)` a partir de `target/site/jacoco/jacoco.xml`,
generado por `mvn verify`. Reporte HTML: `target/site/jacoco/index.html`.

| clase de producción | líneas cubiertas | cobertura |
|---|---:|---:|
| `AuthResource` | 25/25 | 100.0% |
| `LegacyDatabase` | 53/60 | 88.3% |
| `PasswordHasher` | 30/32 | 93.8% |
| `LegacyTokenUtil` | 24/26 | 92.3% |
| **Bundle completo** | **135/146** | **92.5%** |

Las cuatro clases de autenticación medidas superan el objetivo de 85% por
líneas. El bundle incluye 7 clases instrumentadas; Quarkus SmallRye Health es
una dependencia y no una clase fuente del proyecto.

## Fallos

- La última ejecución focalizada y `mvn verify` no tuvieron fallos ni errores.
- Durante la edición hubo un intento de compilación fallido por un literal JSON
  mal escapado en una prueba; se corrigió antes de la ejecución registrada
  arriba. No queda como fallo pendiente.

## Pendientes

- `GET /q/health` también respondió `200` ante `POST` y devolvió JSON con
  `Accept: text/plain` durante la verificación manual. No se añadieron tests que
  fijen esos comportamientos porque `POST` no forma parte del contrato de
  integración y el método no está restringido actualmente.
- `LegacyDatabase` captura errores de consulta y retorna `null`, que el recurso
  transforma en `401`. No hay prueba que presente una caída de SQLite como
  credenciales inválidas correctas; se registra como discrepancia de manejo de
  errores, no como comportamiento deseable.
- No se probó un estado health `DOWN`: el backend no registra checks propios y
  el contrato actual solo documenta `UP` y el preflight CORS rechazado.

## Pruebas propuestas

- Acordar si health debe admitir métodos distintos de GET y negociar el header
  `Accept`; después añadir tests de contrato para la decisión, sin asumir el
  comportamiento actual como deseado.
- Definir el status de una falla de persistencia y añadir una prueba con una
  base inaccesible cuando deje de confundirse con `401` por credenciales.
- Añadir un health check concreto solo si el backend incorpora una dependencia
  cuya disponibilidad deba afectar el estado agregado; entonces cubrir `DOWN`.