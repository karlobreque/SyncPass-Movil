# Avances del proyecto

> **Última actualización:** 2026-11-17
> **Registro rápido de sesiones:** [`REGISTRO.md`](REGISTRO.md).
> **Estado:** MVP funcional en modo local/demo, compilado y con pruebas unitarias en verde.
> **Alcance de este informe:** qué se encontró, qué se implementó, cómo se verificó y qué queda pendiente para la entrega y la defensa.

---

## 1. Resumen ejecutivo

El proyecto pasó de un cliente Android que dependía por completo de Supabase a un **MVP demostrable sin backend remoto**. Los cuatro paneles funcionan con datos ficticios sembrados en Room, la asistencia por QR incorpora **validación GPS con radio cerrado (20/40/60/80/100 m)** y el dashboard expone una **sincronización manual explícita**.

| Indicador | Resultado |
| --- | --- |
| Compilación debug | Correcta (`:app:assembleDebug`) |
| APK generado | `app/build/outputs/apk/debug/app-debug.apk` (~37 MB) |
| Pruebas unitarias | 72 ejecutadas, 0 fallos, 0 errores |
| Cobertura JaCoCo (dominio) | 82,7% instrucciones · 79,3% ramas · 94,1% líneas |
| Código principal | 43 archivos Kotlin · 11.121 líneas |
| Código de pruebas | 9 archivos Kotlin · 858 líneas |
| Documentación | 12 documentos + presentación HTML interactiva |
| Modo local | Operativo con seed ficticio y fallbacks del repositorio |
| Etapa actual | **Pruebas locales:** se validan reglas y CRUD en Room antes de integrar Supabase |
| Modo remoto | Supabase **offline** por ahora; integración y sincronización planificadas para una fase posterior |
| Captura de fechas | Selectores Material 3 + validación término ≥ inicio |
| Responsables | Docentes y voluntarios por proyecto/evento (`responsable_cache`) + historial |
| Ubicación GPS del evento | Configurable desde la tarjeta: ubicación actual o enlace de Google Maps |
| Auditoría | Creador y fecha/hora en proyectos, eventos y asistencias |

---

## 2. Punto de partida (auditoría)

Antes de intervenir se revisaron el código, el caso y la rúbrica `EVA3/prueba3Android.md`. Los hallazgos principales fueron:

| # | Hallazgo | Impacto | Acción tomada |
| --- | --- | --- | --- |
| 1 | Sin credenciales de Supabase en el entorno. | La app no podía listar ni guardar datos. | Modo local con seed y fallback en el repositorio. |
| 2 | Sin backend Spring Boot/EC2 en el repositorio. | Brecha frente a la rúbrica. | Documentado como deuda prioritaria; no se simuló como implementado. |
| 3 | Sin validación de presencia física. | No se cumplía el requisito de cercanía al evento. | Validador Haversine + GPS + bloqueo por radio. |
| 4 | Sin pruebas unitarias reales (solo `2 + 2`). | Cobertura 0% y sin evidencia de calidad. | 20 pruebas de dominio + 1 de sanidad, JaCoCo configurado. |
| 5 | Sin documentación en `/docs`. | Sin C4, alcance ni mockups. | 12 documentos profesionales + presentación HTML. |
| 6 | La app no permitía entrar sin Cognito. | Demo imposible sin nube. | Acceso rápido demo por rol. |
| 7 | Sincronización solo automática. | La rúbrica exige mecanismo explícito. | Botón “Sincronizar ahora” + contador + mensaje. |
| 8 | Métricas dependían del backend. | Informes vacíos en modo local. | Cálculo local desde Room. |
| 9 | Mensajes de “Supabase no configurado” en rojo. | La demo parecía fallar. | Banners informativos de modo local. |
| 10 | Eventos sin coordenadas podían romper el flujo. | Regresión del QR existente. | Se registran como `SIN_VALIDACION`. |

---

## 3. Bitácora de iteraciones

### Iteración 0 — Auditoría y plan (2026-11-14)

- Revisión del caso, la rúbrica y el código existente.
- Identificación de brechas funcionales, técnicas y documentales.
- Definición de la estrategia: **primero funcionalidad demostrable, luego calidad y documentación**.
- Confirmación de que el proyecto compilaba antes de los cambios.

**Resultado:** plan de trabajo y criterios de aceptación del MVP.

### Iteración 1 — Modo local/demo (2026-11-15)

