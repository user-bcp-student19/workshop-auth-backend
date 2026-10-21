# Proposal

## Why

Fuente del trabajo comprometido: [Issue #12, HU-01 - Token firmado con expiracion y claims minimos](https://github.com/user-bcp-student19/workshop-auth-backend/issues/12), consultado mediante GitHub oficial, sin comentarios al momento de la consulta.

El contrato del Dia 1 en `docs/ENDPOINTS.md` describe un token firmado con `sub`, `role`, `iat` y `exp`, pero `client` se acepta sin utilizarse. El Issue exige identificar el cliente en el token, comprobar la coherencia temporal y aportar evidencia de que secretos y tokens no se exponen en logs. Esto importa para que la identidad y vigencia del token sean verificables y su emision no filtre credenciales.

## What Changes

### Dentro del alcance

- Validar `client` recibido y reflejarlo en el token; exigir los claims `sub`, `client`, `iat` y `exp`, conservando `role`.
- Verificar que `expiresIn` sea coherente con `exp - now`, con una tolerancia documentada, y cubrir token expirado.
- Documentar `AUTH_TOKEN_SECRET`: sin secreto por defecto fuera del sandbox; cualquier valor de laboratorio debe identificarse como exclusivo de ese entorno.
- No registrar tokens, secretos, contrasenas ni otros valores sensibles en los flujos de autenticacion.
- Definir evidencia verificable de login valido, credenciales invalidas, claims, expiracion y logs redactados.

### Fuera del alcance

- Disenar clases, capas, algoritmos, dependencias o mecanismos de prueba; elegir HMAC frente a una libreria JWT.
- Implementar el cambio, crear `design.md` o `tasks.md`, ejecutar nuevas pruebas funcionales o modificar `docs/ENDPOINTS.md` en esta fase.
- Agregar endpoints de validacion, refresh, logout, revocacion, autorizacion o nuevos checks de salud.
- Cambiar persistencia, contrasenas, roles, CORS o corregir las exposiciones actuales de los cuerpos `401` y `500` ajenas al Issue.

### Contrato preservado y excepciones

Baseline: `docs/ENDPOINTS.md`, no el comportamiento futuro inferido del Issue. Se preservan ruta, metodo, ausencia de autenticacion previa, negociacion JSON, validacion y uso original de `username` y `password`, respuestas de error y estructura publica de exito. Se mantienen `role`, el escape HTML de `user.displayName`, `debug`, la duracion actual de 3600 segundos y health/CORS.

La diferencia autorizada por el Issue es que `client` deja de ser ignorado y pasa a validarse y formar parte del token. El Issue por si solo no fija obligatoriedad ni restricciones exactas; D2 en `design.md` registra la decision aprobada para esta historia y la excepcion de compatibilidad para clientes que antes lo omitian. El formato compacto HS256 es parte del contrato aprobado; la eleccion de herramienta de construccion no cambia ese contrato.

Decision reconciliada con `design.md` D2: `client` es obligatorio, string, no vacio tras `trim()`, y el valor original aceptado se conserva en el claim. La ausencia, null o valor blanco produce `400` con `{"message":"Client is required"}`; la validacion ocurre despues de username/password y antes de consultar credenciales. Esta es la excepcion de contrato aprobada para clientes que antes omitian `client`.

## Capabilities

### New Capabilities

- `signed-auth-token`: claims, validacion de client, coherencia temporal, expiracion, secreto y confidencialidad de logs del Issue #12.
- `auth-http-contract`: caracterizacion del contrato del Dia 1 que debe preservarse al introducir los ajustes del Issue #12.

Se registran como nuevas capacidades OpenSpec porque no existen specs previas bajo `openspec/specs/`; no representan dos nuevas APIs.

### Modified Capabilities

Ninguna capacidad OpenSpec previa. El contrato HTTP existente se referencia expresamente en las specs nuevas.

## Impact

- API afectada: `POST /auth/login`, entrada `client` y contenido del token. Consumidores que omiten `client` podrian verse afectados si se acuerda hacerlo obligatorio.
- Contratos protegidos: respuesta de login, errores, CORS y `GET /q/health` del baseline. Sin nuevas dependencias ni cambios funcionales en esta entrega.
- Configuracion y operacion: documentacion de `AUTH_TOKEN_SECRET` por entorno y evidencia de logs sin valores sensibles.
- OpenSpec: no habia raiz local configurada; se declara unicamente el schema estandar `spec-driven`, sin personalizar su flujo.

### Supuestos y decisiones pendientes

- `client` identifica al consumidor, no autentica al usuario ni concede permisos; el Issue no solicita un registro de clientes.
- Decisiones reconciliadas con `design.md`: `client` obligatorio como string no blanco tras `trim()`, preservando el valor original; no se agrega whitelist, normalizacion ni limite de longitud. Un valor JSON de tipo incompatible conserva la respuesta de deserializacion del framework.
- Decisiones temporales reconciliadas con `design.md` D3: `exp - iat = 3600`; `now` se observa al recibir la respuesta con reloj UTC compartido; tolerancia de observacion y presupuesto maximos de 2 segundos. Una observacion tardia incumple el presupuesto y no amplia la tolerancia; no existe gracia en `exp`.
- Decision tecnica registrada en `design.md` D1: usar Quarkus SmallRye JWT Build alineado con el BOM, algoritmo HS256 explicito y sin extension bearer. Si la prueba de compatibilidad falla, detenerse y revisar la decision; no cambiar algoritmo o contrato silenciosamente.
- Debe identificarse como sandbox cualquier entorno con secreto de laboratorio; no se presupone que un despliegue sea sandbox.
- `POST /q/health` y la negociacion de health siguen siendo observaciones/preguntas del baseline, no nuevos compromisos de esta historia.

### Riesgos y mitigaciones

- Ruptura involuntaria de clientes: acordar la politica de `client` antes del diseno y contrastar las respuestas restantes con la spec de preservacion.
- Pruebas temporales ambiguas: fijar tolerancia numerica y referencia de reloj antes de implementar; no aceptar una tolerancia indefinida como evidencia.
- Fuga en evidencias o configuracion: usar valores sinteticos, redactar valores sensibles y comprobar separacion de sandbox y otros entornos.

### Criterios de aceptacion y evidencia

1. Un login valido produce token verificable con `sub`, `client`, `iat`, `exp` y el `role` existente, sin alterar la respuesta publica restante.
2. Se prueba `exp - iat = 3600`, `expiresIn = 3600` y una diferencia de observacion de como maximo 2 segundos usando reloj UTC compartido y presupuesto maximo de 2 segundos; una observacion tardia se informa como presupuesto incumplido, y un token en o despues de `exp` no es valido.
3. Se conservan los estados, cuerpos y reglas de entrada del baseline salvo la validacion aprobada de `client`: ausente, null o blanco devuelve `400` con `{"message":"Client is required"}` despues de validar username/password y antes de consultar credenciales.
4. La configuracion documentada no permite usar un secreto por defecto fuera del sandbox; los logs revisados no contienen tokens, secretos ni valores sensibles.

Validacion futura: pruebas de claims, firma, reloj y expiracion; pruebas funcionales de login valido, credenciales invalidas y regresion del contrato; comprobacion de configuracion y logs de exito/error con valores sinteticos. La evidencia incluira resultados de pruebas, tolerancia y reloj utilizados, configuracion por entorno sin secretos y logs redactados. Se documentara la decision HMAC/JWT en la fase de diseno. Esta entrega solo valida los artefactos OpenSpec y se detiene para revision humana; las preguntas anteriores bloquean cerrar la especificacion para implementar.