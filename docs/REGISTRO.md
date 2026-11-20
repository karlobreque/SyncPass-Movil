# Registro de trabajo

> **Para qué sirve:** tener en un solo lugar qué se hizo, cuándo, en qué archivos y cómo se verificó. Se lee la sesión más reciente y no hace falta buscar en todo el proyecto.
> **Última actualización:** 2026-11-17 (sesión `1.6.0`).
> **Regla:** cada sesión agrega una entrada al inicio. No se reescribe el historial. El detalle normativo vive en `CAMBIOS.md`; las métricas completas viven en `AVANCES.md`.

---

## Cómo usar este registro

1. Lee la sesión más reciente (la primera de abajo).
2. Revisa **Objetivo**, **Cambios** y **Archivos**.
3. Si necesitas el detalle completo, abre el documento enlazado.
4. Para continuar el trabajo, revisa **Pendientes**.
5. Antes de tocar un archivo, confirma en `MAP.md` si es de trabajo o si no debe modificarse.

---

## Sesión 2026-11-17 — versión `1.6.0`

### Objetivo

Permitir que un evento creado sin coordenadas pueda georreferenciarse después, desde su propia tarjeta, sin volver obligatorio el GPS en el formulario de alta. El docente puede leer la ubicación actual si está en el recinto o pegar un enlace de Google Maps / un par de coordenadas. Si no configura nada, el evento sigue funcionando y las asistencias quedan como `SIN_VALIDACION`.

### Cambios realizados

#### 1. Estado GPS visible en la tarjeta del evento

- La tarjeta muestra **“GPS: ‑33.51130, ‑70.75800 · radio 100 m”** cuando el evento ya tiene punto de referencia.
- Si no lo tiene, muestra **“GPS: sin coordenadas · las asistencias quedan SIN VALIDACIÓN”**.
- Nuevo botón **“Configurar ubicación GPS”** / **“Cambiar ubicación GPS”** en cada evento.

#### 2. Diálogo “Ubicación GPS del evento”

- Recuerda que configurar el GPS **no es obligatorio** para guardar el evento.
- **“Usar mi ubicación actual”**: solicita permiso de ubicación si hace falta y muestra el punto y la precisión informada por el GPS.
- Campo para **pegar un enlace de Google Maps** o un par `latitud, longitud`; el botón “Leer coordenadas” lo interpreta.
- Permite **cambiar el radio** (20/40/60/80/100 m) en el mismo diálogo.
- **“Quitar ubicación GPS”** vuelve a dejar el evento sin validación.
- El botón de guardar se etiqueta **“Guardar ubicación”** o **“Guardar sin GPS”** según el caso.

#### 3. Lectura de enlaces y coordenadas

- `CoordenadasParser` reconoce:
  - par simple `-33.51130, -70.75800` (coma o punto y coma);
  - enlaces con `@lat,lng`;
  - parámetros `?q=`, `?ll=`, `?center=`, `?destination=`, `?sll=`, `?daddr=`;
  - formato interno `!3d…!4d…` de Google Maps;
  - esquema `geo:lat,lng`.
- Acepta el signo menos unicode (`−`) y espacios no separables.
- Rechaza latitudes fuera de [-90, 90], longitudes fuera de [-180, 180] y textos sin coordenadas.
- Detecta enlaces cortos (`maps.app.goo.gl`) y explica que requieren abrirlos en el navegador para copiar la URL completa.

#### 4. Calidad y seguridad de la referencia

- `UbicacionEventoValidator` advierte cuando la precisión de la captura supera la mitad del radio elegido y sugiere un radio mayor (máximo 100 m).
- Una lectura con precisión mayor a 250 m **no se acepta como referencia**: se debe reintentar o usar un enlace. Guardar sin GPS sigue permitido.

#### 5. Persistencia y estado

- `SyncPassRepository.updateEventoUbicacion(...)` guarda solo `latitud`, `longitud` y `radioValidacionMetros`, sin revalidar fechas, cupos ni responsables.
- Exige que latitud y longitud vengan ambos o ninguno.
- Actualiza Room primero y hace *best-effort* contra Supabase cuando está disponible; sin red, el cambio queda local.
- `DocenteViewModel.configurarUbicacion(...)` refleja el cambio en el estado sin recargar toda la lista.
- La versión de la app sube a `1.6.0` (`versionCode 2`).

### Archivos creados