- Creación de `LocalSeed` con usuarios, roles, proyecto, eventos, inscripciones, asistencias y encuestas ficticias.
- Ampliación de `SyncPassDao` con consultas para usuarios, roles, proyectos, eventos, inscripciones, métricas y pendientes.
- Fallbacks locales en `SyncPassRepository` para lectura y escritura.
- IDs negativos para registros creados offline.
- Métricas calculadas desde Room (`metricasLocales`).

**Resultado:** todos los paneles pueden operar sin Supabase.

### Iteración 2 — Validación GPS (2026-11-15)

- `LocationSnapshot` como DTO independiente de Android.
- `LocationValidator` con Haversine, radio configurable en ese momento (luego cerrado a 20/40/60/80/100 m), tolerancia por precisión y estados de decisión.
- `LocationProvider` con `GPS_PROVIDER`, `NETWORK_PROVIDER`, `PASSIVE_PROVIDER`, precisión, antigüedad y última ubicación conocida.
- Regla de tolerancia acotada: `min(precisión, min(75 m, radio/2))`.
- Rechazo de lecturas con precisión mayor a 250 m.

**Resultado:** lógica de dominio 100% testeable en JVM.

### Iteración 3 — Experiencia de acreditación (2026-11-15)

- `VoluntarioViewModel` valida GPS antes de registrar y bloquea cuando corresponde.
- `VoluntarioPanel` incorpora `GpsStatusCard` con distancia, precisión y estado.
- Reutilización de una lectura reciente (< 90 s) al escanear para evitar esperas.
- `GpsStatusCard` en el escáner con estado, distancia y precisión.
- Estados visibles: validada, fuera de radio, sin ubicación, precisión insuficiente y evento sin coordenadas.
- La captura de coordenadas en el formulario de evento se retiró en la versión `1.2.0`.

**Resultado:** el voluntario entiende **por qué** se acepta o bloquea una asistencia.

### Iteración 4 — Sincronización explícita y acceso demo (2026-11-15)

- `DashboardViewModel` con estado de sincronización, contador y mensaje de resultado.
- Botón **“Sincronizar ahora”** y banner de modo local en `DashboardScreen`.
- `DemoAuth` con cuatro cuentas ficticias y sesión local.
- `LoginScreen` con bloque **“Acceso rápido de demostración”** cuando Supabase no está configurado.
- `MainActivity` conecta login Cognito y acceso demo.

**Resultado:** demo posible en menos de un minuto por rol, sin nube.

### Iteración 5 — Calidad y documentación (2026-11-15)

- `LocationValidatorTest` (12 casos), `DemoAuthTest` (4), `ResponsablesTest` (3), `MappersTest` (7) y `ExampleUnitTest` (1), además de `FechaValidatorTest` (12) y `DateTimeUtilsTest` (7).
- JaCoCo 0.8.13 con tarea `:app:jacocoDebugUnitTestReport`.
- Reporte HTML/XML con perímetro de dominio documentado.
- Documentos: README, problemática, requisitos, alcance, tecnologías, arquitectura C4, metodología, avances, cambios, mockups y credenciales.
- Corrección de banners de error a informativos en Admin y Beneficiario.

**Resultado:** build, tests y cobertura verificables; documentación completa.

### Iteración 6 — Mockups HTML y prototipo web (2026-11-15)

- Presentación visual interactiva en `docs/mockups/index.html`: 15 pantallas Android + 5 Web, con toggle y QR generados en el navegador.
- Mapa del proyecto en `docs/MAP.md` para localizar archivos y decidir si modificarlos.
- Prototipo web independiente en `../../SyncPass-Web` (HTML/CSS/JS estático, 7 páginas), fuera del proyecto Android y sin backend.

**Resultado:** material de presentación listo y base visual para la futura versión web.

### Iteración 7 — Fechas, relación proyecto-evento y auditoría (2026-11-16)

- `DateField` y `DateTimeField` con calendario y reloj de Material 3 para proyectos y eventos.
- `FechaValidator` con la regla término ≥ inicio; mensaje en pantalla y bloqueo del guardado.
- Selector de proyecto dentro de la pestaña Eventos y del formulario; la relación proyecto-evento queda explícita.
- Tabla `proyecto_docente` (N:M) y `DocentesMultiSelect`; el docente creador se preselecciona.
- Auditoría `creadoPor`/`creadoEn` en proyectos y eventos; `asistenciaRegistradoPor`/`asistenciaFecha` en asistencias.
- `LiveClock` con fecha y hora en vivo en la cabecera del dashboard.
- Base Room versión 3 (migración destructiva) y seed con docentes y auditoría.
- Pruebas nuevas: `FechaValidatorTest` (7) y `DateTimeUtilsTest` (7).

