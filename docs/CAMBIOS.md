# Registro de cambios

> **Formato:** basado en *Keep a Changelog* y versionado semántico adaptado a un entregable académico.
> **Versión actual del MVP:** `1.6.0` — 2026-11-17.
> **Registro rápido de sesiones:** [`REGISTRO.md`](REGISTRO.md).
> **Objetivo del registro:** explicar qué cambió, por qué, qué archivos se vieron afectados y cómo impacta la compatibilidad.

---

## 1. Resumen de versiones

| Versión | Fecha | Tipo | Resumen |
| --- | --- | --- | --- |
| `1.6.0` | 2026-11-17 | Menor | Configuración del GPS del evento desde su tarjeta: ubicación actual o enlace, sin exigirlo en el alta. |
| `1.5.0` | 2026-11-16 | Menor | Perfiles demo por rol, responsables múltiples con voluntarios e historial de asignaciones. |
| `1.4.0` | 2026-11-16 | Menor | “Ver eventos” abre la pestaña Eventos del proyecto y avisa si aún no tiene eventos. |
| `1.3.0` | 2026-11-16 | Menor | Los eventos deben quedar dentro del período de fechas del proyecto. |
| `1.2.0` | 2026-11-16 | Menor | Radio cerrado 20/40/60/80/100 m, región y comuna, formulario de evento sin latitud/longitud ni GPS. |
| `1.1.0` | 2026-11-16 | Menor | Selectores de fecha/hora, relación proyecto-evento explícita, docentes a cargo, auditoría y reloj en vivo. |
| `1.0.0-demo` | 2026-11-15 | Mayor | Modo local completo, validación GPS, sincronización explícita, login demo, pruebas y documentación. |
| `0.9.0` | 2026-11-14 | Base | Estado auditado: cliente Android con Supabase/Cognito, sin GPS, sin tests ni docs. |

---

## 2. `1.6.0` — 2026-11-17

### 2.1 Agregado

| Cambio | Archivos | Motivo |
| --- | --- | --- |
| Configuración GPS posterior al alta | `DocentePanel.kt`, `UbicacionEventoDialog.kt` | Georreferenciar un evento sin pedir coordenadas al crearlo. |
| Lectura de la ubicación actual como punto del evento | `UbicacionEventoDialog.kt`, `LocationProvider` | Obtener las coordenadas sin que el docente las conozca ni las escriba. |
| Pegado de enlaces de Google Maps o pares `lat, lng` | `UbicacionEvento.kt` (`CoordenadasParser`) | Alternativa cuando no se está en el recinto. |
| Aviso de precisión y radio recomendado | `UbicacionEvento.kt` (`UbicacionEventoValidator`) | Evitar puntos de referencia imprecisos que rechacen asistencias reales. |
| Estado GPS visible en la tarjeta del evento | `DocentePanel.kt` | Saber de un vistazo si el evento valida presencia. |
| Botón “Quitar ubicación GPS” | `UbicacionEventoDialog.kt` | Volver a `SIN_VALIDACION` sin borrar el evento. |
| 25 pruebas nuevas | `UbicacionEventoTest.kt`, `UbicacionesChileTest.kt` | Cubrir parseo, validación y catálogo territorial. |

### 2.2 Cambiado

| Cambio | Antes | Ahora | Impacto |
| --- | --- | --- | --- |
| Coordenadas del evento | Solo las traía el seed o se perdían al crear. | Se configuran o quitan desde la tarjeta del evento. | Los eventos creados pueden pasar a validación GPS real. |
| Alta de evento | No pedía coordenadas (desde `1.2.0`). | **Sigue sin pedirlas**; el guardado tampoco las exige. | No se agrega fricción ni obligación de estar en el recinto. |
| Radio de validación | Se elegía solo en el formulario de alta/edición. | También se puede ajustar al configurar el GPS. | Se puede corregir el radio según la precisión de la captura. |
| Persistencia de coordenadas | `updateEvento` reescribía todo el evento. | `updateEventoUbicacion` cambia solo latitud, longitud y radio. | No revalida fechas, cupos ni responsables. |
| Versión de la app | `1.0` (`versionCode 1`). | `1.6.0` (`versionCode 2`). | Se alinea con la numeración documentada. |
| Pruebas unitarias | 46. | 72. | Se agregan 22 de GPS y 4 del catálogo de regiones. |

### 2.3 Flujo de configuración GPS

1. El docente abre **Eventos** y presiona **“Configurar ubicación GPS”** en la tarjeta.
2. Elige uno de los caminos:
   - **“Usar mi ubicación actual”**: la app pide permiso si hace falta y muestra latitud, longitud y precisión.
   - **Pegar enlace o coordenadas**: interpreta enlaces de Google Maps y pares `latitud, longitud`.
3. Revisa el aviso de precisión; puede subir el radio a 20/40/60/80/100 m.
4. Guarda con **“Guardar ubicación”** o deja el evento sin GPS con **“Guardar sin GPS”**.
5. La tarjeta muestra el punto y el radio; el voluntario ya puede validar presencia al escanear.