| Archivo | Para qué |
| --- | --- |
| `data/model/UbicacionEvento.kt` | `CoordenadasEvento`, `CoordenadasParser` y `UbicacionEventoValidator`. **Código puro.** |
| `ui/components/UbicacionEventoDialog.kt` | Diálogo de configuración GPS con permiso, lectura actual y pegado de enlaces. |
| `app/src/test/.../UbicacionEventoTest.kt` | 22 casos de parseo, validación y precisión. |
| `app/src/test/.../UbicacionesChileTest.kt` | 4 casos del catálogo territorial (cobertura del seed de regiones). |

### Archivos modificados

| Archivo | Cambio |
| --- | --- |
| `ui/panels/DocentePanel.kt` | Estado GPS en la tarjeta, botón y apertura del diálogo. |
| `ui/panels/DocenteViewModel.kt` | `configurarUbicacion(...)`. |
| `data/repository/SyncPassRepository.kt` | `updateEventoUbicacion(...)` con validación y persistencia local/remota. |
| `app/build.gradle.kts` | `versionName = "1.6.0"`, `versionCode = 2`. |
| `docs/*` | Registro, cambios, avances, requisitos, alcance, mapa, mockups y README. |

### Decisiones y supuestos

| Decisión | Motivo |
| --- | --- |
| El alta no pide coordenadas y el guardado no las exige. | El docente puede crear el evento desde otro lugar; el GPS se coordina después. |
| Configurar el GPS desde la tarjeta y no desde el formulario. | Separa “crear la actividad” de “georreferenciar el recinto” y evita revalidar todo el evento. |
| No mostrar latitud/longitud como campos manuales. | Un usuario no conoce coordenadas; se leen del dispositivo o se interpretan desde un enlace. |
| Aceptar enlaces de Google Maps. | Es la forma natural en que un docente comparte la ubicación por mensajería. |
| Advertir por precisión y bloquear lecturas > 250 m. | Evita fijar un punto tan impreciso que rechace asistencias reales. |
| Sugerir radio ≥ 2 × precisión. | La tolerancia del validador cubre el error del escaneo, no el error del punto de referencia. |
| Guardar sin GPS sigue siendo válido. | No rompe la demo ni los eventos ya creados; la asistencia queda `SIN_VALIDACION`. |

### Verificación ejecutada

| Comando | Resultado |
| --- | --- |
| `./gradlew :app:testDebugUnitTest --offline` | ✅ 72 pruebas, 0 fallos |
| `./gradlew :app:jacocoDebugUnitTestReport --offline` | ✅ 82,7% instrucciones · 79,3% ramas · 94,1% líneas |
| `./gradlew :app:assembleDebug --offline` | ✅ APK generado (~37 MB) |
| `UbicacionEventoTest` | ✅ 22 casos |
| `UbicacionesChileTest` | ✅ 4 casos |

### Pendientes

- [ ] Verificar manualmente en dispositivo: permiso de ubicación, lectura GPS, enlace de Google Maps y guardado sin GPS.
- [ ] Sincronizar `latitud`, `longitud` y `radio_validacion_metros` con el esquema remoto cuando Supabase esté disponible (hoy es *best-effort*).
- [ ] Agregar la pantalla del diálogo a `docs/mockups/index.html` si se regenera la presentación.
- [ ] Evaluar un selector en mapa como alternativa al enlace (requiere API key de Google Maps u OSM).

### Dónde mirar si hay que cambiar algo

| Tema | Archivo principal | Qué tocar |
| --- | --- | --- |
| Lectura de enlaces/coordenadas | `data/model/UbicacionEvento.kt` | `CoordenadasParser`. |
| Regla de precisión y radio | `data/model/UbicacionEvento.kt` | `UbicacionEventoValidator`. |
| Diálogo y permisos | `ui/components/UbicacionEventoDialog.kt` | Textos, flujo de captura o pegado. |
| Tarjeta y botón | `ui/panels/DocentePanel.kt` | Estado GPS y apertura del diálogo. |
| Persistencia | `SyncPassRepository.kt` | `updateEventoUbicacion`. |
| Estado de la UI | `DocenteViewModel.kt` | `configurarUbicacion`. |
| Pruebas | `UbicacionEventoTest.kt`, `UbicacionesChileTest.kt` | Casos nuevos de parseo y precisión. |

---

## Sesión 2026-11-16 — versión `1.5.0`

### Objetivo

Permitir elegir un perfil demo por rol y asignar varios responsables (docentes y voluntarios) a proyectos y eventos, dejando un historial de altas y bajas para saber quién estuvo a cargo y cuándo cambió.

