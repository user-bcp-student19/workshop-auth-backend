# Log detallado de pruebas posterior al plan

- **Fecha:** 2026-10-19
- **Comando:** `mvn verify`
- **Resultado Maven:** `BUILD SUCCESS`
- **Resumen:** 12 pruebas; 0 fallos, 0 errores, 0 omitidas.
- **Fuente del detalle:** reportes XML de Surefire en `target/surefire-reports/`.

## `AuthCorsTest` — 2 pruebas

| Prueba | Resultado | Duración |
|---|---|---:|
| `rejectsUnconfiguredOriginForLoginPreflight` — rechaza el preflight de origen no permitido | PASS | 1.486 s |
| `allowsConfiguredOriginForLoginPreflight` — permite el preflight del origen configurado | PASS | 0.018 s |

## `AuthResourceTest` — 3 pruebas

| Prueba | Resultado | Duración |
|---|---|---:|
| `logsInWithWorkshopUser` — conserva el estado y los campos del contrato de login | PASS | 2.154 s |
| `storesSeedPasswordAsSaltedHash` — comprueba que la contraseña semilla no queda en texto plano | PASS | 0.007 s |
| `rejectsInvalidCredentialsWithUnauthorizedStatus` — mantiene el rechazo de credenciales con HTTP 401 | PASS | 0.193 s |

## `LegacyDatabaseTest` — 1 prueba

| Prueba | Resultado | Duración |
|---|---|---:|
| `migratesExistingPasswordsOnceAndVerifiesCredentials` — migra una base heredada, verifica login correcto/incorrecto, evita inyección por username y confirma idempotencia | PASS | 1.655 s |

## `LegacyTokenUtilTest` — 3 pruebas

| Prueba | Resultado | Duración |
|---|---|---:|
| `rejectsMissingOrWeakSigningKeys` — rechaza claves de firma insuficientes | PASS | 0.003 s |
| `signingKeyChangesTokenSignature` — confirma que cambiar la clave cambia la firma | PASS | 0.041 s |
| `createsSignedTokenWithExplicitClaimsAndNoEmbeddedSecret` — verifica formato firmado, claims, expiración y ausencia del secreto en el token | PASS | 0.017 s |

## `PasswordHasherTest` — 3 pruebas

| Prueba | Resultado | Duración |
|---|---|---:|
| `createsDifferentSaltedHashesForSamePassword` — genera salts distintos para una misma contraseña | PASS | 0.280 s |
| `verifiesOnlyTheMatchingPassword` — acepta la contraseña correcta y rechaza otra | PASS | 0.422 s |
| `rejectsMalformedHashesAndNullPasswords` — rechaza hashes malformados y entradas nulas | PASS | 0.000 s |

JaCoCo analizó el bundle con 7 clases. Este log no incluye porcentajes de cobertura por clase.