> El alta del evento no cambia: se sigue creando sin coordenadas y las asistencias quedan `SIN_VALIDACION` hasta que se configure el punto.

### 2.4 Compatibilidad

| Aspecto | Estado |
| --- | --- |
| Base Room | **Versión 5, sin cambios.** `latitud`, `longitud` y `radioValidacionMetros` ya existían. |
| Contrato remoto | **Sin cambios de esquema.** Se actualizan `latitud`, `longitud` y `radio_validacion_metros` con *best-effort*. |
| Eventos existentes | Conservan sus coordenadas actuales; el seed mantiene el evento `202` georreferenciado. |
| QR y validación GPS | **Sin cambios** en la regla de distancia; el punto de referencia ahora es configurable. |
| Offline-first | **Reforzado.** La configuración se guarda primero en Room; si Supabase falla, no se pierde. |
| Login, roles y responsables | **Sin cambios.** |
| minSdk / targetSdk | 24 / 37, sin cambios. |

### 2.5 Archivos afectados

```text
app/src/main/java/cl/kcorp/syncpass/data/model/UbicacionEvento.kt              (nuevo)
app/src/main/java/cl/kcorp/syncpass/ui/components/UbicacionEventoDialog.kt      (nuevo)
app/src/main/java/cl/kcorp/syncpass/data/repository/SyncPassRepository.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocentePanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocenteViewModel.kt
app/src/test/java/cl/kcorp/syncpass/data/model/UbicacionEventoTest.kt          (nuevo)
app/src/test/java/cl/kcorp/syncpass/data/model/UbicacionesChileTest.kt         (nuevo)
app/build.gradle.kts
```

### 2.6 Verificación

- `./gradlew :app:testDebugUnitTest --offline` ✅ 72 pruebas, 0 fallos
- `./gradlew :app:jacocoDebugUnitTestReport --offline` ✅ 82,7% instrucciones · 79,3% ramas · 94,1% líneas
- `./gradlew :app:assembleDebug --offline` ✅ APK ~37 MB

---

## 3. `1.5.0` — 2026-11-16

### 2.1 Agregado

| Cambio | Archivos | Motivo |
| --- | --- | --- |
| Selector de rol y perfil en el acceso demo | `DemoAuth.kt`, `LoginScreen.kt`, `MainActivity.kt` | Entrar con distintos docentes, voluntarios o beneficiarios sin depender de Cognito. |
| Catálogo ampliado de perfiles demo | `DemoAuth.kt`, `DemoAuthTest.kt` | Al menos dos perfiles por rol, incluidos varios voluntarios. |
| Responsables múltiples para proyectos y eventos | `Models.kt`, `Entities.kt`, `SyncPassRepository.kt` | Asignar docentes y voluntarios, agregando o quitando personas con el tiempo. |
| Tabla genérica `responsable_cache` | `Entities.kt`, `SyncPassDao.kt` | Reemplaza la relación solo-docentes por responsables con rol. |
| Historial de responsables | `ResponsableHistorialEntity`, `SyncPassDao.kt`, `SyncPassRepository.kt` | Registrar altas y bajas de responsables con fecha y actor. |
| Selectores de voluntarios en proyectos y eventos | `DocentePanel.kt` | Asignar voluntarios a proyectos y actividades. |
| Diálogo “Historial de responsables” | `DocentePanel.kt` | Consultar los cambios de cada proyecto o evento. |
| Pruebas de cambios de responsables | `ResponsablesTest.kt` | Verificar altas, bajas y cambios de rol. |

### 2.2 Cambiado

| Cambio | Antes | Ahora | Impacto |
| --- | --- | --- | --- |
| Acceso demo | Un botón fijo por rol. | Se elige un rol y luego un perfil ficticio. | Permite probar varios docentes/voluntarios/beneficiarios. |
| Proyecto | Solo docentes a cargo. | Docentes **y** voluntarios a cargo. | Se pueden agregar o quitar responsables. |
| Evento | Sin responsables asignados. | Lista de docentes y voluntarios responsables. | Cada evento puede tener un equipo distinto. |
| Persistencia de responsables | Tabla `proyecto_docente`. | Tabla `responsable_cache` para proyectos y eventos. | Unifica docentes y voluntarios. |
| Tarjetas de proyecto/evento | Mostraban solo docentes del proyecto. | Muestran responsables actuales y acceso al historial. | Trazabilidad visible. |
| Eliminación de usuario | Solo borraba al usuario. | También retira sus asignaciones actuales. | Evita responsables huérfanos. |
| Base Room | Versión 4. | Versión 5 con migración destructiva. | Nuevas tablas de responsables e historial. |
| Pruebas unitarias | 42. | 46. | Nuevas pruebas de perfiles demo y cambios de responsables. |

### 2.3 Historial y responsables