### Cambios realizados

#### 1. Acceso demo con selector de rol y perfil

- El acceso rápido ahora muestra una fila de roles: **Administrador, Docente, Voluntario y Beneficiario**.
- Al elegir un rol se listan sus perfiles ficticios con nombre, correo y descripción.
- Por defecto queda seleccionado **Voluntario** para facilitar la prueba de acreditación.
- `DemoAuth` contiene al menos dos perfiles por rol y `accountsFor(role)` devuelve la lista.
- `session(account)` genera la sesión del perfil elegido; `session(role)` se mantiene para compatibilidad.

#### 2. Responsables múltiples

- Proyectos: docentes **y voluntarios** a cargo.
- Eventos: docentes **y voluntarios** responsables.
- Se pueden agregar y quitar personas en cualquier edición.
- Un evento puede tener un equipo distinto al de su proyecto.
- En creación de evento se preseleccionan los docentes del proyecto.

#### 3. Historial de responsables

- Nueva tabla local `responsable_historial`.
- Registra `AGREGADO` o `ELIMINADO`, persona, rol, fecha y quién hizo el cambio.
- Un cambio de rol se registra como baja + alta.
- El botón **“Historial de responsables”** abre un diálogo en las tarjetas de proyecto y evento.
- El cálculo de cambios vive en `Responsables.kt` y tiene pruebas propias.

#### 4. Perfiles y administración

- La gestión de usuarios/roles sigue en **Administrador → Usuarios**: allí se agregan, editan y eliminan docentes, voluntarios y beneficiarios.
- Al eliminar un usuario se limpian sus asignaciones actuales para no dejar responsables huérfanos.
- El historial conserva el nombre registrado en el momento del cambio.

#### 5. Base de datos y seed

- `proyecto_docente` se reemplaza por la tabla genérica `responsable_cache`.
- Room sube a **versión 5** con migración destructiva.
- El seed asigna docentes y voluntarios al proyecto `101`, al evento `201`, al `203` y docentes al evento `202`.

### Archivos creados

| Archivo | Para qué |
| --- | --- |
| `data/model/Responsables.kt` | Constantes de rol/tipo/acción y cálculo de altas/bajas. |
| `data/local/Entities.kt` (`ResponsableEntity`) | Responsables actuales de proyectos y eventos. |
| `data/local/Entities.kt` (`ResponsableHistorialEntity`) | Historial de cambios. |
| `app/src/test/.../ResponsablesTest.kt` | 3 pruebas de altas, bajas y cambio de rol. |

### Archivos modificados

| Archivo | Cambio |
| --- | --- |
| `data/auth/DemoAuth.kt` | Catálogo de 12 perfiles y selección por cuenta. |
| `ui/login/LoginScreen.kt` | Selector de rol y lista de perfiles demo. |
| `MainActivity.kt` | Inicia sesión con el perfil demo elegido. |
| `data/model/Models.kt` | Voluntarios en proyecto, responsables en evento, ids en `EventoInput`. |
| `data/local/SyncPassDao.kt` | Consultas de responsables, historial y limpieza por usuario/proyecto. |
| `data/local/SyncPassDatabase.kt` | Versión 5 y nuevas entidades. |
| `data/local/LocalSeed.kt` | Responsables demo. |
| `data/repository/SyncPassRepository.kt` | Guardado, lectura e historial de responsables; limpieza al eliminar. |
| `ui/panels/DocenteViewModel.kt` | Voluntarios, historial y nuevas firmas de alta/edición. |
| `ui/panels/DocentePanel.kt` | Selectores de docentes/voluntarios, historial en tarjetas y diálogo. |
| `app/src/test/.../DemoAuthTest.kt` | Validación de varios perfiles por rol. |

### Decisiones y supuestos

| Decisión | Motivo |
| --- | --- |
| Usar una tabla genérica de responsables | Unifica docentes y voluntarios para proyectos y eventos. |
| Registrar solo altas/bajas efectivas | Evita historial lleno de cambios sin efecto. |
| Guardar nombre del responsable en el historial | El historial sigue siendo legible aunque el usuario se elimine. |
| Responsables e historial local-first | El esquema remoto todavía no tiene estas tablas; se documenta como deuda. |
| Etapa actual: pruebas locales | Primero se aseguran validaciones y CRUD en Room. Supabase está offline y se integrará después con su modelo definitivo. |
| Preseleccionar docentes del proyecto en el evento | Reduce pasos y asegura un responsable inicial. |

### Verificación ejecutada

