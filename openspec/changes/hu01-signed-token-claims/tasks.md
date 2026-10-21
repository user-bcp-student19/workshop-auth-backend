# Tasks

Unidad de trabajo del producto: [GitHub Issue #12](https://github.com/user-bcp-student19/workshop-auth-backend/issues/12). Este archivo es exclusivamente su checklist de implementacion para el cambio OpenSpec `hu01-signed-token-claims`; no crea historias adicionales ni autoriza ampliar el alcance.

Referencias aprobadas: [proposal.md](proposal.md), [signed-auth-token](specs/signed-auth-token/spec.md), [auth-http-contract](specs/auth-http-contract/spec.md) y [design.md](design.md). Baseline protegido: [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md).

Ejecutar en orden de dependencia. Completar la seccion 1 antes de modificar token, flujo de autenticacion o manejadores de logs; completar la seccion 2 antes del cambio funcional. Todas las casillas empiezan pendientes: crear este checklist no acredita pruebas ejecutadas ni implementacion terminada. Reutilizar las pruebas y utilidades existentes; cada comprobacion debe dejar evidencia sin valores sensibles.

## 1. Caracterizacion previa al cambio funcional

- [ ] 1.1 Preparar aislamiento de pruebas con SQLite temporal y datos sinteticos, reutilizando JUnit/RestAssured; verificar que la prueba de login existente pasa sin leer ni modificar la base local del usuario y registrar el resultado sobre las fuentes actuales.
- [ ] 1.2 Ampliar la caracterizacion del login exitoso sin Authorization: comprobar `200`, JSON, campos y tipos de `user`, `token`, `expiresIn = 3600`, `role`, `debug` y escape HTML de displayName; verificar con la suite HTTP focalizada que las aserciones describen el comportamiento actual.
- [ ] 1.3 Caracterizar cuerpo null y username/password ausentes, nulos, vacios y blancos, ademas de credenciales con espacios preservados; verificar los `400` y mensajes exactos del baseline y que no se recortan los valores usados para autenticar.
- [ ] 1.4 Caracterizar usuario inexistente, contrasena incorrecta, fallo SQLite que devuelve `401` y captura general `500` mediante fallos controlados; verificar estados/cuerpos y ausencia de token sin modificar consultas ni semantica de persistencia.
- [ ] 1.5 Caracterizar JSON mal formado, GET login, Accept y Content-Type incompatibles; verificar estados `400/405/406/415` y cuerpos de 0 bytes mediante pruebas HTTP focalizadas.
- [ ] 1.6 Caracterizar health publico y preflight CORS para ambos endpoints; registrar por separado la diferencia entre CORS actual y baseline, con resultado esperado/observado. Verificar que no se cambian origenes, no se silencian pruebas y no se declara conforme el caso discrepante.
- [ ] 1.7 Registrar el formato Base64 actual y la ausencia de configuracion de secreto como diferencias conocidas, y capturar errores sinteticos de inicializacion/consulta para analizar logs; verificar que la evidencia esta redactada, no contiene el secreto embebido y no establece el formato inseguro como contrato futuro.
- [ ] 1.8 Ejecutar la bateria de caracterizacion sobre fuentes actuales y conservar resultados nuevos, no informes heredados de `target/`; verificar que cada caso queda clasificado como contrato preservable o discrepancia conocida y que no quedan fallos sin explicar antes de refactorizar.

## 2. Reconciliacion de decisiones aprobadas

- [ ] 2.1 Actualizar las preguntas pendientes de propuesta/specs con D2 del diseno: client obligatorio, no blanco tras trim, valor original preservado, `400 {"message":"Client is required"}` y precedencia cuerpo/username/password/client/credenciales; verificar consistencia entre escenarios y diseno sin agregar whitelist, normalizacion o limite nuevo.
- [ ] 2.2 Reflejar D1 y D3 en las decisiones pendientes: SmallRye JWT Build con HS256 y tolerancia de observacion de 2 segundos con reloj UTC compartido, presupuesto maximo de 2 segundos y ninguna gracia tras exp; verificar con `openspec validate hu01-signed-token-claims --strict --no-interactive` y revision de trazabilidad al Issue, sin alterar otros contratos.

## 3. Emision HS256 aislada

- [ ] 3.1 Incorporar `quarkus-smallrye-jwt-build` alineado con el BOM existente, sin extension bearer; verificar resolucion/compilacion y una prueba acotada de construccion HS256. Si la version no lo soporta, detener y revisar D1, sin cambiar algoritmo o biblioteca silenciosamente.
- [ ] 3.2 Preparar el componente emisor con Clock UTC sustituible, una unica duracion de 3600 segundos y resultado interno de emision; verificar con reloj fijo que se captura un solo instante y que `exp - iat = 3600`, sin conectar aun el login al nuevo emisor.
- [ ] 3.3 Construir mediante la libreria los claims estructurados `sub`, `client`, `role`, `iat` y `exp`; verificar tipos, algoritmo HS256 y firma con JCA HmacSHA256 independiente, incluyendo rechazo de firma alterada y dos clients distintos para el mismo usuario.
- [ ] 3.4 Agregar comprobaciones deterministas de frontera temporal con firma correcta: antes de exp, exactamente en exp y despues; verificar expiracion sin sleeps, sin nuevo endpoint y sin afirmar que existe autorizacion HTTP implementada.
- [ ] 3.5 Revisar el emisor frente a D1/D3 y registrar evidencia focalizada; verificar que no hay JSON concatenado, protocolo JWT artesanal, claims/tiempos implicitos que cambien el contrato ni una segunda fuente de duracion. Mantener la bateria HTTP preservable sin regresiones.

## 4. Configuracion de secreto y operacion

- [ ] 4.1 Conectar el emisor a `AUTH_TOKEN_SECRET` mediante configuracion Quarkus, sin fallback embebido en dev/prod ni sandbox; verificar con configuracion aislada que una clave explicita de al menos 32 bytes UTF-8 permite firmar y que no se alteran propiedades CORS.
- [ ] 4.2 Validar presencia y longitud antes de emitir, permitiendo arrancar sin clave; verificar que ausencia y clave corta impiden emision, que clave corta lanza `IllegalStateException` con el mensaje exacto protegido y que ningun error incluye el valor de la clave.
- [ ] 4.3 Documentar configuracion por entorno y sandbox explicito, minimo en bytes UTF-8 y aprovisionamiento aislado de claves sinteticas de prueba; verificar que las instrucciones no incluyen secretos reales ni defaults reutilizables y explican que health UP no garantiza emision disponible.

## 5. Integracion incremental del login

- [ ] 5.1 Extraer solo la orquestacion local a un servicio y mantener traduccion HTTP/escape HTML en AuthResource, reutilizando LegacyDatabase sin modificar SQL ni almacenamiento; verificar que pasa la caracterizacion preservable antes de cambiar emision o politica de client.
- [ ] 5.2 Implementar la guarda de client acordada despues de username/password y antes de la consulta; verificar ausente/null/vacio/blanco, ausencia de consulta/token al rechazar, mensaje exacto, precedencia de errores y deserializacion no alterada por validadores automaticos.
- [ ] 5.3 Sustituir la emision legacy por el componente HS256 y obtener expiresIn del mismo resultado/duracion; verificar login valido, claims, client original incluidos espacios aceptados, dos clients distintos, respuesta publica sin client de primer nivel y retirada del emisor Base64/secreto embebido sin fallback.
- [ ] 5.4 Verificar conjuntamente credenciales invalidas con client valido, errores de clave corta y ausente, y health publico sin clave; comprobar `401` preservado, `500` de clave corta con clase/mensaje exactos y ningun token emitido en errores.
- [ ] 5.5 Agregar prueba HTTP de coherencia entre expiresIn y exp al observar respuesta con UTC compartido; verificar diferencia absoluta de como maximo 2 segundos, registrar instantes y presupuesto, y reportar observacion tardia como presupuesto incumplido sin ampliar tolerancia ni aceptarla como prueba valida.
- [ ] 5.6 Actualizar la documentacion de login para client obligatorio, error y precedencia acordados, claims y tolerancia, dejando identificable el baseline del Dia 1; verificar concordancia con las pruebas y comunicar impacto a consumidores, sin publicar tokens completos ni presentar CORS como corregido.

## 6. Confidencialidad de logs

- [ ] 6.1 Reemplazar exclusivamente los dos printStackTrace de LegacyDatabase por eventos fijos de inicializacion/consulta fallida, sin SQL, parametros, mensaje libre o throwable; verificar con fallos sinteticos que se conservan inicializacion/retorno y traduccion a `401`, sin refactor de persistencia.
- [ ] 6.2 Capturar logs de login exitoso, credenciales invalidas, errores de clave, inicializacion y consulta SQLite; verificar ausencia de los valores sinteticos exactos de token, secreto y contrasena, y que los nuevos componentes no registran request/response sensibles.
- [ ] 6.3 Adjuntar evidencia redactada de logs y documentar los eventos fijos y su menor detalle diagnostico; verificar que la evidencia permite reconocer cada resultado sin revelar credenciales, SQL sensible, tokens o claves.

## 7. Validacion integrada y condiciones de entrega

- [ ] 7.1 Ejecutar `mvn verify` sobre la integracion completa y registrar resultados nuevos de caracterizacion, claims/firma, expiracion, errores, configuracion y logs; verificar que no se deshabilitan comprobaciones y que cualquier discrepancia CORS queda explicitamente registrada como bloqueo externo, no como conformidad total.
- [ ] 7.2 Revisar cobertura JaCoCo del codigo modificado y resultados de CodeQL/Dependency Review; verificar cobertura de al menos 85% segun REGLAS.md y gates requeridos sin silenciamientos. Registrar como pendientes los gates no ejecutables, sin afirmar que han pasado.
- [ ] 7.3 Revisar trazabilidad de escenarios y evidencia contra el Issue #12, ejecutar validacion OpenSpec estricta y comprobar el diff; verificar que no se incluyeron cambios CORS, SQL, contrasenas, nuevos endpoints, bearer global o redisenos ajenos al alcance.
- [ ] 7.4 Documentar la revision de preparacion para entrega: consumidores con client, secreto aprovisionado, compatibilidad HS256 y estado del bloqueo CORS externo; verificar que no se promueve un despliegue como conforme mientras siga abierto y que el rollback no restaura Base64, fallback inseguro o claves comprometidas. No ejecutar despliegue ni ampliar alcance como parte de esta comprobacion.

## Limites Del Checklist

La correccion CORS requiere autorizacion y trabajo externo: aqui solo se detecta, documenta y comprueba el bloqueo. No se agregan tareas para corregir SQL/almacenamiento de contrasenas o exposiciones publicas ajenas al Issue. Las restricciones de no crear diseno/tareas en fases anteriores son historicas; esta entrega autoriza unicamente este checklist, no su ejecucion funcional ni el despliegue.