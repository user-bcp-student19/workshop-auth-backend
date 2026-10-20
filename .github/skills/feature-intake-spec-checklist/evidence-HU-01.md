# Feature Intake -> Spec Checklist

## Summary
- Ajustar la emisión del token de `POST /auth/login` para incluir los claims `sub`, `client`, `iat` y `exp`, mantener coherente la expiración y evitar registrar tokens, secretos o valores sensibles.

## Scope (in/out)
- **In**
  - Exigir los claims `sub`, `client`, `iat` y `exp` en un token válido.
  - Obtener `client` de la solicitud, validarlo e incluirlo en el token.
  - Comprobar la coherencia de `expiresIn` con `exp - now` dentro de una tolerancia documentada.
  - Resolver el uso del secreto por defecto fuera del sandbox.
  - Evitar registrar tokens y secretos.
  - Probar login válido, credenciales inválidas y token expirado.
- **Out**
  - HU-01 no especifica elementos fuera del alcance.

## Assumptions
- HU-01 no confirma supuestos adicionales.

## Technical decisions to make
- **Pendiente:** conservar HMAC o adoptar una librería JWT.
- **Pendiente:** eliminar el secreto por defecto fuera del sandbox o documentarlo como exclusivo de laboratorio.
- **Pendiente:** definir y documentar la tolerancia para comparar `expiresIn` con `exp - now`.
- **Pendiente:** definir los criterios para validar el valor `client` recibido en la solicitud.

## Risks and mitigations
- Exposición de tokens, secretos o valores sensibles en logs: evitar su registro y revisar evidencia de logs redactados.
- Uso del secreto por defecto fuera del sandbox: resolver si se elimina o se documenta como exclusivo de laboratorio.
- Inconsistencia entre `expiresIn` y `exp`: documentar la tolerancia y validar la coherencia respecto a ella.

## Acceptance criteria
- Todo token válido contiene los cuatro claims requeridos: `sub`, `client`, `iat` y `exp`.
- `expiresIn` es coherente con `exp - now` dentro de la tolerancia definida y documentada.
- Existen pruebas para login válido, credenciales inválidas y token expirado.
- No se registran tokens, secretos ni valores sensibles.
- La decisión entre HMAC y una librería JWT queda documentada.

## Validation plan
- **Pruebas unitarias:** verificar presencia de claims y coherencia de expiración dentro de la tolerancia definida.
- **Pruebas funcionales:** cubrir login válido, credenciales inválidas y token expirado.
- **Seguridad (comprobaciones mínimas):** comprobar que no se registren tokens, secretos ni valores sensibles; revisar el tratamiento del secreto por defecto.
- **Evidencia requerida:** resultados de pruebas de claims, expiración y errores de autenticación; documentación de configuración del secreto sin incluir su valor; evidencia de logs redactados; y este documento.

## Suggested tasks by role
- **Chapter/PO**
  - Confirmar los resultados esperados de login con credenciales inválidas y token expirado.
  - Confirmar los criterios funcionales para considerar válido el valor `client`.
- **Tech Lead**
  - Documentar la decisión entre HMAC y una librería JWT.
  - Definir y documentar la tolerancia de expiración.
  - Resolver la política del secreto por defecto fuera del sandbox.
- **Backend**
  - Emitir los cuatro claims y obtener y validar `client` desde la solicitud.
  - Asegurar la coherencia de `expiresIn` con `exp - now` según la tolerancia acordada.
  - Evitar registrar tokens y secretos, y documentar la configuración sin exponer el valor.
- **Frontend**
  - HU-01 no confirma cambios de frontend; su aplicabilidad queda abierta.
- **QA**
  - Verificar los cuatro claims en un token válido.
  - Probar la coherencia de expiración respecto a la tolerancia documentada.
  - Probar los tres escenarios de login indicados y revisar evidencia de logs redactados.
- **Cyber**
  - Revisar que tokens, secretos y valores sensibles no aparezcan en logs.
  - Revisar la política del secreto por defecto fuera del sandbox.

## Open questions
- ¿Se conservará HMAC o se adoptará una librería JWT?
- ¿Se eliminará el secreto por defecto fuera del sandbox o se documentará como exclusivo de laboratorio?
- ¿Qué tolerancia se documentará para la coherencia entre `expiresIn` y `exp - now`?
- ¿Qué criterios determinan si el valor `client` recibido en la solicitud es válido?
- ¿Qué respuestas se esperan para credenciales inválidas y token expirado?
- ¿Aplica algún cambio de frontend a esta historia?
- ¿Hay elementos que deban quedar explícitamente fuera del alcance?