| Comando | Resultado |
| --- | --- |
| `./gradlew :app:compileDebugKotlin` | ✅ Sin errores |
| `./gradlew :app:testDebugUnitTest` | ✅ 46 pruebas, 0 fallos |
| `ResponsablesTest` | ✅ 3 casos |
| `DemoAuthTest` | ✅ 4 casos |

### Pendientes

- [ ] Llevar `responsable_cache` y `responsable_historial` al esquema de Supabase.
- [ ] Refrescar la lista de voluntarios al volver de Administrador después de crear uno.
- [ ] Verificar manualmente el historial con varias altas y bajas en proyecto y evento.
- [ ] Definir si se requiere un “responsable principal” además de la lista.

### Dónde mirar si hay que cambiar algo

| Tema | Archivo principal | Qué tocar |
| --- | --- | --- |
| Perfiles demo | `data/auth/DemoAuth.kt` | Agregar/quitar cuentas y roles. |
| Selector de perfiles | `ui/login/LoginScreen.kt` | `DemoAccessCard`. |
| Responsables actuales | `ResponsableEntity` + `SyncPassRepository.kt` | Guardado y lectura. |
| Historial | `ResponsableHistorialEntity` + `guardarResponsables` | Acciones y fechas. |
| UI de asignación | `ui/panels/DocentePanel.kt` | Formularios de proyecto/evento. |
| Regla de cambios | `data/model/Responsables.kt` | Cálculo de agregados/eliminados. |
| Pruebas | `ResponsablesTest.kt`, `DemoAuthTest.kt` | Casos nuevos. |

---

## Sesión 2026-11-16 — versión `1.4.0`

### Objetivo

Desde la pestaña **Proyectos**, el botón **“Ver eventos”** debe abrir directamente la pestaña **Eventos** con ese proyecto seleccionado. Si el proyecto no tiene eventos, la app debe indicarlo claramente.

### Cambios realizados

#### 1. Navegación de Proyectos a Eventos

- `ProyectosTab` recibe `onOpenEventos`.
- Al presionar **“Ver eventos”**, el panel llama a `selectProyecto(id)` y cambia a la pestaña `Eventos` (`tab = 2`).
- Ya no es necesario seleccionar el proyecto y luego cambiar de pestaña manualmente.

#### 2. Conteo de eventos por proyecto

- `DocenteUiState` incorpora `eventosPorProyecto`.
- Al cargar el panel Docente se cuentan los eventos de los proyectos del docente.
- El conteo se actualiza al crear o eliminar eventos y al recargar el proyecto seleccionado.

#### 3. Aviso de proyecto sin eventos

- Cada tarjeta de proyecto muestra:
  - **“N eventos creados.”** si tiene actividades.
  - **“Aún no tiene eventos creados.”** si está vacío.
- Mientras se cargan los eventos se muestra “Cargando eventos del proyecto…” para no confundirlo con un proyecto vacío.
- Si se entra a Eventos sin actividades, el estado vacío indica el nombre del proyecto:
  **“El proyecto «…» aún no tiene eventos creados.”**

### Archivos modificados

| Archivo | Cambio |
| --- | --- |
| `ui/panels/DocentePanel.kt` | Botón “Ver eventos” cambia de pestaña; conteo y aviso en la tarjeta; estado vacío con nombre del proyecto. |
| `ui/panels/DocenteViewModel.kt` | `eventosPorProyecto` y actualización al cargar/crear/eliminar. |

### Decisiones y supuestos

| Decisión | Motivo |
| --- | --- |
| Cambiar de pestaña incluso si el proyecto no tiene eventos. | El estado vacío explica la situación y deja al docente en el lugar correcto para crear el primero. |
| Calcular el conteo en memoria desde los eventos cargados. | Evita un cambio de esquema o un endpoint nuevo. |
| Mantener el filtro por proyecto existente. | No duplica lógica de selección. |

### Verificación ejecutada

| Comando | Resultado |
| --- | --- |
| `./gradlew :app:compileDebugKotlin` | ✅ Sin errores |
| `./gradlew :app:testDebugUnitTest` | ✅ 42 pruebas, 0 fallos |

### Pendientes

- [ ] Verificar manualmente con un proyecto sin eventos y con un proyecto con varios eventos.
- [ ] Revisar si el conteo debe provenir de una consulta agregada en modo remoto con muchos datos.

### Dónde mirar si hay que cambiar algo