- Cada proyecto y evento guarda una lista de responsables con su rol (`DOCENTE` o `VOLUNTARIO`).
- El historial registra `AGREGADO` y `ELIMINADO` con nombre, rol, fecha y quién realizó el cambio.
- Un cambio de rol se registra como una baja y un alta.
- La verificación se hace contra las tablas locales `responsable_cache` y `responsable_historial`.
- La gestión de perfiles de usuario sigue en **Administrador → Usuarios**; desde ahí se agregan, editan o eliminan docentes, voluntarios y beneficiarios.

### 2.4 Compatibilidad

| Aspecto | Estado |
| --- | --- |
| Base Room | **Versión 5.** Migración destructiva: se reconstruye la caché y se reejecuta el seed. |
| Datos remotos | **Modo de pruebas local.** Supabase está offline por ahora. El CRUD, las validaciones, los responsables y el historial se prueban en Room; la sincronización con el modelo de Supabase se hará en una fase posterior. |
| Proyectos y eventos existentes | Se recrean desde el seed o se cargan del backend; la asignación local se reconstruye desde el historial local. |
| QR y validación GPS | **Sin cambios.** |
| Login Cognito | **Sin cambios.** El acceso demo solo aparece sin Supabase configurado. |
| minSdk / targetSdk | 24 / 37, sin cambios. |

### 2.5 Archivos afectados

```text
app/src/main/java/cl/kcorp/syncpass/MainActivity.kt
app/src/main/java/cl/kcorp/syncpass/data/auth/DemoAuth.kt
app/src/main/java/cl/kcorp/syncpass/data/local/Entities.kt
app/src/main/java/cl/kcorp/syncpass/data/local/SyncPassDao.kt
app/src/main/java/cl/kcorp/syncpass/data/local/SyncPassDatabase.kt
app/src/main/java/cl/kcorp/syncpass/data/local/LocalSeed.kt
app/src/main/java/cl/kcorp/syncpass/data/model/Models.kt
app/src/main/java/cl/kcorp/syncpass/data/model/Responsables.kt                 (nuevo)
app/src/main/java/cl/kcorp/syncpass/data/repository/SyncPassRepository.kt
app/src/main/java/cl/kcorp/syncpass/ui/login/LoginScreen.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocentePanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocenteViewModel.kt
app/src/test/java/cl/kcorp/syncpass/data/auth/DemoAuthTest.kt
app/src/test/java/cl/kcorp/syncpass/data/model/ResponsablesTest.kt             (nuevo)
```

### 2.6 Verificación

- `./gradlew :app:compileDebugKotlin` ✅
- `./gradlew :app:testDebugUnitTest` ✅ 46 pruebas, 0 fallos

---

## 4. `1.4.0` — 2026-11-16

### 3.1 Agregado

| Cambio | Archivos | Motivo |
| --- | --- | --- |
| Navegación directa desde “Ver eventos” a la pestaña Eventos | `ui/panels/DocentePanel.kt` | Evitar que el docente tenga que cambiar de pestaña manualmente. |
| Conteo de eventos por proyecto | `ui/panels/DocenteViewModel.kt` | Saber si un proyecto tiene actividades antes de abrirlo. |
| Aviso de proyecto sin eventos | `DocentePanel.kt` | Indicar “Aún no tiene eventos creados.” en la tarjeta del proyecto. |

### 3.2 Cambiado

| Cambio | Antes | Ahora | Impacto |
| --- | --- | --- | --- |
| Botón “Ver eventos” | Solo seleccionaba el proyecto. | Selecciona el proyecto y cambia a la pestaña **Eventos**. | Un toque lleva a los eventos correctos. |
| Tarjeta de proyecto | No mostraba si tenía actividades. | Muestra “N eventos creados” o “Aún no tiene eventos creados.”. | El docente sabe qué esperar antes de entrar. |
| Estado vacío de Eventos | “Este proyecto aún no tiene eventos.” | Incluye el nombre del proyecto seleccionado. | Mensaje más claro. |
| Estado del ViewModel | No tenía conteo por proyecto. | `eventosPorProyecto` se carga al iniciar y se actualiza al crear/eliminar eventos; `eventosLoading` evita mostrar “sin eventos” mientras carga. | La tarjeta y la pestaña quedan sincronizadas con los cambios. |

### 3.3 Compatibilidad

| Aspecto | Estado |
| --- | --- |
| Base Room | **Versión 4, sin cambios.** |
| Contrato remoto | **Sin cambios.** |
| Datos existentes | **Sin migración.** El conteo se calcula en memoria. |
| Pestañas y filtros | **Conservados.** El filtro por proyecto sigue siendo el mismo. |

### 3.4 Archivos afectados

```text
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocentePanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocenteViewModel.kt
```

### 3.5 Verificación

- `./gradlew :app:compileDebugKotlin` ✅
- `./gradlew :app:testDebugUnitTest` ✅ 42 pruebas, 0 fallos

---

## 5. `1.3.0` — 2026-11-16

### 4.1 Agregado

