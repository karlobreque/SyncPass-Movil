# Mapa del proyecto SyncPass-Movil

> **Objetivo:** saber qué contiene cada archivo y si hay que modificarlo, sin releer todo el proyecto.
> **Uso:** busca el archivo o la responsabilidad en las tablas y ve directo a la sección indicada.
> **Última actualización:** 2026-11-17.

---

## 1. Leyenda

| Marca | Significado |
| --- | --- |
| 🟢 **SÍ** | Es un archivo de trabajo habitual. Modificar cuando cambie la funcionalidad asociada. |
| 🟡 **SOLO SI APLICA** | Modificar únicamente si cambia ese aspecto concreto (configuración, esquema, tema, docs). |
| 🔴 **NO** | No modificar. Es generado, es secreto, es legado sin uso o solo presentación. |

---

## 2. Archivos Android que SÍ se tocan

### 2.1 Núcleo y datos

| Archivo | Contenido | Marca | Cuándo modificarlo |
| --- | --- | --- | --- |
| `MainActivity.kt` | Decide login vs. dashboard, restaura sesión, conecta Cognito y `DemoAuth`. | 🟢 | Cambios de navegación, sesión o arranque. |
| `data/auth/CognitoAuthManager.kt` | Login, logout y restauración con AWS Cognito. | 🟢 | Flujos de autenticación real. |
| `data/auth/JwtUtils.kt` | Decodifica el JWT, define `Role` y `UserSession`. | 🟡 | Si cambian los grupos de Cognito o los roles. |
| `data/auth/DemoAuth.kt` | Perfiles ficticios por rol y sesión local elegida. | 🟢 | Alta/baja de perfiles demo o roles. |
| `data/model/Models.kt` | Modelos de dominio: `Usuario`, `DocenteRef`, `Proyecto`, `Evento`, `Inscripcion`, `Inscrito`, métricas. | 🟢 | Si cambian campos o reglas de negocio. |
| `data/model/UbicacionesChile.kt` | Catálogo de 16 regiones y sus comunas para el formulario de evento. **Código puro.** | 🟡 | Si cambia el catálogo territorial o los nombres. |
| `data/model/Responsables.kt` | Tipos, roles, acciones y cálculo de altas/bajas de responsables. **Código puro.** | 🟢 | Cambios en la regla de responsables. Tiene tests. |
| `data/model/FechaValidator.kt` | Validación pura de fechas: término ≥ inicio, formatos ISO y eventos dentro del período del proyecto. **Código puro.** | 🟢 | Cambios en las reglas de fechas. Tiene tests. |
| `data/model/UbicacionEvento.kt` | Parseo de coordenadas/enlaces de Google Maps y reglas de precisión/radio del punto GPS. **Código puro.** | 🟢 | Cambios en el flujo de ubicación del evento. Tiene tests. |
| `data/local/Entities.kt` | Entidades Room: `proyecto_cache`, `responsable_cache`, `responsable_historial`, `evento_cache`, `inscripcion_cache`, `inscrito_cache`, `usuario_cache`, `pending_action`. | 🟡 | Si cambia el modelo de datos. **Implica bump de versión de BD.** |
| `data/local/SyncPassDatabase.kt` | Base Room versión 5, `fallbackToDestructiveMigration`. | 🟡 | Solo cambios de esquema o versión. |
| `data/local/SyncPassDao.kt` | Consultas Room: CRUD, métricas, pendientes, `markAsistencia`. | 🟢 | Nuevas consultas locales o filtros. |
| `data/local/LocalSeed.kt` | Dataset ficticio (usuarios, roles, proyectos, eventos, inscripciones). | 🟢 | Cambiar datos de la demo. |
| `data/local/Mappers.kt` | Conversión modelo ↔ entidad. | 🟢 | Si cambian modelos o entidades. |
| `data/location/LocationValidator.kt` | Haversine, radio, precisión y estados de validación. **Código puro.** | 🟢 | Cambios en la regla GPS. Tiene tests. |
| `data/location/LocationProvider.kt` | Lectura GPS con `LocationManager`, precisión y antigüedad. | 🟢 | Ajustes de lectura de ubicación. |
| `data/remote/SupabaseConfig.kt` | Lee `SUPABASE_URL` y `SUPABASE_ANON_KEY` del `BuildConfig`. | 🟡 | Si cambian las claves de configuración. |
| `data/remote/SupabaseClient.kt` | Cliente HTTP/PostgREST para Supabase. | 🟢 | Endpoints, payloads o manejo de errores remotos. |
| `data/repository/SyncPassRepository.kt` | **Única fuente de verdad.** Remoto con fallback local, métricas, asistencia, encuestas, sync. | 🟢 | Casi cualquier cambio de flujo de datos. |
| `data/sync/SyncScheduler.kt` | Programación de WorkManager. | 🟡 | Cambios en la sincronización en segundo plano. |
| `data/sync/SyncWorker.kt` | Worker que drena la cola `pending_action`. | 🟡 | Reintentos, errores de sync. |
| `data/api/AuthInterceptor.kt` | Interceptor OkHttp para adjuntar el token de Cognito. | 🔴 | **Sin uso actual.** Candidato a eliminar cuando se defina la API Spring Boot/EC2. |