**Resultado:** el usuario elige fechas válidas, asocia cada evento a su proyecto y el sistema conoce al responsable de cada registro.

### Iteración 8 — Ubicación de eventos sin GPS (2026-11-16)

- Radio de validación restringido a **20, 40, 60, 80 y 100 m** con `RadioValidacionDropdown`.
- Catálogo `UbicacionesChile` con 16 regiones y sus comunas.
- `RegionComunaFields` con listas dependientes región → comuna.
- Se retiraron los campos latitud/longitud y el botón “Usar mi ubicación actual” del alta y edición.
- Los eventos nuevos quedan sin coordenadas y registran `SIN_VALIDACION`.
- Al editar un evento existente se conservan sus coordenadas GPS.
- Room sube a versión 4; el seed usa Metropolitana de Santiago / Maipú y radio 100 m.
- Pruebas nuevas de normalización de radio (2 casos).

**Resultado:** crear un evento ya no exige estar en el recinto ni escribir coordenadas a mano, y la ubicación se expresa con región y comuna.

### Iteración 9 — Eventos dentro del período del proyecto (2026-11-16)

- `FechaValidator.rangoDentroDePeriodo` y `mensajeErrorPeriodo` validan el rango del evento contra el proyecto.
- El formulario de alta y el diálogo de edición muestran el período del proyecto.
- El botón de guardar se deshabilita si el evento empieza antes o termina después del proyecto.
- `createEvento` y `updateEvento` rechazan la operación fuera de rango como respaldo.
- El último día del proyecto se admite completo (cualquier hora).
- Pruebas nuevas: 5 casos en `FechaValidatorTest`.

**Resultado:** no se pueden programar actividades antes del inicio ni después del término del proyecto.

### Iteración 10 — Acceso directo a eventos del proyecto (2026-11-16)

- **“Ver eventos”** en la tarjeta de proyecto selecciona ese proyecto y cambia a la pestaña **Eventos**.
- `DocenteUiState.eventosPorProyecto` mantiene el conteo de actividades por proyecto.
- La tarjeta muestra “N eventos creados.” o “Aún no tiene eventos creados.”.
- El estado vacío de Eventos incluye el nombre del proyecto sin actividades.

**Resultado:** el docente llega en un toque a los eventos del proyecto y sabe de antemano si existen.

### Iteración 11 — Perfiles demo y responsables con historial (2026-11-16)

- Acceso demo con selector de rol y al menos dos perfiles ficticios por rol.
- Responsables múltiples de proyectos y eventos: docentes y voluntarios.
- Nueva tabla genérica `responsable_cache` y eliminación de `proyecto_docente`.
- Historial `responsable_historial` con altas, bajas, rol, fecha y actor.
- Diálogo “Historial de responsables” en tarjetas de proyecto y evento.
- Las tarjetas muestran el equipo actual de responsables.
- Al eliminar un usuario se retiran sus asignaciones actuales.
- Room sube a versión 5.
- Pruebas nuevas: `ResponsablesTest` (3) y `DemoAuthTest` ampliado a 4.

**Resultado:** se puede saber quién estuvo a cargo de cada proyecto/evento y cuándo cambió, incluyendo voluntarios.

### Iteración 12 — Ubicación GPS configurable después del alta (2026-11-17)

- La tarjeta de evento muestra si tiene punto GPS y el radio configurado.
- Nuevo diálogo **“Ubicación GPS del evento”** con dos caminos: **usar la ubicación actual** (GPS/red) o **pegar un enlace de Google Maps / par de coordenadas**.
- `CoordenadasParser` interpreta pares simples, `@lat,lng`, `?q=`, `?ll=`, `?center=`, `?destination=`, `!3d!4d`, `geo:` y el signo menos unicode.
- `UbicacionEventoValidator` advierte si la precisión de la captura supera la mitad del radio y recomienda un radio mayor; rechaza referencias con precisión > 250 m.
- Se puede **quitar** el punto GPS para volver a `SIN_VALIDACION`.
- El alta del evento **sigue sin pedir coordenadas** y el guardado no las exige: se coordina el GPS después.
- `updateEventoUbicacion` persiste solo latitud, longitud y radio, primero en Room y luego *best-effort* en Supabase.
- La app sube a `1.6.0` (`versionCode 2`).
- Pruebas nuevas: `UbicacionEventoTest` (22) y `UbicacionesChileTest` (4).