| Cambio | Archivos | Motivo |
| --- | --- | --- |
| Validación del evento dentro del período del proyecto | `data/model/FechaValidator.kt` | Evitar programar actividades antes del inicio o después del término del proyecto. |
| Mensaje con el período permitido en el formulario | `ui/panels/DocentePanel.kt` | Mostrar al docente el rango válido antes de guardar. |
| Bloqueo del guardado fuera de rango en alta y edición | `DocentePanel.kt` | Impedir el error desde la interfaz. |
| Validación de respaldo en el repositorio | `SyncPassRepository.kt` | Proteger también las llamadas que no pasan por la UI. |
| Pruebas de período | `FechaValidatorTest.kt` | Cubrir antes, después, primer día y último día. |

### 4.2 Cambiado

| Cambio | Antes | Ahora | Impacto |
| --- | --- | --- | --- |
| Validación de fechas de evento | Solo se exigía término ≥ inicio. | Además, el evento debe quedar dentro del período del proyecto. | No se pueden programar eventos fuera del proyecto. |
| Diálogo de edición de evento | Recibía solo el nombre del proyecto. | Recibe el proyecto completo para conocer sus fechas. | Misma validación al editar. |
| Formulario de evento | No mostraba el período del proyecto. | Muestra “Período del proyecto: …” y el error si el evento se sale. | El usuario entiende por qué no puede guardar. |

### 4.3 Regla aplicada

- El evento debe comenzar en la fecha de inicio del proyecto o después.
- El evento debe terminar en la fecha de término del proyecto o antes.
- Como el proyecto usa solo fecha, el **último día completo** es válido (se admite cualquier hora de ese día).
- La validación se aplica en la UI (botón deshabilitado + mensaje) y en el repositorio (rechazo de la operación).

### 4.4 Compatibilidad

| Aspecto | Estado |
| --- | --- |
| Eventos existentes | **Sin migración de datos.** Se mantienen; la regla se aplica al crear o editar. |
| Base Room | **Versión 4, sin cambios.** |
| Contrato remoto | **Sin cambios.** |
| Formato de fechas | **Sin cambios:** proyecto `yyyy-MM-dd`, evento `yyyy-MM-ddTHH:mm`. |
| QR y validación GPS | **Sin cambios.** |
| minSdk / targetSdk | 24 / 37, sin cambios. |

### 4.5 Archivos afectados

```text
app/src/main/java/cl/kcorp/syncpass/data/model/FechaValidator.kt
app/src/main/java/cl/kcorp/syncpass/data/repository/SyncPassRepository.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocentePanel.kt
app/src/test/java/cl/kcorp/syncpass/data/model/FechaValidatorTest.kt
```

### 4.6 Verificación

- `./gradlew :app:compileDebugKotlin` ✅
- `./gradlew :app:testDebugUnitTest` ✅ 42 pruebas, 0 fallos
- `FechaValidatorTest` ✅ 12 casos (5 nuevos de período de proyecto)

---

## 6. `1.2.0` — 2026-11-16

### 5.1 Agregado

| Cambio | Archivos | Motivo |
| --- | --- | --- |
| Región y comuna del evento | `data/model/UbicacionesChile.kt` (nuevo), `Models.kt`, `Entities.kt`, `Mappers.kt` | Ubicar el recinto con listas cerradas y evitar errores de escritura. |
| Selector dependiente región → comuna | `ui/components/Selectors.kt`, `DocentePanel.kt` | Al elegir región se cargan solo sus comunas. |
| Selector cerrado de radio de validación | `ui/components/Selectors.kt`, `LocationValidator.kt` | Restringir el radio a 20, 40, 60, 80 o 100 m. |
| Normalización del radio | `LocationValidator.kt`, `SyncPassRepository.kt` | Ajustar valores antiguos o inválidos al valor permitido más cercano. |
| Pruebas de radio | `LocationValidatorTest.kt` | Verificar la normalización y la validación con radio ajustado. |
| Región/comuna en tarjetas de evento | `DocentePanel.kt`, `VoluntarioPanel.kt` | Mostrar la ubicación legible del recinto. |

### 5.2 Cambiado

| Cambio | Antes | Ahora | Impacto |
| --- | --- | --- | --- |
| Formulario de evento | Latitud, longitud y botón “Usar mi ubicación actual”. | Región, comuna y radio cerrado; sin campos GPS. | Crear un evento no exige estar físicamente en el recinto. |
| Radio de validación | Texto libre con cualquier número entero. | Solo `20`, `40`, `60`, `80` o `100` m. | No se pueden ingresar valores fuera del catálogo. |
| Evento con GPS del seed | Radio 120 m. | Radio 100 m. | Coincide con el catálogo permitido. |
| Base Room | Versión 3. | Versión 4 con migración destructiva. | La caché se reconstruye y el seed se reejecuta. |
| Tarjetas de evento | Mostraban solo recinto y dirección. | Además muestran comuna y región cuando existen. | Ubicación más clara para docente y voluntario. |
| Edición de evento | Permitía cambiar coordenadas y usar GPS. | Conserva las coordenadas existentes y solo permite cambiar región/comuna/radio. | No se pierden eventos ya georreferenciados. |

