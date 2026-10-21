# Workshop Auth Backend — Java Quarkus

> **Laboratorio intencionalmente vulnerable.** Ejecutar únicamente en local, repositorios privados y ambientes aislados. No desplegar en Internet, CERT o PROD.

Backend Quarkus que autentica usuarios de laboratorio almacenados en SQLite y entrega un token consumido por el frontend Angular y la API Python.

## Ejecución

Requisitos: JDK 21 y Maven 3.9+.

```bash
mvn quarkus:dev
```

Servicio: `http://localhost:8080`

Credenciales locales:

- `student` / `Workshop123!`
- `trainer` / `Trainer123!`

## Endpoints

- `POST /auth/login`
- `GET /q/health`

## Configuración del secreto de firma

El emisor JWT de HU-01 lee `AUTH_TOKEN_SECRET` del entorno. Debe configurarse explícitamente con al menos 32 bytes UTF-8. No hay un valor por defecto: sin clave, el emisor no debe crear tokens. En el laboratorio, configura una clave sintética únicamente en el entorno local aislado; para otros entornos, aprovisiónala mediante el gestor de secretos correspondiente. No la guardes en el repositorio, ejemplos, historial de shell ni logs.

`POST /auth/login` usa este emisor y no mantiene fallback al formato Base64 legacy. Las pruebas usan una clave sintética exclusiva de Surefire; no representa un secreto de despliegue.

## Propósito de la rama `main`

La rama contiene deuda técnica y de seguridad deliberada: SQL concatenado, contraseñas en texto plano, token reversible, secreto hardcodeado, CORS permisivo, método largo, manejo genérico de excepciones y dependencias con CVEs.