**Resultado:** cualquier evento creado puede georreferenciarse sin pedirle números al docente y sin romper el flujo de quienes no configuran GPS.

---

## 4. Cambios implementados por bloque

### 4.1 Modo local y datos ficticios

| Archivo | Cambio |
| --- | --- |
| `data/local/LocalSeed.kt` | Dataset ficticio idempotente al primer arranque. |
| `data/local/SyncPassDao.kt` | Consultas de usuarios, roles, proyectos, eventos, inscripciones, métricas y asistencia. |
| `data/repository/SyncPassRepository.kt` | Fallbacks locales, métricas locales y payload de asistencia con estado. |
| `ui/panels/*` | Banners de modo local en lugar de errores. |

Dataset de demostración:

- Proyecto `101` — “Alfabetización Digital para Vecinos”.
- Eventos `201` (sin coordenadas), `202` (con coordenadas y radio 100 m) y `203`.
- Los tres eventos usan región Metropolitana de Santiago y comuna Maipú.
- Beneficiario `4` — `je.aquino@duouc.cl`.
- Inscripciones `301`–`305` con asistencias y encuestas de ejemplo.

### 4.2 Validación GPS

| Archivo | Cambio |
| --- | --- |
| `data/location/LocationValidator.kt` | Haversine, tolerancia, estados y mensajes. |
| `data/location/LocationProvider.kt` | Lectura con precisión, antigüedad y fallback a última conocida. |
| `ui/panels/VoluntarioViewModel.kt` | Validación previa al registro y bloqueo. |
| `ui/panels/VoluntarioPanel.kt` | `GpsStatusCard`, permisos y refresco. |
| `ui/panels/DocentePanel.kt` | Formulario con región/comuna y radio cerrado; sin captura GPS desde `1.2.0`. |

### 4.3 Sincronización

| Archivo | Cambio |
| --- | --- |
| `ui/dashboard/DashboardViewModel.kt` | `syncNow`, estado `syncing`, mensaje y contador. |
| `ui/dashboard/DashboardScreen.kt` | Bloque de sincronización manual y banner local. |
| `data/repository/SyncPassRepository.kt` | Payload y encolado de asistencia con `estado_validacion`. |

### 4.4 Autenticación demo

| Archivo | Cambio |
| --- | --- |
| `data/auth/DemoAuth.kt` | Cuentas, roles y sesión local. |
| `ui/login/LoginScreen.kt` | Tarjeta de acceso rápido por rol. |
| `MainActivity.kt` | Navegación login Cognito / demo. |

### 4.5 Calidad

| Archivo | Cambio |
| --- | --- |
| `app/build.gradle.kts` | Plugin JaCoCo, tarea de reporte y perímetro de medición. |
| `app/src/test/.../LocationValidatorTest.kt` | 12 casos de GPS y normalización de radio. |
| `app/src/test/.../DemoAuthTest.kt` | 4 casos de perfiles demo. |
| `app/src/test/.../ResponsablesTest.kt` | 3 casos de altas, bajas y cambio de rol. |
| `app/src/test/.../MappersTest.kt` | 7 casos de mapeo Room ↔ modelo. |
| `app/src/test/.../FechaValidatorTest.kt` | 12 casos de rangos de fecha/hora y período de proyecto. |
| `app/src/test/.../DateTimeUtilsTest.kt` | 7 casos de formatos y reloj. |

### 4.6 Fechas, proyectos y auditoría

| Archivo | Cambio |
| --- | --- |
| `data/model/FechaValidator.kt` | Regla término ≥ inicio y mensajes de error. |
| `ui/components/DateTimeFields.kt` | Calendario/reloj Material 3, campos de solo lectura y `LiveClock`. |
| `ui/components/DateTimeUtils.kt` | Conversión ISO ↔ millis y formatos de fecha/hora. |
| `ui/components/Selectors.kt` | Selector de proyecto y selección múltiple de docentes. |
| `data/local/Entities.kt` | `proyecto_docente`, `creadoPor`/`creadoEn` y auditoría de asistencia. |
| `data/local/SyncPassDatabase.kt` | Room versión 3 (luego versión 4 en `1.2.0`). |
| `data/repository/SyncPassRepository.kt` | Docentes por proyecto, validación de fechas y auditoría. |
| `ui/panels/DocentePanel.kt` | Formularios con pickers, selector de proyecto y docentes. |
| `ui/panels/VoluntarioPanel.kt` | Muestra quién registró la asistencia y cuándo. |
| `ui/dashboard/DashboardScreen.kt` | Reloj de fecha y hora en la cabecera. |