### 5.3 Retirado

- Campos **Latitud** y **Longitud** del formulario de creación y del diálogo de edición.
- Botón **“Usar mi ubicación actual”** y el composable `LocationCaptureButton`.
- Conversión manual de coordenadas (`toCoordinate`) y sus imports asociados.

### 5.4 Compatibilidad

| Aspecto | Estado |
| --- | --- |
| Eventos existentes con coordenadas | **Conservadas.** Al editar se mantienen latitud/longitud y la validación GPS sigue activa. |
| Eventos nuevos | **Sin coordenadas.** La asistencia se registra como `SIN_VALIDACION` hasta configurar GPS más adelante. |
| Contrato remoto | **Conservado.** `region` y `comuna` se envían *best-effort*; si el esquema no las tiene, se reintenta sin ellas. |
| Radio remoto/local | **Normalizado** antes de guardar. |
| Base Room | **Versión 4.** Migración destructiva: se pierde la caché local y se reejecuta el seed. |
| QR y validación GPS | **Sin cambios** en el escáner del voluntario. |
| minSdk / targetSdk | 24 / 37, sin cambios. |

### 5.5 Archivos afectados

```text
app/src/main/java/cl/kcorp/syncpass/data/model/Models.kt
app/src/main/java/cl/kcorp/syncpass/data/model/UbicacionesChile.kt            (nuevo)
app/src/main/java/cl/kcorp/syncpass/data/local/Entities.kt
app/src/main/java/cl/kcorp/syncpass/data/local/Mappers.kt
app/src/main/java/cl/kcorp/syncpass/data/local/LocalSeed.kt
app/src/main/java/cl/kcorp/syncpass/data/local/SyncPassDatabase.kt
app/src/main/java/cl/kcorp/syncpass/data/location/LocationValidator.kt
app/src/main/java/cl/kcorp/syncpass/data/repository/SyncPassRepository.kt
app/src/main/java/cl/kcorp/syncpass/ui/components/Selectors.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocentePanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocenteViewModel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/VoluntarioPanel.kt
app/src/test/java/cl/kcorp/syncpass/data/location/LocationValidatorTest.kt
```

### 5.6 Verificación

- `./gradlew :app:compileDebugKotlin` ✅
- `./gradlew :app:testDebugUnitTest` ✅ 37 pruebas, 0 fallos
- `LocationValidatorTest` ✅ 12 casos (incluye 2 nuevos de normalización de radio)

---

## 7. `1.1.0` — 2026-11-16

### 6.1 Agregado

| Cambio | Archivos | Motivo |
| --- | --- | --- |
| Selectores de fecha y hora con Material 3 | `ui/components/DateTimeFields.kt` | Evitar fechas mal escritas y formatos inconsistentes. |
| Selector de proyecto en el formulario de eventos | `ui/components/Selectors.kt`, `ui/panels/DocentePanel.kt` | Hacer explícita la relación proyecto-evento sin salir de la pestaña. |
| Asignación de uno o varios docentes por proyecto | `proyecto_docente` (Room), `DocentesMultiSelect` | Cumplir la responsabilidad docente sobre proyectos y actividades. |
| Reloj en vivo en el dashboard | `ui/components/DateTimeFields.kt`, `ui/dashboard/DashboardScreen.kt` | Mostrar la fecha y hora actuales de la app. |
| Auditoría de creación y asistencia | `Models.kt`, `Entities.kt`, `Mappers.kt`, repositorio | Saber quién creó cada proyecto/evento y quién acreditó cada asistencia, con fecha y hora. |
| Validación de rango de fechas | `data/model/FechaValidator.kt` | Impedir que el término quede antes del inicio. |
| Pruebas de fechas y formatos | `FechaValidatorTest`, `DateTimeUtilsTest` | Evidencia de la nueva lógica pura. |

### 6.2 Cambiado

| Cambio | Antes | Ahora | Impacto |
| --- | --- | --- | --- |
| Formularios de proyecto | Fechas en texto `AAAA-MM-DD`. | Selector de calendario y validación de rango. | No se puede escribir una fecha inválida. |
| Formularios de evento | Fechas y horas en texto `AAAA-MM-DDTHH:mm`. | Calendario + reloj, con validación de rango. | Menos errores de digitación. |
| Creación de eventos | Requería seleccionar el proyecto en otra pestaña. | Selector de proyecto dentro de la pestaña y del formulario. | Flujo más simple y comprensible. |
| Proyecto | Un `idCoordinador`. | `idCoordinador` + docentes N:M en `proyecto_docente`. | Se puede asignar uno o varios docentes. |
| Asistencia | Solo `estadoValidacion`. | Además `registradoPor` y `fechaMarcacion`. | Trazabilidad de la acreditación. |
| Base Room | Versión 2. | Versión 3 con migración destructiva. | La caché se reconstruye y el seed se reejecuta. |

### 6.3 Corregido