### 2.2 UI y presentación Android

| Archivo | Contenido | Marca | Cuándo modificarlo |
| --- | --- | --- | --- |
| `ui/components/Common.kt` | Componentes compartidos: `PanelHeader`, `MetricBox`, `InfoBanner`, `ErrorBanner`, formateadores. | 🟢 | Cambios visuales transversales. |
| `ui/components/DateTimeUtils.kt` | Utilidades puras de fecha/hora: formato ISO, reloj, fecha larga. | 🟢 | Cambios de formato o reloj. Tiene tests. |
| `ui/components/DateTimeFields.kt` | `DateField`, `DateTimeField` (calendario/reloj Material 3) y `LiveClock`. | 🟢 | Cambios en la captura de fechas o el reloj. |
| `ui/components/UbicacionEventoDialog.kt` | Diálogo para fijar el GPS del evento: ubicación actual, enlace/coordenadas, radio y quitar. | 🟢 | Cambios en la configuración GPS posterior al alta. |
| `ui/components/Selectors.kt` | `ProyectoDropdown`, `DocentesMultiSelect`, `RegionComunaFields` y `RadioValidacionDropdown`. | 🟢 | Cambios en selectores de proyecto, docentes, región/comuna o radio. |
| `ui/components/QrCode.kt` | Generación de QR con ZXing. | 🟡 | Cambiar contenido o tamaño del QR. |
| `ui/dashboard/DashboardScreen.kt` | Contenedor por rol, banner de modo local, botón de sincronización. | 🟢 | Cambios en el shell o en sync manual. |
| `ui/dashboard/DashboardViewModel.kt` | Estado de sincronización: `pendingSync`, `syncing`, `syncMessage`. | 🟢 | Cambios en sync o contador. |
| `ui/login/LoginScreen.kt` | Login Cognito + selector de rol y perfiles de acceso demo. | 🟢 | Cambios en autenticación o acceso demo. |
| `ui/panels/AdminPanel.kt` | Panel admin: usuarios, roles y pestañas embebidas de otros roles. | 🟢 | CRUD de usuarios o banners. |
| `ui/panels/AdminViewModel.kt` | Lógica del panel admin. | 🟢 | CRUD de usuarios/roles. |
| `ui/panels/DocentePanel.kt` | Proyectos, eventos, responsables docentes/voluntarios, historial, formulario con región/comuna y radio, informes, y configuración GPS del evento. | 🟢 | Cambios en pantallas docentes, formulario de evento o ubicación GPS. |
| `ui/panels/DocenteViewModel.kt` | Proyectos, eventos, responsables, historial, conteo por proyecto, ubicación GPS, métricas e informes. | 🟢 | Cambios en flujos docentes. |
| `ui/panels/VoluntarioPanel.kt` | Lista de eventos, inscritos, escáner QR y `GpsStatusCard`. | 🟢 | Cambios en acreditación o GPS UI. |
| `ui/panels/VoluntarioViewModel.kt` | Validación GPS, escaneo y registro de asistencia. | 🟢 | Cambios en el flujo de asistencia. |
| `ui/panels/BeneficiarioPanel.kt` | Información, eventos, pase QR y encuesta. | 🟢 | Cambios en inscripción, QR o encuesta. |
| `ui/panels/BeneficiarioViewModel.kt` | Inscripción, cancelación, encuesta y QR. | 🟢 | Cambios en flujos del beneficiario. |
| `ui/scanner/QrScannerScreen.kt` | Cámara con CameraX y ML Kit. | 🟢 | Cambios en el escáner. |
| `ui/theme/Color.kt` | Paleta institucional. | 🟡 | Cambios de marca. |
| `ui/theme/Theme.kt` | Tema Material 3. | 🟡 | Cambios de tema. |
| `ui/theme/Type.kt` | Tipografías. | 🟡 | Cambios tipográficos. |

