# Problemática y propuesta de solución

## 1. Contexto

Las instituciones educativas y organizaciones comunitarias ejecutan proyectos de vinculación con el medio que incluyen talleres, jornadas y actividades en terreno. En estos procesos participan coordinadores docentes, voluntarios y beneficiarios de la comunidad.

La operación actual suele apoyarse en planillas, listas en papel, formularios aislados y mensajería instantánea. Esa combinación funciona para grupos pequeños, pero pierde trazabilidad cuando aumenta el volumen de eventos, participantes y sedes.

## 2. Problema central

> No existe una forma simple, confiable y trazable de saber **quién se inscribió**, **quién asistió realmente** y **si estaba presente en el lugar del evento**, sin depender de registros manuales dispersos.

### 2.1 Manifestaciones del problema

| Dimensión | Situación actual | Consecuencia |
| --- | --- | --- |
| Inscripción | Listas en papel o mensajes informales | Duplicados, cupos excedidos y pérdida de datos. |
| Acreditación | Firma o marca manual | Suplantación y registros incompletos. |
| Presencia física | Confianza en el registro del voluntario | No se verifica que el asistente esté en el lugar. |
| Métricas | Consolidación posterior en planillas | Informes tardíos y propensos a error. |
| Evidencia | Documentos dispersos | Dificultad para auditar y rendir cuentas. |
| Conectividad | Sedes sin internet estable | Actividades que no se pueden registrar en línea. |

### 2.2 Actores afectados

- **Docente/coordinador:** invierte tiempo en consolidar listas y no cuenta con información oportuna.
- **Voluntario:** debe validar asistencias manualmente, muchas veces sin herramientas.
- **Beneficiario:** no tiene un comprobante claro de inscripción ni retroalimentación del taller.
- **Administrador:** no puede auditar roles, usuarios ni actividad con facilidad.
- **Organización:** pierde evidencia para informes de impacto y rendición.

## 3. Necesidades detectadas

1. Centralizar proyectos, eventos, inscripciones y asistencias.
2. Identificar a cada participante mediante un código QR personal.
3. Verificar que la acreditación ocurra **en el lugar del evento** mediante GPS.
4. Operar sin conexión y sincronizar cuando vuelva la red.
5. Entregar información consolidada para la toma de decisiones.
6. Respetar la privacidad y minimizar la recolección de datos personales.

## 4. Propuesta de solución

**SyncPass** es una aplicación Android nativa que digitaliza el ciclo completo de una actividad comunitaria:

```text
Docente crea proyecto/evento
        ↓
Beneficiario se inscribe y recibe un pase QR
        ↓
Voluntario escanea el QR en el lugar
        ↓
App valida GPS contra las coordenadas del evento
        ↓
Asistencia válida (o bloqueada si está fuera del radio)
        ↓
Beneficiario responde encuesta
        ↓
Docente revisa métricas e informes
```

### 4.1 Propuesta de valor

- **Trazabilidad:** cada asistencia queda asociada a una inscripción, una marca de tiempo y una validación de ubicación.
- **Confianza:** el QR evita suplantaciones simples y el GPS verifica presencia física.
- **Continuidad operativa:** modo offline-first con sincronización explícita.
- **Información oportuna:** métricas de asistencia, encuestas y satisfacción en el mismo panel.
- **Bajo costo de adopción:** una app Android, sin hardware especializado.

### 4.2 Diferenciador técnico

La validación GPS no se limita a comparar dos puntos. Considera el **radio configurado del evento**, la **precisión informada por el GPS** y un **margen acotado** para evitar falsos rechazos sin permitir validaciones desde lejos. El detalle está en [ARQUITECTURA.md](ARQUITECTURA.md#7-validación-gps).

## 5. Indicadores de éxito

| Indicador | Meta del MVP |
| --- | --- |
| Asistencias registradas con identificador único | 100% |
| Asistencias con validación GPS en eventos georreferenciados | ≥ 95% |
| Tiempo de acreditación por asistente | < 10 segundos |
| Operaciones disponibles sin conexión | Inscripción, QR, asistencia, encuesta |
| Consolidación de métricas | Inmediata en el panel |
| Cobertura de pruebas de lógica de dominio | ≥ 30% |

## 6. Restricciones del contexto

- Los datos usados en la demo son **ficticios**; no se permite información personal real.
- El entorno de evaluación no tiene disponibles las instancias EC2 ni Supabase.
- La app debe seguir funcionando en modo local para poder demostrar todos los flujos.
- La seguridad de credenciales y tokens es parte del alcance, no un agregado posterior.