### 4.7 Ubicación de eventos sin GPS (`1.2.0`)

| Archivo | Cambio |
| --- | --- |
| `data/model/UbicacionesChile.kt` | Catálogo de regiones y comunas. |
| `data/model/Models.kt` | `region` y `comuna` en `Evento` y `EventoInput`. |
| `data/local/Entities.kt` | `region` y `comuna` en `EventoEntity`. |
| `data/local/SyncPassDatabase.kt` | Room versión 4. |
| `data/location/LocationValidator.kt` | `RADIOS_VALIDOS` y `normalizarRadio`. |
| `data/repository/SyncPassRepository.kt` | Persistencia de región/comuna y radio normalizado. |
| `ui/components/Selectors.kt` | `RegionComunaFields` y `RadioValidacionDropdown`. |
| `ui/panels/DocentePanel.kt` | Formulario sin latitud/longitud ni botón GPS. |
| `ui/panels/VoluntarioPanel.kt` | Muestra comuna y región del evento. |

### 4.8 Ubicación GPS configurable (`1.6.0`)

| Archivo | Cambio |
| --- | --- |
| `data/model/UbicacionEvento.kt` | `CoordenadasEvento`, `CoordenadasParser` y `UbicacionEventoValidator`. **Código puro con 22 pruebas.** |
| `ui/components/UbicacionEventoDialog.kt` | Diálogo con permiso, lectura GPS, pegado de enlaces, radio y opción de quitar. |
| `ui/panels/DocentePanel.kt` | Estado GPS en la tarjeta del evento y botón de configuración. |
| `ui/panels/DocenteViewModel.kt` | `configurarUbicacion(...)` y actualización del estado. |
| `data/repository/SyncPassRepository.kt` | `updateEventoUbicacion(...)` con validación de par y persistencia local/remota. |
| `app/build.gradle.kts` | Versión `1.6.0` y `versionCode 2`. |
| `app/src/test/.../UbicacionesChileTest.kt` | Catálogo territorial cubierto (4 casos). |

---

## 5. Métricas de calidad

### 5.1 Pruebas unitarias

| Suite | Casos | Resultado |
| --- | --- | --- |
| `UbicacionEventoTest` | 22 | ✅ |
| `LocationValidatorTest` | 12 | ✅ |
| `FechaValidatorTest` | 12 | ✅ |
| `DateTimeUtilsTest` | 7 | ✅ |
| `DemoAuthTest` | 4 | ✅ |
| `ResponsablesTest` | 3 | ✅ |
| `MappersTest` | 7 | ✅ |
| `UbicacionesChileTest` | 4 | ✅ |
| `ExampleUnitTest` | 1 | ✅ |
| **Total** | **72** | **0 fallos / 0 errores** |

Tiempo total de ejecución: menos de 1 s.

### 5.2 Cobertura JaCoCo

| Métrica | Cobertura |
| --- | --- |
| Instrucciones | **82,7%** (4.398 / 5.320) |
| Ramas | **79,3%** (184 / 232) |
| Líneas | **94,1%** (894 / 950) |
| Complejidad | 65,9% |
| Métodos | 69,7% |
| Clases | 83,9% |

**Perímetro medido:** `data/location/**` (sin `LocationProvider`), `DemoAuth`, `Mappers`, `data/model/**` y `ui/components/DateTimeUtils.kt`.
**Excluido:** pantallas Compose (incluido `UbicacionEventoDialog`), `LocationProvider` y clientes remotos, por requerir emulador, hardware o backend real.
**Nota:** `UbicacionesChileTest` cubre el catálogo territorial que antes bajaba la métrica sin aportar lógica.

El umbral exigido por la rúbrica (≥ 30%) se supera con holgura en las tres métricas principales.

### 5.3 Tamaño y estructura

| Elemento | Valor |
| --- | --- |
| Archivos Kotlin principales | 43 |
| Líneas principales | 11.121 |
| Archivos de prueba | 9 |
| Líneas de prueba | 858 |
| APK debug | ~37 MB |
| Documentos | 12 |

