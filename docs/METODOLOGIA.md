# Metodología de trabajo

## 1. Enfoque general

Se trabajó con un proceso **iterativo e incremental**, inspirado en Scrumban: ciclos cortos de análisis, implementación y validación, con un tablero simple de tareas y priorización por valor para la demo.

```text
Levantamiento → Diseño → Implementación → Validación → Documentación
       ↑                                                │
       └──────────── retroalimentación ─────────────────┘
```

## 2. Principios de trabajo

1. **Primero lo que se demuestra:** cada iteración debía dejar un flujo visible y ejecutable.
2. **No romper lo que funciona:** los cambios se validaban con compilación y pruebas antes de avanzar.
3. **Documentación viva:** cada decisión relevante se registra en `/docs`.
4. **Datos ficticios:** ninguna prueba usa información personal real.
5. **Degradación controlada:** si la nube falla, la demo debe seguir funcionando.
6. **Trazabilidad:** todo cambio significativo se anota en [CAMBIOS.md](CAMBIOS.md) y [AVANCES.md](AVANCES.md).

## 3. Iteraciones

| Iteración | Objetivo | Entregable |
| --- | --- | --- |
| 0 | Auditoría del proyecto y rúbrica. | Diagnóstico, brechas y plan. |
| 1 | Modo local/demo. | Seed ficticio, Room y fallbacks del repositorio. |
| 2 | Validación GPS. | Validador Haversine, proveedor GPS y bloqueo por radio. |
| 3 | Experiencia de acreditación. | Estado GPS en escáner y bloqueo por distancia. |
| 4 | Sincronización explícita y acceso demo. | Botón “Sincronizar ahora”, banner local y login por rol. |
| 5 | Calidad y documentación. | Tests, JaCoCo, docs C4/alcance/mockups y bitácora. |
| 6 | Configuración de eventos sin GPS. | Región/comuna, radio cerrado y retiro de latitud/longitud del formulario. |
| 7 | Perfiles demo y responsables. | Selector de perfil por rol, responsables docentes/voluntarios e historial. |

El detalle de cada sesión está en [AVANCES.md](AVANCES.md).

## 4. Organización del trabajo

### 4.1 Backlog

- **Must:** flujos necesarios para la demo y la rúbrica.
- **Should:** mejoras de experiencia y robustez.
- **Could:** ideas de valor que no bloquean el MVP.
- **Won't:** fuera de alcance documentado.

### 4.2 Definition of Ready

Una tarea está lista para implementarse cuando:

- Tiene un requisito asociado.
- Se conoce el flujo de usuario.
- Se identifican los archivos/capas afectadas.
- Tiene criterio de aceptación verificable.

### 4.3 Definition of Done

Una tarea se considera terminada cuando:

1. Compila (`./gradlew :app:assembleDebug`).
2. Las pruebas unitarias pasan.
3. El flujo se probó manualmente o con test.
4. No rompe el modo remoto ni el modo local.
5. Se actualizó la documentación si cambió el alcance o la arquitectura.
6. Se registró en la bitácora de cambios.

## 5. Estrategia de pruebas

### 5.1 Pruebas unitarias JVM

Se priorizó la lógica de dominio pura:

| Suite | Qué valida |
| --- | --- |
| `LocationValidatorTest` | Distancia Haversine, radio, normalización de radio, precisión, estados y bloqueo. |
| `UbicacionEventoTest` | Parseo de enlaces y coordenadas, rangos válidos, enlaces cortos y precisión contra el radio. |
| `UbicacionesChileTest` | Catálogo de 16 regiones y sus comunas. |
| `FechaValidatorTest` | Rango término ≥ inicio, formatos ISO y eventos dentro del período del proyecto. |
| `DateTimeUtilsTest` | Conversión ISO ↔ millis, formatos y reloj. |
| `DemoAuthTest` | Perfiles demo, roles y sesión. |
| `ResponsablesTest` | Altas, bajas y cambios de rol de responsables. |
| `MappersTest` | Round-trip modelo ↔ entidad y campos opcionales. |
| `ExampleUnitTest` | Sanidad del entorno de pruebas. |

**Total:** 72 casos, 0 fallos.

Comando:

```bash
./gradlew :app:testDebugUnitTest
```

### 5.2 Cobertura

```bash
./gradlew :app:jacocoDebugUnitTestReport
```

Reporte: `app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html`.

Resultado actual:

| Métrica | Cobertura |
| --- | --- |
| Instrucciones | 82,7% |
| Ramas | 79,3% |
| Líneas | 94,1% |
| Complejidad | 69,7% |
| Métodos | 71,4% |
| Clases | 81,8% |

**Perímetro medido:** lógica de dominio testeable en JVM (validador GPS, fechas, snapshots, cuentas demo, modelos, mappers y utilidades de fecha/hora). Se excluyen pantallas Compose y `LocationProvider` porque dependen de Android/GPS real y requieren pruebas instrumentadas.

### 5.3 Pruebas manuales