- **Fechas de término anteriores al inicio:** ahora se bloquea el guardado con un mensaje claro.
- **Eventos sin proyecto explícito:** el formulario siempre muestra y permite elegir el proyecto.
- **Proyectos sin docente a cargo:** el alta exige al menos un docente (el creador se preselecciona).
- **Asistencias sin responsable:** se guarda quién las registró y cuándo.
- **Reloj ausente:** el dashboard muestra fecha y hora actualizadas cada segundo.

### 6.4 Compatibilidad

| Aspecto | Estado |
| --- | --- |
| Modo remoto con Supabase | **Conservado.** Las columnas nuevas se envían best-effort; si no existen, se reintenta sin ellas. |
| Login Cognito y acceso demo | **Conservado.** |
| Eventos sin coordenadas | **Conservado.** Se registran como `SIN_VALIDACION`. |
| Base Room | **Versión 3.** Migración destructiva: se pierde la caché local y se reejecuta el seed. |
| Formato de fechas | **Sin cambios en el contrato:** `yyyy-MM-dd` y `yyyy-MM-ddTHH:mm`. |
| minSdk / targetSdk | 24 / 37, sin cambios. |

### 6.5 Archivos afectados

```text
app/src/main/java/cl/kcorp/syncpass/data/model/Models.kt
app/src/main/java/cl/kcorp/syncpass/data/model/FechaValidator.kt              (nuevo)
app/src/main/java/cl/kcorp/syncpass/data/local/Entities.kt
app/src/main/java/cl/kcorp/syncpass/data/local/Mappers.kt
app/src/main/java/cl/kcorp/syncpass/data/local/SyncPassDao.kt
app/src/main/java/cl/kcorp/syncpass/data/local/SyncPassDatabase.kt
app/src/main/java/cl/kcorp/syncpass/data/local/LocalSeed.kt
app/src/main/java/cl/kcorp/syncpass/data/repository/SyncPassRepository.kt
app/src/main/java/cl/kcorp/syncpass/ui/components/DateTimeUtils.kt           (nuevo)
app/src/main/java/cl/kcorp/syncpass/ui/components/DateTimeFields.kt          (nuevo)
app/src/main/java/cl/kcorp/syncpass/ui/components/Selectors.kt               (nuevo)
app/src/main/java/cl/kcorp/syncpass/ui/dashboard/DashboardScreen.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocentePanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocenteViewModel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/VoluntarioPanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/VoluntarioViewModel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/BeneficiarioPanel.kt
app/build.gradle.kts
app/src/test/java/cl/kcorp/syncpass/data/model/FechaValidatorTest.kt         (nuevo)
app/src/test/java/cl/kcorp/syncpass/ui/components/DateTimeUtilsTest.kt       (nuevo)
app/src/test/java/cl/kcorp/syncpass/data/local/MappersTest.kt
```

### 6.6 Verificación

- `./gradlew :app:compileDebugKotlin --offline` ✅
- `./gradlew :app:testDebugUnitTest --offline` ✅ 35 pruebas
- `./gradlew :app:jacocoDebugUnitTestReport --offline` ✅ 71,9% instrucciones
- `./gradlew :app:assembleDebug --offline` ✅

---

## 8. `1.0.0-demo` — 2026-11-15

### 7.1 Agregado

| Cambio | Archivos | Motivo |
| --- | --- | --- |
| Dataset ficticio de demostración | `data/local/LocalSeed.kt` | Permitir la demo sin Supabase/EC2. |
| Consultas locales de usuarios, roles, proyectos, eventos, inscripciones y métricas | `data/local/SyncPassDao.kt` | Soportar todos los paneles en modo local. |
| Fallbacks locales en el repositorio | `data/repository/SyncPassRepository.kt` | Evitar pantallas vacías cuando el backend no responde. |
| Métricas locales desde Room | `data/repository/SyncPassRepository.kt` | Informes operativos sin backend. |
| Snapshot de ubicación independiente de Android | `data/location/LocationValidator.kt` | Testear GPS en JVM. |
| Validador Haversine con radio, tolerancia y estados | `data/location/LocationValidator.kt` | Verificar presencia física real. |
| Proveedor GPS con precisión y antigüedad | `data/location/LocationProvider.kt` | Decidir con datos confiables. |
| Tarjeta de estado GPS en el escáner | `ui/panels/VoluntarioPanel.kt` | Mostrar distancia, precisión y motivo del bloqueo. |
| Captura de coordenadas del evento | `ui/panels/DocentePanel.kt` | Evitar ingreso manual de latitud/longitud. |
| Sincronización manual con contador y mensaje | `ui/dashboard/DashboardViewModel.kt`, `DashboardScreen.kt` | Cumplir el requisito de sincronización explícita. |
| Acceso demo por rol | `data/auth/DemoAuth.kt`, `ui/login/LoginScreen.kt`, `MainActivity.kt` | Entrar a cada panel sin Cognito. |
| Payload de asistencia con `estado_validacion` | `data/repository/SyncPassRepository.kt` | Persistir si hubo validación GPS. |
| Pruebas de dominio | `app/src/test/**` | Evidencia de calidad. |
| JaCoCo y tarea de reporte | `app/build.gradle.kts` | Medir cobertura ≥ 30%. |
| Documentación completa | `docs/**` | C4, alcance, requisitos, mockups y bitácora. |

