# Spec Delta

## Purpose

Proteger el comportamiento HTTP documentado el Dia 1 en `docs/ENDPOINTS.md` mientras se aplican los ajustes del Issue #12. No se elevan observaciones abiertas de health a requisitos nuevos.

## ADDED Requirements

### Requirement: Entrada de login compatible

El servicio SHALL conservar `POST /auth/login` sin autenticacion previa y con solicitud y respuesta JSON. `username` y `password` SHALL rechazarse si faltan, son nulos o quedan vacios tras `trim()`, pero sus valores originales SHALL usarse para buscar al usuario y comprobar la contrasena. Solo `client` puede cambiar conforme a la spec `signed-auth-token` y a sus decisiones pendientes.

#### Scenario: Login sin Authorization
- **WHEN** se envia un login valido, con `client` aceptado, `Content-Type: application/json` y sin `Authorization`
- **THEN** la autenticacion puede completarse sin exigir una sesion o token previo

#### Scenario: Se conservan valores originales de credenciales
- **WHEN** `username` o `password` contienen espacios alrededor de un valor no vacio
- **THEN** se usan los valores originales y no se autentican automaticamente como si esos espacios hubieran sido eliminados

### Requirement: Respuesta exitosa preservada

Un login valido SHALL conservar `200 OK`, `Content-Type: application/json`, `token`, `expiresIn`, `user.username`, `user.displayName`, `user.role` y `debug`. `user.displayName` SHALL mantener el escape HTML y `debug` SHALL seguir siendo `authenticated-with-legacy-flow`. Los ajustes de claims y validacion de `client` no autorizan eliminar campos existentes ni devolver `client` como nuevo campo de primer nivel.

#### Scenario: Estructura de exito del Dia 1
- **WHEN** se autentica un usuario valido con `client` aceptado
- **THEN** la respuesta es `200` JSON con los campos del baseline y `expiresIn` entero igual a 3600
- **AND** los datos publicos corresponden al usuario y `debug` es `authenticated-with-legacy-flow`

#### Scenario: Nombre visible con HTML
- **WHEN** un usuario valido tiene caracteres HTML en su nombre visible
- **THEN** `user.displayName` conserva el escape HTML documentado

### Requirement: Errores de login preservados

El servicio SHALL conservar los siguientes estados y cuerpos del baseline. Para caracterizar errores de credenciales se utilizara un `client` aceptado; no se define aqui la precedencia entre nuevas validaciones de `client` y errores existentes. Cambiar mensajes `401` o detalles de `500` no forma parte del Issue #12.

#### Scenario: Cuerpo JSON null
- **WHEN** el cuerpo JSON es `null`
- **THEN** la respuesta es `400` con `{"message":"Request is null"}`

#### Scenario: Username ausente o vacio
- **WHEN** `username` falta, es nulo o contiene solo espacios
- **THEN** la respuesta es `400` con `{"message":"Username is required"}`

#### Scenario: Password ausente o vacia
- **WHEN** con `username` no vacio, `password` falta, es nula o contiene solo espacios
- **THEN** la respuesta es `400` con `{"message":"Password is required"}`

#### Scenario: Credenciales invalidas
- **WHEN** con `client` aceptado, el usuario `student` no se autentica por usuario inexistente o contrasena incorrecta
- **THEN** la respuesta es `401` con `{"message":"Invalid user student or password"}` y no se devuelve token

#### Scenario: Error de consulta de autenticacion
- **WHEN** una consulta SQLite falla y el flujo actual no obtiene un usuario autenticado
- **THEN** se conserva el resultado `401` documentado, sin convertir esta historia en una correccion de persistencia

#### Scenario: Error de secreto demasiado corto
- **WHEN** un login con credenciales validas y `client` aceptado llega a emitir token con secreto configurado de menos de 32 bytes
- **THEN** se conserva `500` con `exception` igual a `java.lang.IllegalStateException` y `message` igual a `Authentication token secret must contain at least 32 bytes`

### Requirement: Errores de protocolo y CORS preservados

El servicio SHALL conservar la negociacion de contenido, metodos y CORS documentados, incluidos los cuerpos vacios de los siguientes errores.

#### Scenario: JSON mal formado
- **WHEN** el cuerpo JSON no se puede deserializar
- **THEN** la respuesta es `400` con cuerpo de 0 bytes

#### Scenario: Metodo no registrado
- **WHEN** se envia `GET /auth/login`
- **THEN** la respuesta es `405` con cuerpo de 0 bytes

#### Scenario: Respuesta no aceptable
- **WHEN** se solicita login con `Accept` que excluye `application/json`
- **THEN** la respuesta es `406` con cuerpo de 0 bytes

#### Scenario: Tipo de solicitud incompatible
- **WHEN** el cuerpo de login se envia con un tipo distinto de `application/json`
- **THEN** la respuesta es `415` con cuerpo de 0 bytes

#### Scenario: Preflight de origen no permitido
- **WHEN** un origen fuera de la lista configurada solicita preflight `OPTIONS` para `/auth/login` o `/q/health`
- **THEN** la respuesta es `403` con cuerpo de 0 bytes y no se amplia la lista CORS por este cambio

### Requirement: Health publico sin cambios

El servicio SHALL conservar `GET /q/health` publico, sin nuevos checks propios ni requisitos de autenticacion introducidos por esta historia. Las observaciones de `POST /q/health` y `Accept: text/plain` no se resuelven en este cambio.

#### Scenario: Health del baseline
- **WHEN** el proceso esta saludable y se solicita `GET /q/health` con `Accept: application/json` sin `Authorization`
- **THEN** la respuesta es `200` JSON con `{"status":"UP","checks":[]}`