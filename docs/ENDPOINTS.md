# Contrato de endpoints

## `POST /auth/login`

Consume y produce `application/json`.

### Solicitud

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

`username` y `password` son obligatorios: se rechazan si son nulos o quedan
vacíos después de aplicar `trim()` durante la validación. `client` puede
omitirse y no participa en la autenticación.

### Respuestas

- `200 OK`: objeto con `token` (string), `expiresIn` (segundos), `user` con
  `username`, `displayName` y `role`, y el campo `debug`.
- `400 Bad Request`: objeto `{ "message": "..." }` si falta la solicitud, el
  nombre de usuario o la contraseña.
- `401 Unauthorized`: objeto `{ "message": "..." }` cuando las credenciales
  no coinciden; el mensaje incluye el nombre de usuario recibido.
- `500 Internal Server Error`: objeto con `message` y `exception` cuando la
  ejecución del recurso lanza una excepción.

El endpoint de salud indicado por el README es `GET /q/health` (Quarkus
SmallRye Health).