# Plan de corrección

## Selección

- **H-02**: seleccionado por severidad alta y riesgo directo ante una filtración de la base de datos.
- **H-04**: seleccionado por severidad alta y alcance acotable a la configuración del secreto y la política CORS.

El contrato de `POST /auth/login` documentado en `docs/ENDPOINTS.md` se conserva: mismos campos de solicitud y respuesta, códigos HTTP y semántica observable. No se propone cambiar ese documento.

## H-02 — Contraseñas almacenadas en texto plano

**Estado y tipo:** Pendiente — Vulnerabilidad, severidad alta.

**Problema:** `LegacyDatabase` crea la columna `password` como texto y siembra credenciales literales. Una lectura de SQLite expone contraseñas reutilizables; reemplazar el valor sin migrar la base existente también podría impedir el login de usuarios ya almacenados.

**Cambio propuesto:** Añadir un componente dedicado de hashing lento con salt aleatorio por contraseña, usando `PBKDF2WithHmacSHA256` de la JDK para no incorporar una dependencia externa. Al crear o migrar usuarios, guardar algoritmo/parámetros, salt y hash, nunca la contraseña. Implementar una migración idempotente de las filas preexistentes en texto plano y convertir las credenciales semilla al mismo formato. Comparar hashes de forma constante en tiempo. Mantener intactos la solicitud, las respuestas y los estados HTTP del endpoint.

**Archivos a tocar:**

- `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`: esquema, siembra, migración y verificación de credenciales.
- `src/main/java/com/bcp/workshop/auth/PasswordHasher.java` (nuevo): creación y verificación de hashes con salt.
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: caracterización del contrato de login.
- `src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java` (nuevo): persistencia, migración y verificación contra una SQLite temporal.

**Pruebas de caracterización antes de tocar producción:** Añadir primero pruebas de integración que documenten el login exitoso de `student`, el rechazo de una contraseña incorrecta (`401`) y los campos actuales de la respuesta exitosa (`token`, `expiresIn`, `user` y `debug`). En una base temporal, caracterizar la autenticación de un usuario existente antes y después de la migración; después del cambio, verificar que el hash y el salt almacenados no son la contraseña original, que el login correcto sigue funcionando y que una contraseña incorrecta falla. No registrar contraseñas ni hashes en logs.

## H-04 — Secreto y política CORS permisiva

**Estado y tipo:** Pendiente — Vulnerabilidad, severidad alta.

**Problema:** El secreto está escrito en `LegacyTokenUtil` y CORS acepta cualquier origen. El secreto no se puede rotar de forma independiente al artefacto; el origen comodín permite solicitudes de navegador desde sitios no autorizados. Además, el token actual incluye el secreto en texto recuperable desde Base64, por lo que solo moverlo a configuración no evitaría que se revele en los tokens.

**Cambio propuesto:** Cargar el secreto desde una propiedad respaldada por variable de entorno, sin valor por defecto de producción ni secreto real en el repositorio, y usarlo únicamente como clave de firma, nunca dentro del contenido del token. Definir una lista explícita de orígenes CORS por entorno mediante configuración externa; eliminar `/.*/` y evitar un comodín. Mantener el contrato HTTP: el token sigue siendo un string y no cambian los campos ni estados de `POST /auth/login`. Para usar el secreto sin exponerlo, la generación firmada del token debe coordinarse con H-03; H-04 por sí solo no debe considerarse cerrado mientras el token conserve el formato Base64 actual. Los valores de configuración de pruebas serán ficticios y aislados.

**Archivos a tocar:**

- `src/main/resources/application.properties`: propiedades externas para el secreto y los orígenes permitidos; configuración de prueba aislada.
- `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java`: recibir configuración e impedir que el secreto aparezca en el contenido del token; coordinar el formato firmado con el trabajo de H-03.
- `src/main/java/com/bcp/workshop/auth/AuthResource.java`: ajustar la inyección si el utilitario deja de ser estático, sin cambiar la respuesta HTTP.
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: preservar mediante caracterización los campos, tipos y estado del login exitoso.
- `src/test/java/com/bcp/workshop/auth/LegacyTokenUtilTest.java` (nuevo): comprobar uso de la clave configurada y que el token no revele su valor.
- `src/test/java/com/bcp/workshop/auth/AuthCorsTest.java` (nuevo, si no se puede integrar en la prueba de recurso): comprobar que un origen configurado recibe permiso y que uno no configurado no queda autorizado.

**Pruebas de caracterización antes de tocar producción:** Añadir primero una prueba de integración de login exitoso que fije el contrato HTTP vigente y confirme que `token` continúa siendo string. Añadir pruebas de configuración que definan el origen de prueba permitido y documenten el comportamiento actual de CORS; tras el cambio, exigir permiso solo para el origen configurado. Añadir una prueba para que el secreto de prueba sea suministrado por configuración, no por una constante Java. Luego verificar que cambiar el secreto de prueba cambia la firma, que su valor no se encuentra en el token y que la ausencia de configuración requerida falla de forma explícita, sin filtrar el valor en errores.

## Aprobación

Plan aprobado explícitamente por el usuario en la conversación el 2026-10-19. Se autoriza implementar H-02 y H-04 según el alcance descrito arriba.