---

## 6. Verificación ejecutada

| Comando | Resultado |
| --- | --- |
| `./gradlew :app:compileDebugKotlin --offline` | ✅ Sin errores |
| `./gradlew :app:testDebugUnitTest --offline` | ✅ 72 pruebas en verde |
| `./gradlew :app:jacocoDebugUnitTestReport --offline` | ✅ Reporte generado |
| `./gradlew :app:assembleDebug --offline` | ✅ APK generado |

Artefactos:

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Reporte HTML: `app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html`
- Reporte XML: `app/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml`
- Resultados de tests: `app/build/reports/tests/testDebugUnitTest/index.html`

> **Nota de honestidad técnica:** la verificación automática cubre compilación, pruebas de dominio y empaquetado. Los flujos de cámara y GPS requieren un dispositivo o emulador con permisos; se listan como verificación manual pendiente en la sección 8.

---

## 7. Estado actual por módulo

| Módulo | Estado | Evidencia |
| --- | --- | --- |
| Login Cognito | Implementado | `CognitoAuthManager`, `JwtUtils`, `MainActivity`. |
| Login demo | Implementado | `DemoAuth`, `LoginScreen`. |
| Dashboard por rol | Implementado | `DashboardScreen`. |
| Administrador | Implementado en local | `AdminPanel`, `AdminViewModel`, DAO. |
| Proyectos | Implementado en local | CRUD + docentes a cargo (N:M). |
| Eventos | Implementado en local | CRUD + selector de proyecto + región/comuna + radio cerrado. |
| Ubicación GPS del evento | Implementado | `UbicacionEventoDialog`, `CoordenadasParser` (21 tests) y tarjeta con acción “Configurar ubicación GPS”. |
| Fechas y horas | Implementado y probado | `DateField`, `DateTimeField`, `FechaValidator` (12 casos) + `DateTimeUtils` (7 casos). |
| Responsables | Implementado en local | `responsable_cache`, `DocentesMultiSelect`, historial. |
| Acceso demo por perfil | Implementado | `DemoAuth.accountsFor`, `DemoAccessCard`. |
| Auditoría | Implementado en local | `creadoPor`/`creadoEn`, `asistenciaRegistradoPor`. |
| Reloj del dashboard | Implementado | `LiveClock`. |
| QR de beneficiario | Implementado | `QrCode`, pestaña “Mi pase QR”. |
| Escáner QR | Implementado | CameraX + ML Kit. |
| Validación GPS | Implementado y probado | `LocationValidator` + 12 tests. |
| Asistencia | Implementado | `registrarAsistencia` con estado. |
| Encuestas | Implementado en local | `BeneficiarioPanel` + Room. |
| Métricas | Implementado en local | `metricasLocales`. |
| Offline-first | Implementado | Room + `pending_action`. |
| Sync manual | Implementado | Botón y contador. |
| Sync automática | Implementado | `SyncWorker` + WorkManager. |
| Documentación | Completa para el MVP | 12 documentos + presentación HTML. |
| Backend Spring Boot/EC2 | Pendiente | Fuera del repositorio actual. |
| Sync remota verificada | Pendiente | Sin credenciales ni instancias. |

---

## 8. Verificación manual recomendada antes de la demo

Ejecutar en un dispositivo o emulador con GPS y cámara:

- [ ] Instalar el APK y abrir la app.
- [ ] Confirmar que aparece el banner de modo local.
- [ ] Entrar como **Docente** y ver Resumen, Proyectos, Eventos e Informes.
- [ ] Elegir el rol **Voluntario** en el acceso demo y entrar con distintos perfiles.
- [ ] Asignar un voluntario a un proyecto/evento y confirmar que aparece en la tarjeta.
- [ ] Quitar un responsable y abrir **“Historial de responsables”** para ver la baja.
- [ ] Confirmar que el dashboard muestra el reloj con fecha y hora actualizadas.
- [ ] Crear un proyecto con uno o dos docentes y confirmar “Docentes a cargo”.
- [ ] Intentar guardar un término anterior al inicio y confirmar el bloqueo.
- [ ] Intentar guardar un evento antes del inicio o después del término del proyecto y confirmar el bloqueo.
- [ ] Presionar **“Ver eventos”** en una tarjeta de proyecto y confirmar que cambia a la pestaña Eventos filtrada.
- [ ] Revisar un proyecto sin eventos y confirmar el aviso “Aún no tiene eventos creados.”.
- [ ] Crear un evento eligiendo el proyecto desde el selector del formulario.
- [ ] Verificar que las fechas y horas se eligen solo con los selectores.
- [ ] Confirmar “Creado por …” en las tarjetas de proyecto y evento.
- [ ] Crear un evento y verificar que la región carga sus comunas y el radio solo ofrece 20/40/60/80/100 m.
- [ ] Verificar que el formulario de evento no muestra latitud, longitud ni “Usar mi ubicación actual”.
- [ ] Configurar el GPS de un evento con **“Usar mi ubicación actual”** y confirmar punto, precisión y radio.
- [ ] Pegar un enlace de Google Maps o un par `latitud, longitud` y confirmar que se interpreta.
- [ ] Guardar un evento **sin GPS** y confirmar que la tarjeta lo indica y la asistencia queda `SIN_VALIDACION`.
- [ ] Presionar **“Quitar ubicación GPS”** y confirmar que el evento vuelve a `SIN_VALIDACION`.
- [ ] Verificar que una lectura con precisión > 250 m no se puede guardar como referencia.
- [ ] Entrar como **Beneficiario**, inscribirse y abrir el pase QR.
- [ ] Entrar como **Voluntario**, abrir el escáner del evento georreferenciado.
- [ ] Verificar que `GpsStatusCard` muestra distancia y precisión.
- [ ] Escanear el QR dentro del radio y confirmar asistencia `VALIDADA`.
- [ ] Confirmar en la lista de inscritos quién registró la asistencia y cuándo.
- [ ] Simular ubicación fuera del radio y confirmar el bloqueo.
- [ ] Abrir el evento sin coordenadas y confirmar `SIN_VALIDACION`.
- [ ] Responder la encuesta como beneficiario.
- [ ] Verificar que Informes refleja asistencia y encuesta.
- [ ] Activar modo avión, registrar una operación y volver a conectarse.
- [ ] Presionar **“Sincronizar ahora”** y confirmar que no hay crash.
- [ ] Cerrar sesión y repetir con otro rol.

---

## 9. Pendientes y próximos pasos

| Prioridad | Pendiente | Resultado esperado |
| --- | --- | --- |
| Alta | Implementar API Spring Boot y desplegarla en EC2. | Cumplir el contenedor backend de la rúbrica. |
| Alta | Configurar Supabase y validar CRUD/sync remotos. | Flujo remoto comprobado. |
| Alta | Verificación manual completa en dispositivo. | Evidencia para la defensa. |
| Alta | Preparar respuestas de defensa individual. | Cubrir MVVM, offline-first, GPS y auth. |
| Media | Pruebas instrumentadas de GPS y cámara. | Cobertura de flujos nativos. |
| Media | Sincronizar `responsable_cache`, `responsable_historial` y la auditoría con el esquema remoto. | Responsables e historial compartidos entre dispositivos. |
| Media | Sincronizar `latitud`, `longitud` y `radio_validacion_metros` configurados desde la tarjeta. | Validación GPS consistente entre dispositivos. |
| Media | Exportación de informes PDF/CSV. | Evidencia entregable. |
| Media | Notificaciones de recordatorio. | Mayor participación. |
| Baja | Mapas para elegir recinto. | Mejor configuración de eventos. |

---

## 10. Reproducción del estado actual

```bash
cd SyncPass-Movil

# Compilar
./gradlew :app:assembleDebug --offline

# Pruebas
./gradlew :app:testDebugUnitTest --offline

# Cobertura
./gradlew :app:jacocoDebugUnitTestReport --offline

# Instalar en dispositivo conectado
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Si se dispone de credenciales, agregar en `local.properties`:

```properties
SUPABASE_URL=https://<proyecto>.supabase.co
SUPABASE_ANON_KEY=<anon-key>
```

Al configurarlas, la app intenta el flujo remoto y mantiene el fallback local.

---

## 11. Conclusión

El MVP cumple el objetivo de la demo: **todos los roles pueden operar, la asistencia se valida por GPS y el sistema funciona sin depender de la nube**. Las brechas restantes —Spring Boot/EC2 y sincronización remota verificada— están identificadas, documentadas y priorizadas, sin presentarlas como terminadas.

El proyecto queda en condiciones de ser demostrado y defendido, con evidencia reproducible de compilación, pruebas y cobertura.