### 7.2 Cambiado

| Cambio | Antes | Ahora | Impacto |
| --- | --- | --- | --- |
| Firma de `registrarAsistencia` | `(id, lat?, lng?)` | `(id, lat?, lng?, estadoValidacion)` | Interno; los llamadores se actualizaron. |
| `LocationProvider.currentLocation` | `Location?` | `LocationSnapshot?` | Interno; desacopla Android del dominio. |
| Flujo del voluntario | Escaneaba y registraba directo. | Valida GPS y bloquea si corresponde. | Cambio funcional visible. |
| Formularios de evento | Latitud/longitud solo manuales. | Botón “Usar mi ubicación actual”. | Mejora de usabilidad. |
| Métricas remotas | Solo `VALIDADA`. | `VALIDADA` + `SIN_VALIDACION`. | Refleja también eventos sin coordenadas. |
| Banners sin Supabase | Error rojo. | Banner informativo de modo local. | La demo ya no parece fallida. |
| Login | Solo Cognito. | Cognito + acceso demo. | Demo sin nube. |
| Dashboard | Sync automática silenciosa. | Botón manual + estado + mensaje. | Requisito de rúbrica. |
| Seed | No existía. | Se ejecuta solo si Room está vacía. | Primer arranque con datos. |

### 7.3 Corregido

- **Falsos negativos por precisión GPS:** se agrega tolerancia acotada en lugar de comparar solo `distancia ≤ radio`.
- **Asistencias de eventos sin coordenadas:** antes quedaban fuera de las métricas; ahora se cuentan como `SIN_VALIDACION`.
- **Informes vacíos en modo local:** ahora se calculan desde Room.
- **Mensajes de error engañosos:** Admin y Beneficiario muestran banner informativo, no error.
- **Pérdida de operaciones sin red:** la asistencia se encola con su estado de validación.
- **Reutilización de GPS:** se evita pedir una lectura nueva si la última tiene menos de 90 s.

### 7.4 Seguridad

- No se incorporaron tokens ni credenciales reales al código.
- La configuración remota se inyecta por `local.properties` o variables de entorno.
- Los datos sembrados son ficticios.
- Se documentó la necesidad de rotar cualquier credencial expuesta.
- El QR se valida por formato y pertenencia al evento seleccionado.

### 7.5 Compatibilidad

| Aspecto | Estado |
| --- | --- |
| Modo remoto con Supabase configurado | **Conservado.** El flujo remoto se intenta primero; el local es fallback. |
| Login Cognito | **Conservado.** El acceso demo solo aparece si Supabase no está configurado. |
| Eventos sin coordenadas | **Conservado.** Se registran como `SIN_VALIDACION`. |
| Base Room | **Versión 2 sin cambios de esquema.** Solo se agregaron consultas y un parámetro a `markAsistencia`. |
| IDs locales | **Negativos**, para no colisionar con IDs remotos positivos. |
| API pública de la app | Sin cambios externos; los cambios de firma son internos. |
| minSdk / targetSdk | 24 / 37, sin cambios. |

> **Nota sobre el seed:** si el dispositivo ya tiene una base Room con proyectos o usuarios, el seed **no** se ejecuta. Para forzar el dataset demo hay que borrar los datos de la app o desinstalarla.

### 7.6 Archivos afectados

**Código principal**

```text
app/src/main/java/cl/kcorp/syncpass/MainActivity.kt
app/src/main/java/cl/kcorp/syncpass/data/auth/DemoAuth.kt              (nuevo)
app/src/main/java/cl/kcorp/syncpass/data/local/LocalSeed.kt            (nuevo)
app/src/main/java/cl/kcorp/syncpass/data/local/SyncPassDao.kt
app/src/main/java/cl/kcorp/syncpass/data/location/LocationProvider.kt  (reescrito)
app/src/main/java/cl/kcorp/syncpass/data/location/LocationValidator.kt (nuevo)
app/src/main/java/cl/kcorp/syncpass/data/repository/SyncPassRepository.kt
app/src/main/java/cl/kcorp/syncpass/ui/dashboard/DashboardScreen.kt
app/src/main/java/cl/kcorp/syncpass/ui/dashboard/DashboardViewModel.kt
app/src/main/java/cl/kcorp/syncpass/ui/login/LoginScreen.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/AdminPanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/BeneficiarioPanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/DocentePanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/VoluntarioPanel.kt
app/src/main/java/cl/kcorp/syncpass/ui/panels/VoluntarioViewModel.kt
```

**Pruebas y build**

```text
app/build.gradle.kts
app/src/test/java/cl/kcorp/syncpass/data/auth/DemoAuthTest.kt          (nuevo)
app/src/test/java/cl/kcorp/syncpass/data/local/MappersTest.kt          (nuevo)
app/src/test/java/cl/kcorp/syncpass/data/location/LocationValidatorTest.kt (nuevo)
```