| Tema | Archivo principal | Qué tocar |
| --- | --- | --- |
| Cambio de pestaña desde Proyectos | `ui/panels/DocentePanel.kt` | `ProyectosTab` y `onOpenEventos`. |
| Conteo por proyecto | `ui/panels/DocenteViewModel.kt` | `eventosPorProyecto` y `loadEventosInternal`. |
| Mensaje sin eventos | `ui/panels/DocentePanel.kt` | Tarjeta de proyecto y estado vacío de `EventosTab`. |

---

## Sesión 2026-11-16 — versión `1.3.0`

### Objetivo

Impedir que un evento se programe fuera del período de vigencia del proyecto. El evento debe comenzar en la fecha de inicio del proyecto o después, y terminar en la fecha de término o antes.

### Cambios realizados

#### 1. Validación de período

- `FechaValidator.rangoDentroDePeriodo(...)` compara el rango del evento con el del proyecto.
- `FechaValidator.mensajeErrorPeriodo(...)` entrega un mensaje claro con la fecha límite.
- Como el proyecto guarda solo fecha, su **último día completo** es válido: un evento puede terminar a las 20:00 del último día.

#### 2. Formulario de alta y edición

- El formulario muestra **“Período del proyecto: …”** bajo las fechas del evento.
- Si el evento se sale del rango, aparece el mensaje y el botón de guardar queda deshabilitado.
- El diálogo de edición ahora recibe el proyecto completo para conocer sus fechas.

#### 3. Validación en el repositorio

- `createEvento` y `updateEvento` rechazan la operación si el evento queda fuera del período.
- La comprobación usa el proyecto cacheado en Room, por lo que protege también llamadas internas.

### Archivos modificados

| Archivo | Cambio |
| --- | --- |
| `data/model/FechaValidator.kt` | Validación y mensaje de período del proyecto. |
| `data/repository/SyncPassRepository.kt` | Rechazo de eventos fuera de rango. |
| `ui/panels/DocentePanel.kt` | Período visible, error y bloqueo del guardado en alta/edición. |
| `app/src/test/.../FechaValidatorTest.kt` | 5 casos nuevos de período de proyecto. |

### Decisiones y supuestos

| Decisión | Motivo |
| --- | --- |
| El término del proyecto cubre hasta el final del día. | El proyecto usa `yyyy-MM-dd`, sin hora. |
| Validar en UI y repositorio. | La UI evita el error y el repositorio protege llamadas internas. |
| No migrar datos existentes. | La regla se aplica al crear o editar; no es necesario cambiar el esquema. |

### Verificación ejecutada

| Comando | Resultado |
| --- | --- |
| `./gradlew :app:compileDebugKotlin` | ✅ Sin errores |
| `./gradlew :app:testDebugUnitTest` | ✅ 42 pruebas, 0 fallos |
| `FechaValidatorTest` | ✅ 12 casos, 0 fallos |

### Pendientes

- [ ] Verificar manualmente el mensaje al intentar guardar un evento antes o después del proyecto.
- [ ] Definir si un proyecto con fechas modificadas debe revalidar sus eventos existentes.

### Dónde mirar si hay que cambiar algo

| Tema | Archivo principal | Qué tocar |
| --- | --- | --- |
| Regla de período | `data/model/FechaValidator.kt` | `rangoDentroDePeriodo` y `mensajeErrorPeriodo`. |
| Validación de respaldo | `data/repository/SyncPassRepository.kt` | `createEvento` y `updateEvento`. |
| Formulario y mensajes | `ui/panels/DocentePanel.kt` | `EventoForm` y `EventoEditDialog`. |
| Pruebas | `FechaValidatorTest.kt` | Casos antes/después/primer día/último día. |

---

## Sesión 2026-11-16 — versión `1.2.0`

### Objetivo

Evitar errores al configurar la ubicación de un evento: usar un radio cerrado de 20/40/60/80/100 m, agregar región y comuna, y quitar latitud/longitud y la captura GPS del formulario, porque el docente puede crear el evento desde otro lugar. Las coordenadas quedan opcionales para configurarlas más adelante.

### Cambios realizados

#### 1. Radio de validación cerrado

- El campo de texto libre se reemplazó por `RadioValidacionDropdown`.
- Solo se puede elegir **20, 40, 60, 80 o 100 m**.
- `LocationValidator.RADIOS_VALIDOS` y `normalizarRadio(...)` centralizan la regla.
- El repositorio normaliza el radio antes de guardar, incluso si llega un valor antiguo.

#### 2. Región y comuna

