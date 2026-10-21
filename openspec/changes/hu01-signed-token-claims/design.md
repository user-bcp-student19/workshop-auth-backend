# Design

## Context

Fuente: [Issue #12](https://github.com/user-bcp-student19/workshop-auth-backend/issues/12). Motivacion y requisitos: [proposal.md](proposal.md), [signed-auth-token](specs/signed-auth-token/spec.md) y [auth-http-contract](specs/auth-http-contract/spec.md). Contrato protegido: [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md). Este documento corresponde a la fase de diseno ahora autorizada, sin implementar ni crear tareas.

El repositorio usa Java 21, Quarkus 3.27.4, CDI, REST Jackson, SQLite, JUnit 5, RestAssured y JaCoCo. `AuthResource.login` concentra validacion, consulta, emision y respuesta. `LoginRequest` ya declara `client`, pero no lo utiliza el recurso. `LegacyDatabase.findByCredentials` conserva valores originales, concatena SQL y captura errores devolviendo `null`; sus dos manejadores imprimen stacktraces. Solo hay una prueba fuente de login exitoso, que comprueba usuario y presencia de token.

### Diferencias verificadas respecto al baseline

El codigo fuente actual NO es la implementacion descrita por el contrato del Dia 1:

- `LegacyTokenUtil` genera Base64 de campos delimitados con un secreto embebido; no firma HS256, no serializa claims JWT ni lee `AUTH_TOKEN_SECRET`.
- `application.properties` permite cualquier origen CORS y no configura el secreto por perfil.
- No se encuentra en las fuentes la bateria de pruebas que el baseline cita. Los informes de `target/` no demuestran cobertura del codigo actual y no sustituyen nuevas ejecuciones.

No se redefine el contrato aprobado para hacerlo coincidir con estas diferencias. El usuario confirma en esta fase: disenar HS256, mantener CORS fuera del cambio y registrar su reconciliacion como bloqueo externo antes del despliegue. La proteccion por hash de contrasenas mencionada en el baseline tampoco se introduce aqui: no se modifica persistencia ni su semantica de autenticacion.

## Goals / Non-Goals

**Goals:**

- Separar el borde HTTP, la orquestacion del login y la emision segura con el minimo cambio local, sin redisenar persistencia.
- Obtener claims y duracion desde una sola emision y un reloj sustituible, conservando la respuesta HTTP.
- Eliminar el secreto embebido y evitar fugas en logs, incluidas excepciones de acceso a datos.
- Hacer caracterizables las diferencias actuales, los contratos preservados y los nuevos criterios antes de tocar comportamiento critico.

**Non-Goals:**

- Reorganizar toda la aplicacion, introducir interfaces por cada clase o sustituir SQLite.
- Corregir SQL concatenado o contrasenas almacenadas, implementar BCrypt, cambiar roles, errores publicos, CORS o health.
- Agregar filtros bearer, endpoints consumidores, refresh, revocacion o un registro de clientes.
- Garantizar retrocompatibilidad del token Base64 inseguro: contradice las specs aprobadas y no tendra fallback.

## Protected Contracts

- `POST /auth/login` sigue siendo publico y JSON; no requiere `Authorization`.
- Se preservan validacion y uso original de `username`/`password`, estados y cuerpos existentes, campos de exito, escape HTML de `user.displayName`, `role`, `debug` y `expiresIn = 3600`.
- Se conserva el error `500` por secreto menor de 32 bytes con `IllegalStateException` y el mensaje exacto del baseline. El mecanismo de error no debe incluir el secreto.
- No se cambia la negociacion de contenido ni los cuerpos vacios `400/403/405/406/415` del contrato. Se caracteriza por separado la discrepancia CORS; no se declara resuelta.
- `GET /q/health` sigue publico y sin checks propios nuevos. Las observaciones sobre POST y Accept de health no se convierten en nuevos requisitos.
- La unica extension del contrato de entrada es la validacion acordada de `client`; el token pasa al formato compacto HS256 exigido, con `sub`, `client`, `role`, `iat` y `exp`.

## Affected Components

| Componente | Cambio delimitado para la futura implementacion |
| --- | --- |
| `AuthResource` | Conservar traduccion HTTP y serializacion publica; delegar orquestacion sin alterar mensajes ni escape HTML. |
| Servicio de autenticacion local | Coordinar las guardas de entrada, la llamada existente a `LegacyDatabase` y la emision; devolver un resultado interno sin exponer la entidad como respuesta HTTP. |
| `LoginRequest` | Conservar campos y tipos; validar `client` explicitamente, sin Bean Validation que cambie automaticamente los cuerpos de error. |
| Emisor de tokens | Sustituir el interior de `LegacyTokenUtil` por un componente de seguridad CDI con reloj/configuracion sustituibles y firma mediante libreria; no mantener el formato Base64 ni dos emisores activos. |
| `LegacyDatabase` | Mantener consulta, inicializacion y retorno ante error; sustituir solo las impresiones de excepcion que pueden revelar valores sensibles. No extraer otra capa de repositorio en este cambio. |
| `application.properties` y documentacion operativa | Configurar secreto por entorno sin fallback embebido; documentar 32 bytes UTF-8 como minimo y sandbox. No editar CORS. |
| `pom.xml` | Incorporar unicamente soporte de construccion JWT alineado con la plataforma y validado por Dependency Review. |
| Pruebas existentes | Ampliar caracterizacion HTTP; agregar pruebas acotadas de emision, tiempo, configuracion y ausencia de secretos en logs. |

## Decisions

### D1. Usar libreria JWT manteniendo HS256

Decision: usar `quarkus-smallrye-jwt-build` alineado con el BOM Quarkus para construir JWT compactos, con clave simetrica y algoritmo HS256 explicitos. Los claims se suministran como datos estructurados; no se construye JSON ni Base64 con concatenaciones. No agregar la extension de autenticacion bearer ni cambiar seguridad global.

Alternativas: conservar HMAC con JCA minimiza dependencias pero exige mantener serializacion JOSE, codificacion y convenciones JWT propias; una libreria independiente como Nimbus es viable pero suma gestion de compatibilidad fuera de la integracion Quarkus. Se elige la integracion de construccion para reducir codigo de protocolo propio sin introducir validacion de acceso global.

Consecuencias: dependencia adicional revisable; verificar en una prueba acotada compatibilidad de HS256 y claims con la version del BOM antes de integrar. Si esa prueba falla, detener y revisar esta decision, no cambiar algoritmo o contrato silenciosamente. Verificar la firma en pruebas con JCA HmacSHA256 sobre los segmentos compactos y comparar bytes, independientemente del constructor JWT.

### D2. Politica minima de client confirmada por el usuario

Decision confirmada en esta sesion: `client` es obligatorio, string y no vacio tras `trim()`. Se conserva el valor original aceptado en el claim, sin normalizacion, whitelist o limite de longitud nuevo. No otorga permisos ni cambia `sub` o `role`.

Orden: cuerpo nulo, username, password, client, consulta de credenciales, emision. Ausente, `null`, vacio o blanco produce `400` con `{"message":"Client is required"}`, sin consulta ni token. Un tipo JSON no deserializable conserva la respuesta de deserializacion del framework; no se introduce coercion adicional. Credenciales invalidas con client aceptado conservan `401`. Cuando coinciden client invalido y credenciales incorrectas, prevalece la guarda de client.

Alternativas: client opcional con valor por defecto incumple la trazabilidad del cliente recibido; normalizarlo, imponer un registro o aceptar silenciosamente ausencias amplia el contrato sin necesidad. La obligatoriedad rompe solicitudes que antes lo omitieran y requiere comunicacion a consumidores.

### D3. Un solo reloj y una sola duracion

Decision: inyectar `java.time.Clock` UTC en el emisor, con reloj fijo en pruebas. Capturar un unico instante en segundos; fijar `iat` a ese instante y `exp = iat + 3600`. Un resultado interno de emision mantiene token, instante y vencimiento; `expiresIn` se obtiene de la misma duracion, no de una segunda constante en el recurso.

Confirmado por el usuario: tolerancia de 2 segundos, `now` al recibir/observar la respuesta con reloj UTC compartido y presupuesto maximo de observacion de 2 segundos. La evidencia registra ambos instantes y comprueba `abs(expiresIn - (exp - now)) <= 2`. Un retraso mayor se informa como presupuesto incumplido, sin ampliar tolerancia ni afirmar que esa corrida prueba coherencia. Las pruebas deterministas cubren el calculo sin depender de latencia ni sleeps.

No hay gracia tras `exp`: en `now = exp` y despues el token es expirado. La tolerancia de observacion no es leeway de autenticacion. Como no existe endpoint consumidor, las pruebas verifican firma, claims y frontera temporal con un reloj controlado, sin presentar esa evidencia como autorizacion HTTP implementada.

Alternativas: llamadas independientes a `Instant.now()` generan diferencias evitables; esperar una hora hace las pruebas lentas; ampliar tolerancia para absorber retrasos oculta errores.

### D4. Configuracion sin secreto por defecto

Decision: leer `AUTH_TOKEN_SECRET` mediante configuracion Quarkus, sin valor de respaldo para dev/prod. Validar presencia y al menos 32 bytes UTF-8 antes de firmar; nunca incluir el valor en mensajes ni logs. Leer configuracion de manera que el servicio pueda arrancar sin clave y falle al emitir: conserva health publico y permite el `500` documentado para clave corta. La falta de secreto tambien impide emitir; su mensaje debe ser fijo y no sensible, sin modificar el mensaje ya protegido para clave corta.

Sandbox significa entorno de laboratorio identificado expresamente, no cualquier perfil dev. Tampoco se necesita un secreto por defecto alli: inyectar claves sinteticas aisladas en pruebas y configurar el laboratorio explicitamente. No introducir secretos de ejemplo reutilizables en fuentes o evidencias.

Alternativas: fallar al arrancar evita un servicio parcialmente operativo, pero elimina la respuesta de clave corta y afecta health; fallback de laboratorio simplifica arranque pero arriesga uso fuera del sandbox. Se priorizan contrato y fallo cerrado de emision.

### D5. Logs seguros sin refactorizar persistencia

Decision: no registrar request/response, token, clave, contrasena ni SQL construido. Sustituir los dos `printStackTrace()` de `LegacyDatabase` por eventos fijos de inicializacion/consulta fallida, sin mensaje libre de excepcion, throwable o parametros. Conservar retorno `null` de consulta y su traduccion a `401`. Los nuevos componentes tampoco registraran valores sensibles ni stacktraces sin filtrar.

Alternativas: redactar despues de imprimir no evita la fuga original; adjuntar el throwable puede incluir SQL/credenciales; no tocar persistencia dejaria una via de fuga dentro del alcance de logs del Issue. Se admite menor detalle diagnostico a cambio de no filtrar datos, sin corregir la consulta.

## Incremental Strategy / Migration Plan

1. Registrar las decisiones de client y tiempo como aclaraciones aprobadas para reconciliarlas con las preguntas abiertas de proposal/specs antes de implementar. Este documento no modifica esos artefactos ni crea `tasks.md`.
2. Ejecutar y ampliar caracterizacion sobre fuentes actuales antes de refactorizar. Separar resultados de compatibilidad preservable de diferencias conocidas: Base64 actual, ausencia de configuracion y CORS abierto. No hacer pasar una expectativa HS256 como prueba de caracterizacion ya verde.
3. Incorporar y probar aisladamente constructor HS256, reloj y clave sintetica; conservar sin cambios persistencia y respuesta HTTP. Exigir evidencia de firma y fronteras temporales antes de reemplazar el emisor.
4. Introducir la orquestacion local y conectar emision/validacion de client en un cambio revisable. Actualizar pruebas solo para las diferencias aprobadas; repetir la caracterizacion restante.
5. Cerrar la via de fuga de logs en un cambio pequeno; probar errores con datos sinteticos. Documentar configuracion y comunicacion de client obligatorio sin publicar tokens ni claves.
6. Ejecutar verificacion completa y gates existentes: `mvn verify`, cobertura de codigo modificado de al menos 85%, CodeQL y Dependency Review sin silenciamientos. Conservar resultados de pruebas y evidencia redactada.
7. Antes de desplegar, confirmar consumidores con client, secreto aprovisionado y compatibilidad HS256. La reconciliacion CORS requiere trabajo externo autorizado: si sigue abierta, no declarar conformidad total con el baseline ni promover el despliegue como conforme.

No hay migracion de SQLite ni cambio de datos. Un token Base64 previo no es JWT valido: no agregar un verificador permisivo como mecanismo de transicion. Aunque el backend no expone consumidores protegidos, revisar los consumidores externos y solicitar nuevo login cuando sea necesario.

## Characterization and Validation

Pruebas a disponer y ejecutar ANTES de modificar token o flujo critico, ampliando la clase existente y reutilizando JUnit/RestAssured:

| Area | Caracterizacion previa | Verificacion del cambio |
| --- | --- | --- |
| HTTP exitoso | Login sin Authorization; campos, tipos, debug, role, escape HTML, expiresIn 3600. | Mismas aserciones con client aceptado y token HS256. No comparar cadenas completas de tokens. |
| Entradas | Cuerpo null; username/password ausentes, nulos, blancos; valores originales con espacios. | Mantener errores y precedencia existentes; agregar casos client, incluyendo dos valores distintos y espacios preservados. |
| Fallos | Usuario inexistente, contrasena incorrecta, fallo SQLite que produce 401 y captura general 500. | Mantener estados/cuerpos; agregar error exacto de clave corta, y ausencia de clave sin emision. |
| Protocolo y health | JSON mal formado, GET login, Accept/Content-Type incompatibles, health publico y CORS permitido/denegado. | Preservar casos conformes; marcar CORS como discrepancia no corregida, no omitir ni convertir su resultado actual en nuevo contrato. |
| Token | Capturar formato Base64 actual solo como diferencia conocida, sin publicar su contenido ni inmortalizarlo como contrato futuro. | Firma independiente, algoritmo HS256, claims tipados, sub/role correctos y client exacto; tokens alterados no superan verificacion de firma. |
| Tiempo | Caracterizar duracion de respuesta sin esperar una hora. | Reloj fijo: exp - iat = 3600; antes de exp valido temporalmente, en/despues expirado; observacion dentro de 2 segundos y retraso fuera de presupuesto informado. |
| Logs | Capturar errores sinteticos de inicializacion y consulta, sin adjuntar salidas sensibles sin redaccion. | Capturar logs de exito, credenciales invalidas, configuracion y SQLite; buscar los valores sinteticos exactos de clave, contrasena y token emitido, incluidos manejadores de error. |

Usar base y claves de prueba aisladas, no la base local del usuario. No afirmar que la prueba existente o los reportes de `target/` ya cubren esta matriz. Las pruebas de firma/frontera temporal no implican un nuevo endpoint ni una politica de autorizacion.

## Risks / Trade-offs

- Client obligatorio rompe consumidores que lo omiten: comunicar antes del despliegue y reconciliar specs con la decision confirmada; no inferir una identidad por defecto.
- Divergencia entre fuentes y baseline: conservar evidencia separada y bloquear conformidad completa hasta resolver CORS externamente. No corregirlo de paso.
- La libreria puede agregar claims o valores por defecto: configurar algoritmo y tiempos explicitamente y verificar contenido observable; no cambiar role, duracion o JSON publico.
- Fallo al emitir en vez de al arrancar deja health UP con login no operativo: despliegue debe comprobar configuracion y login sintetico sin introducir nuevos checks publicos.
- Mensajes HTTP internos, SQL concatenado y contrasenas almacenadas siguen siendo deuda: no se certifica endurecimiento global. Reducir logs no corrige esas vulnerabilidades.
- Logs fijos reducen detalle diagnostico: conservar categoria/evento y resultado sin incluir datos sensibles. Si se necesita mas detalle, requiere diseno de observabilidad separado.
- Comparacion temporal de integracion sensible a retrasos: separar pruebas deterministas del presupuesto de observacion y no usar la tolerancia como gracia de expiracion.

## Rollback

- Cambios de codigo, dependencia y documentacion deben ser pequenos y revisables; no hay datos que revertir. Mantener disponibles la configuracion y las pruebas del artefacto candidato.
- No volver al emisor Base64 con secreto embebido ni habilitar un fallback inseguro. Si no existe un artefacto anterior HS256 seguro, detener la promocion o retirar temporalmente la emision, no tratar el codigo actual como rollback aceptable para produccion.
- Revertir solo cambios de esta historia, sin tocar modificaciones ajenas ni CORS. Una reversion de client obligatorio requiere comunicar la incompatibilidad y revisar el cumplimiento de claims, no aplicar un default silencioso.
- Si una clave se expone, revertir binarios no basta: rotar la clave y exigir nuevo login, coordinando con consumidores. Nunca restaurar una clave comprometida.

## Approval and Remaining Gates

El usuario confirmo politica minima de client, orden de validacion, tolerancia de 2 segundos y tratamiento de CORS como bloqueo externo. No se dejan como decisiones tecnicas indefinidas. Las preguntas de propuesta/specs deben reconciliarse con esas confirmaciones en la siguiente revision autorizada, antes de implementar.

Este diseno queda para revision humana. En esta entrega no se ejecutan pruebas funcionales, no se instalan dependencias, no se modifica codigo ni se crean tareas. La validacion OpenSpec comprueba estructura y consistencia formal, no demuestra que el sistema ya cumpla los requisitos.