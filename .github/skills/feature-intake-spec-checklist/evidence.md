# Feature Intake -> Spec Checklist

## Summary
- Añadir una confirmación previa al envío de transferencias, con un resumen y validación de datos, para reducir en un 20% las quejas por transferencias duplicadas durante un mes.

## Scope
- **Dentro del alcance**
  - Confirmación antes de enviar una transferencia.
  - Visualización de un resumen de la transferencia.
  - Validación de los datos antes del envío.
- **Fuera del alcance**
  - Cambios de infraestructura.
  - Rediseño completo de la interfaz.

## Supuestos
- Se usarán la API REST, el frontend web y los servicios existentes de cuentas y transferencias.
- La fuente y el periodo para medir las quejas aún deben definirse.

## Decisiones técnicas por tomar (pendientes o resueltas)
- **Pendiente:** definir qué datos aparecen en el resumen.
- **Pendiente:** acordar las reglas y los mensajes para validar importes y otros datos.
- **Pendiente:** definir la fuente y el periodo de comparación para medir las quejas.
- **Resuelta:** no se incluyen cambios de infraestructura ni un rediseño completo.

## Riesgos y mitigaciones
- Exposición de datos sensibles en logs: excluir datos sensibles y revisar los logs del flujo.
- Errores en la validación de importes: acordar las reglas con los responsables del servicio y probar casos válidos e inválidos.
- Dependencia de los servicios de cuentas y transferencias: validar la integración con ambos antes de liberar.

## Criterios de aceptación (medibles)
- El 100% de los envíos del flujo solicita confirmación y muestra un resumen antes de enviar la transferencia.
- El 100% de los datos que incumplan las reglas acordadas se rechaza antes del envío.
- Las quejas por transferencias duplicadas se reducen en un 20% durante un mes, según la fuente y el método de comparación que se definan.
- Los logs del flujo no contienen datos sensibles en los casos cubiertos por las pruebas de seguridad.

## Plan de validación
- **Pruebas unitarias:** cubrir la confirmación y los casos válidos e inválidos según las reglas acordadas.
- **Pruebas funcionales:** verificar el resumen, la confirmación y la integración con los servicios de cuentas y transferencias.
- **Seguridad (comprobaciones mínimas):** inspeccionar los logs del flujo y comprobar que no incluyan datos sensibles.
- **Evidencia requerida (qué adjuntar):** resultados de pruebas unitarias y funcionales; checklist de revisión de logs; documentación breve actualizada; PR revisado; `SKILL.md`; `examples/input.sample.md`; `examples/output.sample.md`; tag `skill-feature-intake-v0.1.0`; y este documento `evidence.md`.

## Tareas sugeridas por rol
- **Chapter/PO**
  - Acordar el contenido del resumen.
  - Definir cómo se contabilizan las quejas y el periodo de comparación.
- **Tech Lead**
  - Acordar las reglas de validación con los responsables de los servicios.
  - Revisar la integración con la API REST existente.
  - Verificar que la solución se mantenga dentro del alcance.
- **Backend**
  - Implementar las validaciones acordadas antes del envío.
  - Integrar el flujo con los servicios de cuentas y transferencias.
  - Comprobar que los logs no expongan datos sensibles.
- **Frontend**
  - Mostrar el resumen antes del envío.
  - Incorporar la confirmación al flujo web existente.
- **QA**
  - Probar los casos válidos e inválidos según las reglas acordadas.
  - Verificar que la transferencia no se envíe antes de la confirmación.
  - Adjuntar resultados de pruebas funcionales y de integración.
- **Cyber**
  - Revisar el tratamiento de datos sensibles en los logs.
  - Revisar los casos de prueba relacionados con exposición de datos.

## Preguntas abiertas
- ¿Qué campos deben aparecer en el resumen?
- ¿Cuáles son las reglas exactas para validar importes y otros datos?
- ¿Qué fuente y periodo de comparación se usarán para verificar la reducción del 20% en quejas?