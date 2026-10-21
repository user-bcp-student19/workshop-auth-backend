# Spec Delta

## Purpose

Especificar los ajustes de token, expiracion y confidencialidad comprometidos por el Issue #12 sin prescribir su implementacion. El contrato de referencia es `docs/ENDPOINTS.md`.

## ADDED Requirements

### Requirement: Token firmado con identidad y claims minimos

El servicio SHALL emitir, para un login valido con `client` aceptado, un token firmado verificable que contenga `sub`, `client`, `iat` y `exp`. `sub` SHALL identificar al usuario autenticado y `client` SHALL corresponder al valor aceptado de la solicitud, no a un valor fijo. Se SHALL preservar el claim `role` y el formato compacto HS256 del baseline; no se decide aqui la herramienta de firma.

#### Scenario: Login valido identifica al usuario y al cliente
- **WHEN** se envia `POST /auth/login` con credenciales validas y `client` aceptado
- **THEN** se obtiene `200` y un token cuya firma se verifica con la configuracion de prueba
- **AND** el token contiene `sub` del usuario autenticado, `client` de la solicitud, `role` del usuario y `iat` y `exp` como tiempos en segundos

#### Scenario: El cliente no se sustituye por una constante
- **WHEN** el mismo usuario inicia sesion con dos valores distintos de `client` que cumplen la politica acordada
- **THEN** cada token contiene el valor aceptado de su respectiva solicitud y ambos conservan el mismo `sub`

### Requirement: Validacion obligatoria de client

El servicio SHALL requerir `client` como string no vacio despues de `trim()` y validarlo despues de username/password, antes de consultar credenciales. Esta politica es la excepcion aprobada al baseline del Dia 1.

#### Scenario: Cliente ausente o blanco
- **WHEN** `client` falta, es null, vacio o contiene solo espacios
- **THEN** la respuesta es `400` con `{"message":"Client is required"}`
- **AND** no se consultan credenciales ni se emite token

#### Scenario: Tipo JSON incompatible para client
- **WHEN** `client` no se puede deserializar como string
- **THEN** se conserva la respuesta de deserializacion existente del framework, sin coercion nueva

### Requirement: Preservacion del valor client

El valor original de `client` aceptado SHALL conservarse en el claim sin normalizacion, whitelist ni limite de longitud nuevo.

#### Scenario: Cliente aceptado
- **WHEN** un login con credenciales validas incluye un `client` string no blanco
- **THEN** el token emitido contiene el valor original de `client`

### Requirement: Coherencia temporal

El token SHALL cumplir `exp - iat = 3600` y la respuesta SHALL mantener `expiresIn = 3600`. La observacion SHALL usar reloj UTC compartido, tolerancia maxima de 2 segundos y presupuesto maximo de observacion de 2 segundos. La tolerancia no extiende la validez del token.

#### Scenario: Duracion y respuesta coherentes
- **WHEN** se observa la respuesta de un login valido con reloj UTC compartido dentro de 2 segundos
- **THEN** `exp - iat` es 3600 y `expiresIn` es el entero 3600
- **AND** la diferencia absoluta entre `expiresIn` y `exp - now` no supera 2 segundos

#### Scenario: Observacion fuera del presupuesto
- **WHEN** la respuesta se observa mas de 2 segundos despues de su recepcion de referencia
- **THEN** la evidencia registra el presupuesto incumplido y no amplia la tolerancia para declarar conformidad

#### Scenario: Expiracion sin gracia
- **WHEN** se comprueba la validez temporal en o despues de `exp`
- **THEN** el token se considera expirado, sin aplicar la tolerancia de observacion como periodo de gracia

### Requirement: Expiracion efectiva

Un token SHALL NOT ser considerado valido en o despues de `exp`. La comprobacion de este requisito no requiere agregar un endpoint consumidor ni prescribe un mecanismo de validacion.

#### Scenario: Token expirado
- **WHEN** se comprueba la validez temporal de un token de prueba con firma correcta en un instante igual o posterior a su `exp`
- **THEN** el token se considera expirado y no valido, sin exigir ni introducir un endpoint consumidor

### Requirement: Secreto configurado por entorno

El uso de `AUTH_TOKEN_SECRET` SHALL estar documentado sin valores reales. Fuera de un entorno identificado explicitamente como sandbox SHALL NOT utilizarse un secreto por defecto. Cualquier secreto de laboratorio SHALL documentarse como exclusivo del sandbox. Se SHALL preservar el minimo documentado de 32 bytes.

#### Scenario: Entorno no sandbox sin secreto explicito
- **WHEN** un entorno no sandbox no dispone de `AUTH_TOKEN_SECRET` explicito
- **THEN** no se emiten tokens utilizando un secreto por defecto, sin prescribir si el rechazo ocurre al arrancar o al emitir

#### Scenario: Configuracion documentada sin exponer secretos
- **WHEN** se revisan las instrucciones de configuracion para sandbox y otros entornos
- **THEN** se identifica la variable `AUTH_TOKEN_SECRET`, el minimo de 32 bytes y el caracter exclusivo de laboratorio de cualquier valor por defecto
- **AND** no se incluyen secretos reales

### Requirement: Confidencialidad de logs y evidencia

El servicio SHALL NOT registrar tokens completos, secretos de firma, contrasenas ni otros valores sensibles en autenticaciones exitosas o fallidas, incluidos errores de configuracion. La evidencia SHALL usar valores sinteticos y logs redactados; no obliga a introducir nuevos logs ni a cambiar los cuerpos HTTP del baseline.

#### Scenario: Logs de exito y fallos
- **WHEN** se ejecutan con datos sinteticos un login valido, credenciales invalidas y un error de configuracion del secreto
- **THEN** los logs recogidos no contienen los tokens emitidos, los secretos ni las contrasenas utilizados
- **AND** la evidencia adjunta preserva el resultado observable sin revelar esos valores