- `UbicacionesChile` contiene las 16 regiones y sus comunas.
- `RegionComunaFields` muestra dos listas dependientes: al cambiar la región se recargan las comunas.
- Al cambiar de región, una comuna que no pertenece a la nueva región se limpia.
- Región y comuna se guardan en el modelo, Room y payload remoto.
- Las tarjetas de evento del docente y del voluntario muestran “Ubicación: comuna, región”.

#### 3. Sin latitud/longitud ni GPS en el formulario

- Se eliminaron los campos manuales **Latitud** y **Longitud** del alta y de la edición.
- Se eliminó el botón **“Usar mi ubicación actual”** y el composable `LocationCaptureButton`.
- Al crear un evento queda sin coordenadas y la asistencia se registra como `SIN_VALIDACION`.
- Al editar un evento existente se conservan sus coordenadas originales.

#### 4. Base de datos y seed

- Room sube a **versión 4** con `fallbackToDestructiveMigration`.
- El seed de los tres eventos ahora incluye **Metropolitana de Santiago / Maipú**.
- El evento `202` pasó de radio 120 m a **100 m**.

### Archivos creados

| Archivo | Para qué |
| --- | --- |
| `data/model/UbicacionesChile.kt` | Catálogo de regiones y comunas de Chile. |
| `app/src/test/...` (casos nuevos) | Pruebas de normalización de radio dentro de `LocationValidatorTest`. |

### Archivos modificados

| Archivo | Cambio |
| --- | --- |
| `data/model/Models.kt` | `region` y `comuna` en `Evento` y `EventoInput`. |
| `data/local/Entities.kt` | `region` y `comuna` en `EventoEntity`. |
| `data/local/Mappers.kt` | Mapeo de los campos nuevos. |
| `data/local/SyncPassDatabase.kt` | Versión 4. |
| `data/local/LocalSeed.kt` | Región/comuna demo y radio 100 m. |
| `data/location/LocationValidator.kt` | Catálogo de radios y `normalizarRadio`. |
| `data/repository/SyncPassRepository.kt` | `region`/`comuna` en alta y edición; radio normalizado. |
| `ui/components/Selectors.kt` | `OpcionDropdown`, `RegionComunaFields`, `RadioValidacionDropdown`. |
| `ui/panels/DocentePanel.kt` | Formulario sin GPS, con región/comuna y radio cerrado. |
| `ui/panels/DocenteViewModel.kt` | Envía región/comuna al repositorio. |
| `ui/panels/VoluntarioPanel.kt` | Muestra región/comuna en la tarjeta del evento. |
| `app/src/test/.../LocationValidatorTest.kt` | 2 casos nuevos de radio. |

### Decisiones y supuestos

| Decisión | Motivo |
| --- | --- |
| Quitar GPS del alta y de la edición por ahora. | El docente puede crear el evento desde otro lugar; las coordenadas se configurarán en un flujo posterior. |
| Conservar coordenadas al editar. | Evita que un evento ya georreferenciado pierda la validación GPS. |
| Listas cerradas para región y comuna. | Evita errores de escritura y comunas inexistentes. |
| Normalizar el radio en el dominio y en el repositorio. | Protege datos antiguos aunque la UI ya no permita escribirlos. |
| Enviar `region`/`comuna` *best-effort* a Supabase. | El esquema remoto puede no tenerlas todavía. |

### Verificación ejecutada

| Comando | Resultado |
| --- | --- |
| `./gradlew :app:compileDebugKotlin` | ✅ Sin errores |
| `./gradlew :app:testDebugUnitTest` | ✅ 37 pruebas, 0 fallos |
| `LocationValidatorTest` | ✅ 12 casos, 0 fallos |

### Pendientes

- [ ] Configurar coordenadas GPS desde un flujo separado (por ejemplo, acción “Configurar ubicación GPS” en la tarjeta del evento).
- [ ] Llevar `region` y `comuna` al esquema remoto de Supabase.
- [ ] Verificar manualmente en dispositivo/emulador los selectores de región/comuna y el radio cerrado.
- [ ] Actualizar capturas o presentación visual si se regeneran los mockups.

### Dónde mirar si hay que cambiar algo