---

## 3. Configuración y build

| Archivo | Contenido | Marca | Cuándo modificarlo |
| --- | --- | --- | --- |
| `app/build.gradle.kts` | Dependencias, `BuildConfig` de Supabase, JaCoCo, tarea de reporte. | 🟡 | Agregar dependencias, tareas o coverage. |
| `gradle/libs.versions.toml` | Versiones y aliases de dependencias. | 🟡 | Subir versiones. |
| `build.gradle.kts` | Plugins raíz. | 🟡 | Cambios de plugin a nivel proyecto. |
| `settings.gradle.kts` | Repositorios e inclusión de módulos. | 🔴 | **No agregar SyncPass-Web aquí.** El prototipo web es otro proyecto. |
| `gradle.properties` | JVM args y configuration cache. | 🔴 | Solo si hay problema de build. |
| `app/src/main/AndroidManifest.xml` | Permisos (internet, cámara, GPS), `MainActivity`. | 🟡 | Nuevos permisos o activities. |
| `app/src/main/res/**` | Strings, íconos, XML de backup. | 🟡 | Cambios de recursos. |
| `local.properties` | Rutas del SDK y credenciales de Supabase. | 🔴 | **Secreto. No versionar ni compartir.** |

---

## 4. Pruebas

| Archivo | Contenido | Marca | Cuándo modificarlo |
| --- | --- | --- | --- |
| `app/src/test/.../LocationValidatorTest.kt` | 12 casos del validador GPS, incluida la normalización del radio. | 🟢 | Si cambia la regla de distancia, precisión o catálogo de radios. |
| `app/src/test/.../FechaValidatorTest.kt` | 12 casos de rangos de fecha/hora y período de proyecto. | 🟢 | Si cambia la regla término ≥ inicio o el rango del proyecto. |
| `app/src/test/.../DateTimeUtilsTest.kt` | 7 casos de formatos y reloj. | 🟢 | Si cambia el formato de fecha/hora. |
| `app/src/test/.../DemoAuthTest.kt` | 4 casos de perfiles demo por rol y sesión. | 🟢 | Si cambia `DemoAuth`. |
| `app/src/test/.../ResponsablesTest.kt` | 3 casos de altas, bajas y cambio de rol. | 🟢 | Si cambia el cálculo de responsables. |
| `app/src/test/.../MappersTest.kt` | 7 casos de mapeo Room ↔ modelo (incluye auditoría). | 🟢 | Si cambian modelos/entidades. |
| `app/src/test/.../UbicacionEventoTest.kt` | 22 casos de parseo de enlaces/coordenadas y reglas de precisión. | 🟢 | Si cambia `UbicacionEvento.kt`. |
| `app/src/test/.../UbicacionesChileTest.kt` | 4 casos del catálogo de regiones y comunas. | 🟢 | Si cambia `UbicacionesChile.kt`. |
| `app/src/test/.../ExampleUnitTest.kt` | Prueba de sanidad. | 🔴 | No aporta; se puede eliminar. |

Comandos:

```bash
./gradlew :app:testDebugUnitTest --offline
./gradlew :app:jacocoDebugUnitTestReport --offline
./gradlew :app:assembleDebug --offline
```

---

## 5. Documentación