| Escenario | Resultado esperado |
| --- | --- |
| Login demo por cada rol | Abre el panel correcto. |
| Crear evento con región/comuna y radio | Las listas dependientes funcionan y el radio solo ofrece 20/40/60/80/100 m. |
| Crear evento sin GPS | El evento queda sin coordenadas y no se pide ubicación actual. |
| Programar evento fuera del proyecto | El guardado se bloquea con el mensaje del período permitido. |
| Programar evento el último día del proyecto | Se permite cualquier hora de ese día. |
| Elegir perfil demo por rol | Se listan al menos dos perfiles del rol seleccionado. |
| Asignar y quitar responsables | La tarjeta refleja el equipo actual y el historial registra ambos movimientos. |
| Escanear QR dentro del radio | Asistencia `VALIDADA`. |
| Escanear QR fuera del radio | Bloqueo con distancia y radio. |
| Evento sin coordenadas | Asistencia `SIN_VALIDACION`. |
| Modo avión + inscripción/asistencia | Se guarda local y queda pendiente. |
| “Sincronizar ahora” sin backend | Mensaje de modo local, sin crash. |
| Docente consulta informes | Métricas coherentes con asistencias/encuestas. |

### 5.4 Pruebas pendientes

- Pruebas instrumentadas de cámara y GPS.
- Pruebas de integración con Supabase real.
- Pruebas end-to-end de sincronización con backend disponible.
- Pruebas de accesibilidad y tamaños de pantalla.

## 6. Uso de inteligencia artificial

La IA se usó como **asistente de apoyo**, no como sustituto del criterio técnico.

### 6.1 Usos permitidos y aplicados

- Explicar APIs de Android y sintaxis de Compose.
- Proponer estructuras de documentación.
- Revisar consistencia de nombres y mensajes.
- Generar borradores de pruebas y diagramas Mermaid.
- Detectar brechas entre la rúbrica y el código.

### 6.2 Controles aplicados

1. **Verificación contra el código real:** cada afirmación de la documentación se contrastó con archivos y versiones del proyecto.
2. **Compilación obligatoria:** ningún cambio se aceptó sin `assembleDebug`.
3. **Pruebas ejecutables:** las propuestas de lógica se convirtieron en tests JUnit.
4. **Datos ficticios:** no se incorporó información personal real.
5. **Credenciales:** no se usaron tokens expuestos ni claves de terceros.
6. **Revisión humana:** las decisiones de alcance, arquitectura y priorización fueron validadas por el autor.

### 6.3 Evidencia de no alucinación

- Las versiones documentadas provienen de `libs.versions.toml` y `app/build.gradle.kts`.
- Los porcentajes de cobertura provienen del XML generado por JaCoCo.
- Los estados de la rúbrica se contrastaron con el código y se marcaron honestamente como parciales/pendientes.
- No se documentó como implementado el backend Spring Boot/EC2 porque no existe en el repositorio.

## 7. Gestión de riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
| --- | --- | --- | --- |
| Supabase/EC2 no disponibles | Alta | Alto | Modo local con seed y fallback. |
| GPS impreciso o denegado | Media | Alto | Tolerancia acotada, mensajes claros y evento sin coordenadas. |
| Pérdida de datos offline | Baja | Alto | Room + cola `pending_action` + reintentos. |
| Duplicados al sincronizar | Media | Medio | Clave idempotente y UUID de operación. |
| Regresión en QR existente | Media | Alto | Validación de formato/evento y pruebas manuales. |
| Credenciales expuestas | Media | Alto | Variables de entorno, no versionar claves, rotación. |
| Cobertura insuficiente | Media | Medio | Tests de dominio y JaCoCo ≥ 30%. |
| Defensa individual | Media | Alto | Documentación C4 y guion de demo. |

## 8. Control de versiones y reproducibilidad

- El proyecto se mantiene en el directorio `SyncPass-Movil`.
- El directorio actual **no está inicializado como repositorio Git**; se recomienda `git init`, un `.gitignore` para `local.properties`, `build/`, `.gradle/` y llaves, y commits atómicos por iteración.
- El build usa Gradle Wrapper y dependencias fijadas en `libs.versions.toml`.
- Las credenciales se inyectan por `local.properties` o variables de entorno.

## 9. Comunicación y evidencia

| Artefacto | Propósito |
| --- | --- |
| `AVANCES.md` | Qué se hizo y en qué estado está. |
| `CAMBIOS.md` | Qué cambió, por qué y cómo impacta. |
| `ALCANCE.md` | Qué está dentro/fuera y estado frente a la rúbrica. |
| `MOCKUPS.md` | Cómo se ven las vistas y qué está implementado. |
| `users.md` | Cómo entrar a la demo. |

## 10. Próximos pasos metodológicos

1. Cerrar la brecha Spring Boot/EC2 con una iteración dedicada.
2. Configurar Supabase y ejecutar pruebas de integración.
3. Añadir pruebas instrumentadas de GPS y cámara.
4. Preparar el guion de defensa individual con preguntas probables.
5. Grabar la demo final con los flujos de [README.md](README.md#4-demostración-sugerida-58-minutos).