| Tema | Archivo principal | Qué tocar |
| --- | --- | --- |
| Regiones y comunas | `data/model/UbicacionesChile.kt` | Catálogo y agrupación por región. |
| Selectores de región/comuna/radio | `ui/components/Selectors.kt` | `RegionComunaFields`, `RadioValidacionDropdown`. |
| Radio permitido | `data/location/LocationValidator.kt` | `RADIOS_VALIDOS` y `normalizarRadio`. |
| Formulario de evento | `ui/panels/DocentePanel.kt` | Alta y edición del evento. |
| Persistencia | `Models.kt`, `Entities.kt`, `Mappers.kt` | Campos `region` y `comuna`. |
| Datos demo | `data/local/LocalSeed.kt` | Región/comuna y radio de ejemplo. |

---

## Sesión 2026-11-16 — versión `1.1.0`

### Objetivo

Mejorar la captura de fechas y horas, hacer explícita la relación proyecto–evento, asignar docentes a cargo, validar los rangos de fechas y registrar quién creó/certificó cada dato. Además, mostrar la hora actual en la app.

### Cambios realizados

#### 1. Fecha y hora dinámicas

- Se agregaron `DateField` y `DateTimeField` con calendario y reloj de Material 3.
- Los formularios de proyecto y evento ya no aceptan fechas escritas a mano.
- Si el rango es inválido, se muestra el mensaje y se deshabilita el botón de guardar.
- `LiveClock` muestra fecha larga y hora actualizada cada segundo en la cabecera del dashboard.

#### 2. Relación proyecto–evento

- La pestaña **Eventos** tiene un selector de proyecto.
- El formulario de evento también permite elegir el proyecto (`Evento para: …`).
- Cada tarjeta de evento muestra el proyecto al que pertenece.
- Se eliminó la dependencia de “primero selecciona un proyecto en otra pestaña”.

#### 3. Docentes a cargo

- Se agregó la tabla puente `proyecto_docente` (N:M).
- `DocentesMultiSelect` permite marcar uno o varios docentes al crear/editar un proyecto.
- El docente creador queda preseleccionado.
- Las actividades muestran los docentes heredados del proyecto.

#### 4. Validación de fechas

- `FechaValidator` aplica la regla **término ≥ inicio** para proyectos (`yyyy-MM-dd`) y eventos (`yyyy-MM-ddTHH:mm`).
- El mismo día/hora es válido; un término anterior se bloquea con mensaje.
- La validación también se aplica en el repositorio, no solo en la UI.

#### 5. Auditoría

- Proyectos y eventos guardan `creadoPor` y `creadoEn`.
- La asistencia guarda `registradoPor` y `asistenciaFecha`.
- Se muestra en tarjetas, diálogo de edición y lista de inscritos del voluntario.

#### 6. Base de datos y semilla

- Room sube a **versión 3** con `fallbackToDestructiveMigration`.
- El seed incluye docentes, relaciones `proyecto_docente` y datos de auditoría.
- Al instalar sobre una versión anterior se reconstruye la caché local y se reejecuta el seed.

### Archivos creados

| Archivo | Para qué |
| --- | --- |
| `data/model/FechaValidator.kt` | Regla término ≥ inicio y mensajes de error. |
| `ui/components/DateTimeUtils.kt` | Conversión ISO ↔ millis, formatos de fecha/hora y reloj. |
| `ui/components/DateTimeFields.kt` | `DateField`, `DateTimeField`, `LiveClock`. |
| `ui/components/Selectors.kt` | `ProyectoDropdown`, `DocentesMultiSelect`. |
| `app/src/test/.../FechaValidatorTest.kt` | 7 casos de rangos de fecha/hora. |
| `app/src/test/.../DateTimeUtilsTest.kt` | 7 casos de formatos y reloj. |
| `docs/REGISTRO.md` | Este registro de trabajo. |

### Archivos modificados