| Documento | Contenido | Marca | Cuándo modificarlo |
| --- | --- | --- | --- |
| `docs/README.md` | Índice, inicio rápido, demo y estado. | 🟢 | Cada entrega o cambio de alcance. |
| `docs/REGISTRO.md` | **Registro de trabajo por sesión:** objetivo, cambios, archivos y verificación. | 🟢 | **Siempre al cerrar una sesión.** |
| `docs/MAP.md` | Este mapa. | 🟢 | Si se agrega/elimina un archivo relevante. |
| `docs/PROBLEMATICA.md` | Contexto y problema. | 🟡 | Si cambia el caso o los requisitos de fondo. |
| `docs/REQUISITOS.md` | RF, RNF, reglas y casos de uso. | 🟢 | Cada cambio funcional. |
| `docs/ALCANCE.md` | MVP, fuera de alcance, rúbrica, roadmap. | 🟢 | Cambios de alcance o estado de la rúbrica. |
| `docs/TECNOLOGIAS.md` | Stack y versiones. | 🟡 | Subir versiones o cambiar stack. |
| `docs/ARQUITECTURA.md` | C4, modelo de datos, GPS, sync. | 🟢 | Cambios de arquitectura o esquema. |
| `docs/METODOLOGIA.md` | Iteraciones, DoD, pruebas, uso de IA. | 🟡 | Cambios de método. |
| `docs/AVANCES.md` | Bitácora y métricas. | 🟢 | **Siempre al cerrar un avance.** |
| `docs/CAMBIOS.md` | Changelog y compatibilidad. | 🟢 | **Siempre que se modifique comportamiento.** |
| `docs/MOCKUPS.md` | Wireframes en texto y trazabilidad. | 🟡 | Si cambian las vistas. |
| `docs/users.md` | Cuentas demo y avisos de seguridad. | 🟡 | Si cambian cuentas o credenciales. |
| `docs/mockups/index.html` | **Presentación visual interactiva** (15 Android + 5 Web). | 🟡 | Si cambian las pantallas o la propuesta web. |

---

## 6. Mapa de navegación entre pantallas

| Pantalla | Composable | ViewModel | Acciones principales |
| --- | --- | --- | --- |
| Login | `ui/login/LoginScreen.kt` | `MainActivity` | Cognito, acceso demo. |
| Shell por rol | `ui/dashboard/DashboardScreen.kt` | `DashboardViewModel` | Sync manual, banner local, logout. |
| Admin usuarios | `ui/panels/AdminPanel.kt` | `AdminViewModel` | CRUD usuarios/roles. |
| Docente resumen | `ui/panels/DocentePanel.kt` → `ResumenTab` | `DocenteViewModel` | Métricas, próximo evento. |
| Docente proyectos | `DocentePanel.kt` → `ProyectosTab` | `DocenteViewModel` | CRUD proyectos, docentes a cargo y “Ver eventos” con cambio de pestaña. |
| Docente eventos | `DocentePanel.kt` → `EventosTab` | `DocenteViewModel` | CRUD eventos, selector de proyecto, región/comuna, radio, QR y configuración GPS. |
| Docente informes | `DocentePanel.kt` → `InformesTab` | `DocenteViewModel` | Métricas, comentarios, informe. |
| Voluntario eventos | `ui/panels/VoluntarioPanel.kt` | `VoluntarioViewModel` | Lista y métricas. |
| Voluntario inscritos | `VoluntarioPanel.kt` | `VoluntarioViewModel` | Lista de inscritos. |
| Escáner QR | `ui/scanner/QrScannerScreen.kt` + `VoluntarioPanel.kt` | `VoluntarioViewModel` | QR + GPS + asistencia. |
| Beneficiario | `ui/panels/BeneficiarioPanel.kt` | `BeneficiarioViewModel` | Inscripción, QR, encuesta. |

Flujo de datos:

```text
Compose → ViewModel → SyncPassRepository ─┬→ Room (local)
                                          └→ SupabaseClient (remoto)
                                               ↓
                                          StateFlow → Compose
```

---

## 7. Regla de actualización al modificar