**Documentación**

```text
docs/README.md
docs/PROBLEMATICA.md
docs/REQUISITOS.md
docs/ALCANCE.md
docs/TECNOLOGIAS.md
docs/ARQUITECTURA.md
docs/METODOLOGIA.md
docs/AVANCES.md
docs/CAMBIOS.md
docs/MOCKUPS.md
docs/MAP.md
docs/mockups/index.html
docs/users.md
```

### 7.7 Migración y reversión

**Migración**

1. No se requiere migración de esquema Room.
2. Si se actualiza una instalación existente sin datos demo, desinstalar o limpiar datos para que el seed se ejecute.
3. Si se configuran credenciales de Supabase, la app vuelve al flujo remoto sin cambios de código.

**Reversión**

- Revertir los archivos de la sección 7.6 al estado `0.9.0`.
- Retirar el plugin/tarea JaCoCo de `app/build.gradle.kts`.
- Retirar `DemoAuth` y el parámetro `estadoValidacion` si se vuelve al contrato anterior.
- La base Room puede quedar con datos demo; limpiar datos de la app.

---

## 9. Cambios significativos del MVP

Esta tabla resume los cambios que un evaluador debería notar respecto de la versión anterior.

| # | Cambio significativo | Por qué importa | Dónde verlo |
| --- | --- | --- | --- |
| 1 | La app funciona sin Supabase/EC2. | Permite la demo en el entorno actual. | Banner de modo local + seed. |
| 2 | Cualquier rol entra con un toque. | Elimina la dependencia de Cognito para evaluar. | Login → “Acceso rápido de demostración”. |
| 3 | La asistencia se valida por GPS. | Cumple el requisito de presencia física. | Escáner del voluntario. |
| 4 | El bloqueo por distancia es explícito. | Evita acreditaciones desde fuera del evento. | `GpsStatusCard` + mensaje. |
| 5 | Los eventos sin coordenadas siguen funcionando. | No rompe el QR existente. | Evento “Taller de Smartphone Básico”. |
| 6 | El formulario de evento usa listas cerradas para región, comuna y radio. | Reduce errores de configuración y no exige GPS al crear. | Crear/editar evento. |
| 7 | La sincronización es visible y manual. | Requisito explícito de la rúbrica. | Dashboard → “Sincronizar ahora”. |
| 8 | Los informes funcionan en local. | La demo no queda con métricas vacías. | Panel Docente → Informes. |
| 9 | Hay pruebas y cobertura medible. | Evidencia objetiva de calidad. | `testDebugUnitTest`, JaCoCo. |
| 10 | La documentación cubre C4, alcance y mockups. | Facilita la defensa individual. | `/docs`. |
| 11 | Los eventos deben quedar dentro del período del proyecto. | Evita programar actividades fuera del plazo del proyecto. | Crear/editar evento. |
| 12 | El acceso demo permite elegir rol y perfil. | Se pueden probar varios docentes/voluntarios sin crear cuentas. | Login → Acceso rápido. |
| 13 | Proyectos y eventos tienen responsables con historial. | Trazabilidad de quién estuvo a cargo y cuándo cambió. | Tarjetas → Historial de responsables. |

---

## 10. Deuda técnica y cambios pendientes

| Prioridad | Cambio pendiente | Impacto esperado |
| --- | --- | --- |
| Alta | API Spring Boot + despliegue EC2. | Cumplir el backend exigido por la rúbrica. |
| Alta | Configuración y validación de Supabase. | Verificar sync remota real. |
| Alta | Pruebas instrumentadas de GPS/cámara. | Cobertura de flujos nativos. |
| Media | Exportación PDF/CSV. | Entregables de informe. |
| Media | Notificaciones. | Recordatorios de eventos. |
| Media | Sincronizar las coordenadas GPS configuradas con el esquema remoto. | Validación de presencia consistente entre dispositivos. |
| Media | Llevar responsables e historial al esquema de Supabase. | Sincronizar equipos entre dispositivos. |
| Media | Refrescar perfiles al volver desde Administrador. | Ver de inmediato docentes/voluntarios nuevos. |
| Baja | Mapas para seleccionar recinto. | Configuración más simple. |

---

## 11. Control de cambios

Todo cambio futuro debe:

1. Registrarse en este documento con versión, fecha y tipo.
2. Indicar archivos afectados y motivo.
3. Actualizar los requisitos y el alcance si corresponde.
4. Añadir o ajustar pruebas.
5. Verificar compilación, tests y cobertura antes de cerrar el cambio.
6. Señalar si afecta compatibilidad offline, modelo de datos o contrato remoto.

### Plantilla

```markdown
## [versión] — AAAA-MM-DD

### Agregado
- ...

### Cambiado
- ...

### Corregido
- ...

### Compatibilidad
- ...

### Archivos afectados
- ...

### Verificación
- [ ] Compila
- [ ] Tests
- [ ] Cobertura
- [ ] Documentación actualizada
```