| Archivo | Cambio |
| --- | --- |
| `data/model/Models.kt` | `DocenteRef`, docentes en `Proyecto`, auditoría en `Proyecto`/`Evento`/`AsistenciaRef`/`Inscrito`. |
| `data/local/Entities.kt` | `ProyectoDocenteEntity`, `creadoPor`/`creadoEn`, auditoría de asistencia. |
| `data/local/Mappers.kt` | Mapeo de los campos nuevos. |
| `data/local/SyncPassDao.kt` | Consultas de docentes, `markAsistencia` con auditoría. |
| `data/local/SyncPassDatabase.kt` | Versión 3 y nueva entidad. |
| `data/local/LocalSeed.kt` | Docentes, auditoría y relaciones demo. |
| `data/repository/SyncPassRepository.kt` | Docentes por proyecto, validación de fechas, auditoría y `registradoPor`. |
| `ui/panels/DocentePanel.kt` | Pickers, selector de proyecto, multi-select de docentes y textos de auditoría. |
| `ui/panels/DocenteViewModel.kt` | Lista de docentes, creador y nuevas firmas. |
| `ui/panels/VoluntarioPanel.kt` | Envía el nombre del voluntario y muestra quién registró la asistencia. |
| `ui/panels/VoluntarioViewModel.kt` | `registradoPor` en el registro de asistencia. |
| `ui/panels/BeneficiarioPanel.kt` | Muestra el creador del evento. |
| `ui/dashboard/DashboardScreen.kt` | Reloj en la cabecera. |
| `app/build.gradle.kts` | Perímetro JaCoCo para `DateTimeUtils`. |
| `app/src/test/.../MappersTest.kt` | Casos con auditoría. |
| `docs/CAMBIOS.md` | Versión `1.1.0`. |
| `docs/AVANCES.md` | Iteración 7 y métricas. |
| `docs/MAP.md` | Archivos nuevos y Room v3. |
| `docs/ARQUITECTURA.md` | Modelo de datos y auditoría. |
| `docs/REQUISITOS.md` | RF-31 a RF-36, RN-11 a RN-15 y trazabilidad. |
| `docs/MOCKUPS.md` | Wireframes con pickers y selectores. |
| `docs/ALCANCE.md` | Indicadores de pruebas y cobertura. |
| `docs/METODOLOGIA.md` | Cobertura actual. |
| `docs/README.md` | Enlace a este registro. |

### Decisiones y supuestos

| Decisión | Motivo |
| --- | --- |
| Formatos guardados sin cambios (`yyyy-MM-dd` y `yyyy-MM-ddTHH:mm`). | No romper la API remota ni los datos existentes. |
| Relación N:M local en `proyecto_docente`. | Room no tiene N:M nativo; la tabla puente es la forma correcta. |
| El creador se preselecciona como docente. | Garantiza al menos un responsable y reduce clics. |
| Columnas nuevas remotas se envían *best-effort*. | El esquema de Supabase puede no tenerlas todavía. |
| Migración destructiva de Room. | Es una demo; se prioriza simplicidad y reseed automático. |

### Verificación ejecutada

| Comando | Resultado |
| --- | --- |
| `./gradlew :app:compileDebugKotlin --offline` | ✅ Sin errores |
| `./gradlew :app:testDebugUnitTest --offline` | ✅ 35 pruebas, 0 fallos |
| `./gradlew :app:jacocoDebugUnitTestReport --offline` | ✅ 71,9% instrucciones · 82,5% ramas · 90,3% líneas |
| `./gradlew :app:assembleDebug --offline` | ✅ APK ~37 MB |

### Pendientes

- [ ] Verificación manual en dispositivo/emulador: pickers, docentes, auditoría y reloj.
- [ ] Llevar `proyecto_docente` y las columnas de auditoría al esquema remoto de Supabase.
- [ ] Definir si un evento puede cambiar de proyecto al editarlo (hoy el proyecto se muestra fijo).
- [ ] Exportación de informes PDF/CSV y notificaciones (backlog previo).

### Dónde mirar si hay que cambiar algo

| Tema | Archivo principal | Qué tocar |
| --- | --- | --- |
| Selectores de fecha/hora | `ui/components/DateTimeFields.kt` | Calendario, reloj o formato de visualización. |
| Conversión y formatos | `ui/components/DateTimeUtils.kt` | ISO ↔ millis, fecha larga, hora. |
| Regla término ≥ inicio | `data/model/FechaValidator.kt` | Validación y mensajes. |
| Relación proyecto–evento | `ui/components/Selectors.kt` + `DocentePanel.kt` | Selector de proyecto. |
| Docentes a cargo | `proyecto_docente` + `DocentesMultiSelect` | Alta/edición y consulta. |
| Auditoría | `Models.kt`, `Entities.kt`, `Mappers.kt`, repositorio | Campos y persistencia. |
| Reloj del dashboard | `DashboardScreen.kt` + `LiveClock` | Ubicación o formato. |
| Pruebas | `FechaValidatorTest`, `DateTimeUtilsTest`, `MappersTest` | Casos nuevos. |

---

## Sesión 2026-11-15 — versión `1.0.0-demo` (referencia)

- Modo local/demo con seed ficticio.
- Validación GPS con radio y tolerancia.
- Sincronización manual y automática.
- Login demo por rol.
- Pruebas unitarias y documentación inicial.

Detalle completo en `CAMBIOS.md`, sección `7. 1.0.0-demo`.