| Si cambias… | Toca también… |
| --- | --- |
| Modelos o entidades | `Mappers.kt`, `SyncPassDao.kt`, tests de mappers. |
| Esquema Room | `SyncPassDatabase.kt` (subir `version`), `ARQUITECTURA.md`. |
| Regla GPS o radio permitido | `LocationValidator.kt`, `LocationValidatorTest.kt`, `ARQUITECTURA.md`. |
| Evento dentro del período del proyecto | `FechaValidator.kt`, `FechaValidatorTest.kt`, `SyncPassRepository.kt`, `REQUISITOS.md`. |
| Responsables e historial | `Responsables.kt`, `ResponsableEntity`, `ResponsableHistorialEntity`, `SyncPassRepository.kt`, `DocentePanel.kt`. |
| Configuración GPS del evento | `UbicacionEvento.kt`, `UbicacionEventoDialog.kt`, `SyncPassRepository.kt`, `DocentePanel.kt`, `UbicacionEventoTest.kt`, `REQUISITOS.md`. |
| Perfiles demo | `DemoAuth.kt`, `LoginScreen.kt`, `users.md`. |
| Regiones o comunas | `UbicacionesChile.kt`, `RegionComunaFields`, `MOCKUPS.md`. |
| Flujo de datos | `SyncPassRepository.kt`, tests, `AVANCES.md`, `CAMBIOS.md`. |
| Pantalla | Su `ViewModel` y `Panel`; si cambia la vista, `MOCKUPS.md` y `docs/mockups/index.html`. |
| Datos demo | `LocalSeed.kt`, `docs/users.md`, `AVANCES.md`. |
| Cierre de sesión | `docs/REGISTRO.md` (entrada nueva), `AVANCES.md`, `CAMBIOS.md`. |
| Versión dependencia | `libs.versions.toml`, `TECNOLOGIAS.md`. |
| Configuración Supabase/Cognito | `local.properties` (secreto), `SupabaseConfig.kt`, `TECNOLOGIAS.md`. |

**Antes de cerrar cualquier cambio:** compilar, correr tests y actualizar `REGISTRO.md`, `AVANCES.md` y `CAMBIOS.md`.

---

## 8. Archivos y carpetas que NO se tocan

| Ruta | Motivo |
| --- | --- |
| `app/build/` | Salida generada. |
| `build/` | Salida generada. |
| `.gradle/` | Caché de Gradle. |
| `local.properties` | Contiene credenciales; no versionar. |
| `gradle/wrapper/**` | Wrapper estándar. |
| `data/api/AuthInterceptor.kt` | Sin uso actual; legado. |
| `docs/mockups/index.html` | Solo presentación; no es código de la app. |
| `app/src/main/res/drawable*/mipmap*` | Recursos generados/íconos; salvo cambio de marca. |

---

## 9. Prototipo web: SyncPass-Web

El prototipo web **ya existe** y está fuera del proyecto Android:

```text
/home/vowik/StudioProjects/
├── SyncPass-Movil/     ← app Android (este repositorio)
└── SyncPass-Web/       ← prototipo web estático
```

Reglas:

1. **No** crear `SyncPass-Web` dentro de `SyncPass-Movil`.
2. **No** agregarlo en `settings.gradle.kts`.
3. Reutilizar la estética de la sección **Web** de `docs/mockups/index.html`.
4. Mantener Supabase/Cognito como backend compartido cuando estén disponibles.
5. El prototipo tiene su propio mapa: `../../SyncPass-Web/docs/MAP.md`.
6. El HTML de mockups web y el prototipo son **propuestas visuales**, no código implementado con backend.

---

## 10. Referencias externas

| Recurso | Ubicación | Uso |
| --- | --- | --- |
| Rúbrica | `../SyncPass/EVA3/prueba3Android.md` | Criterios de evaluación. |
| Caso | `../SyncPass/Caso/caso.md` | Problema y requisitos de fondo. |
| Ejemplo de mockups | `~/Downloads/ej-mockup/index.html` | Referencia visual. |
| APK | `app/build/outputs/apk/debug/app-debug.apk` | Entregable demo. |
| Reporte JaCoCo | `app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html` | Cobertura. |
