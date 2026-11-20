# Requisitos del sistema

## 1. Actores y roles

| Actor | Descripción | Permisos principales |
| --- | --- | --- |
| Administrador | Gestiona la operación global. | Usuarios, roles, consulta de información. |
| Docente / Coordinador | Responsable de proyectos y eventos. | CRUD de proyectos y eventos, QR, informes. |
| Voluntario | Apoya la acreditación en terreno. | Lista de inscritos, escáner QR, validación GPS. |
| Beneficiario / Estudiante | Participante de las actividades. | Inscripción, pase QR, encuesta, historial. |
| Sistema | Automatizaciones internas. | Seed local, cola de sincronización, cálculo de métricas. |

## 2. Requerimientos funcionales

Prioridad: **M** = Must, **S** = Should, **C** = Could.

| ID | Requerimiento | Prioridad | Estado |
| --- | --- | --- | --- |
| RF-01 | Iniciar sesión con credenciales institucionales mediante AWS Cognito. | M | Implementado |
| RF-02 | Restaurar la sesión del usuario sin volver a pedir credenciales. | M | Implementado |
| RF-03 | Permitir acceso local de demostración por rol cuando no hay backend disponible. | M | Implementado |
| RF-04 | Mostrar un panel distinto según el rol autenticado. | M | Implementado |
| RF-05 | Gestionar usuarios y roles desde el panel administrador. | M | Implementado |
| RF-06 | Crear, editar, listar y eliminar proyectos. | M | Implementado |
| RF-07 | Crear, editar, listar y eliminar eventos asociados a un proyecto. | M | Implementado |
| RF-08 | Configurar recinto, dirección, región, comuna y radio de validación de un evento. | M | Implementado |
| RF-09 | Capturar la ubicación actual del docente para fijar las coordenadas del evento. | S | Implementado (flujo aparte “Configurar ubicación GPS”) |
| RF-10 | Generar un pase QR único por inscripción. | M | Implementado |
| RF-11 | Escanear el QR del beneficiario y verificar que corresponda al evento seleccionado. | M | Implementado |
| RF-12 | Validar que el asistente esté dentro del radio GPS del evento. | M | Implementado |
| RF-13 | Bloquear la acreditación cuando el asistente está fuera del radio o sin ubicación utilizable. | M | Implementado |
| RF-14 | Registrar la asistencia con estado `VALIDADA` o `SIN_VALIDACION`. | M | Implementado |
| RF-15 | Permitir inscribirse y cancelar la inscripción a un evento. | M | Implementado |
| RF-16 | Responder una encuesta de satisfacción y utilidad después del evento. | M | Implementado |
| RF-17 | Calcular métricas de inscritos, asistentes, encuestas y satisfacción. | M | Implementado |
| RF-18 | Mostrar la lista de inscritos y su estado de asistencia. | M | Implementado |
| RF-19 | Operar sin conexión y encolar las operaciones pendientes. | M | Implementado |
| RF-20 | Sincronizar manualmente los datos pendientes desde el dashboard. | M | Implementado |
| RF-21 | Sincronizar automáticamente en segundo plano con WorkManager. | S | Implementado |
| RF-22 | Sembrar datos ficticios para operar la demo sin backend. | M | Implementado |
| RF-23 | Mostrar claramente cuándo la app está en modo local/demo. | S | Implementado |
| RF-24 | Mostrar la distancia, precisión y estado de la validación GPS antes de escanear. | S | Implementado |
| RF-25 | Consultar el historial de actividades del beneficiario. | C | Implementado |
| RF-26 | Generar un informe textual consolidado por proyecto. | C | Parcial (resumen en pantalla) |
| RF-27 | Exportar informes a PDF/CSV. | C | Pendiente |
| RF-28 | Notificar recordatorios de eventos. | C | Pendiente |
| RF-29 | Integrar una API Spring Boot desplegada en EC2. | M según rúbrica | Pendiente (fuera de este repositorio) |
| RF-30 | Gestionar roles y permisos desde Cognito Groups. | M | Implementado |
| RF-31 | Elegir fecha y hora mediante selectores de calendario y reloj, sin texto libre. | M | Implementado |
| RF-32 | Validar que la fecha de término no sea anterior a la de inicio en proyectos y eventos. | M | Implementado |
| RF-33 | Asociar cada evento a un proyecto desde el formulario y filtrar la lista por proyecto. | M | Implementado |
| RF-34 | Asignar uno o varios docentes y voluntarios a cargo de un proyecto; las actividades pueden heredar o definir su propio equipo. | M | Implementado |
| RF-35 | Mostrar quién creó proyectos y eventos y quién registró cada asistencia, con fecha y hora. | S | Implementado |
| RF-36 | Mostrar un reloj en vivo con fecha y hora en la cabecera del dashboard. | S | Implementado |
| RF-37 | Indicar región y comuna del evento mediante listas cerradas y dependientes. | M | Implementado |
| RF-38 | Restringir el radio de validación a 20, 40, 60, 80 o 100 metros. | M | Implementado |
| RF-39 | Impedir que un evento se programe fuera del período de fechas del proyecto. | M | Implementado |
| RF-40 | Abrir la pestaña Eventos del proyecto desde su tarjeta e indicar si aún no tiene eventos. | M | Implementado |
| RF-41 | Asignar docentes y voluntarios como responsables de proyectos y eventos, permitiendo agregarlos o quitarlos y consultar el historial. | M | Implementado |
| RF-42 | Elegir el rol y el perfil ficticio en el acceso rápido de demostración. | S | Implementado |
| RF-43 | Configurar, cambiar o quitar las coordenadas GPS de un evento desde su tarjeta, con la ubicación actual o un enlace de Google Maps, sin exigirlas al crear el evento. | S | Implementado |

## 3. Requerimientos no funcionales

| ID | Categoría | Requerimiento | Criterio verificable |
| --- | --- | --- | --- |
| RNF-01 | Plataforma | App 100% Kotlin sobre Android nativo. | Código fuente en Kotlin; sin WebView para lógica principal. |
| RNF-02 | Arquitectura | MVVM estricto con separación View / ViewModel / Repository / DataSource. | Las pantallas no acceden a Room ni a HTTP directamente. |
| RNF-03 | Offline-first | Operar sin internet para los flujos críticos. | Inscripción, QR, asistencia y encuesta funcionan en modo avión. |
| RNF-04 | Rendimiento | Respuesta percibida fluida. | Escaneo < 2 s; lectura GPS < 10 s; listas sin bloqueo del hilo principal. |
| RNF-05 | Seguridad | Autenticación delegada a Cognito y transporte HTTPS. | Tokens en SharedPreferences del SDK; sin contraseñas en código. |
| RNF-06 | Usabilidad | Interfaz en español, Material 3, mensajes accionables. | Flujos guiados y errores con causa y siguiente paso. |
| RNF-07 | Compatibilidad | Android 7.0 (API 24) a Android 15+ (target 37). | `minSdk 24`, `targetSdk 37`. |
| RNF-08 | Mantenibilidad | Código modular, nombres claros y documentación viva. | Estructura por capas y docs en `/docs`. |
| RNF-09 | Testabilidad | Lógica de dominio separada de Android. | Validador GPS puro probado con JUnit. |
| RNF-10 | Resiliencia | Fallback local ante fallas de red o backend. | Repositorio devuelve caché/estado local sin crash. |
| RNF-11 | Privacidad | Minimizar datos personales y usar datos ficticios en demo. | Seed sin PII real; solo correo/nombre para operar. |
| RNF-12 | Observabilidad | Estado de sincronización visible. | Contador de pendientes y mensaje de resultado. |
| RNF-13 | Cobertura | Cobertura de pruebas ≥ 30%. | JaCoCo: 82,7% instrucciones / 94,1% líneas en lógica de dominio. |

## 4. Reglas de negocio

| ID | Regla |
| --- | --- |
| RN-01 | Un usuario no puede tener más de una inscripción activa por evento. |
| RN-02 | La cancelación cambia el estado a `CANCELADO`; no borra el historial. |
| RN-03 | Solo un voluntario autenticado puede acreditar asistencia. |
| RN-04 | Las coordenadas del evento son opcionales; sin ellas la asistencia queda `SIN_VALIDACION`. |
| RN-05 | El radio de validación solo admite 20, 40, 60, 80 o 100 m. Un valor fuera del catálogo se ajusta al permitido más cercano; si es ≤ 0, se usa 100 m. |
| RN-06 | Una asistencia solo puede ser `VALIDADA` si la distancia efectiva es menor o igual al radio. |
| RN-07 | Los registros creados offline usan IDs negativos para no colisionar con IDs remotos positivos. |
| RN-08 | La sincronización debe ser idempotente: reintentar no duplica asistencias ni encuestas. |
| RN-09 | El QR debe pertenecer al evento seleccionado; de lo contrario se rechaza. |
| RN-10 | La precisión GPS superior a 250 m se considera insuficiente y no valida la asistencia. |
| RN-11 | La fecha de término puede ser igual a la de inicio, pero nunca anterior. |
| RN-12 | Cada evento pertenece a un proyecto; el formulario permite elegirlo explícitamente. |
| RN-13 | Todo proyecto debe tener al menos un docente a cargo. |
| RN-14 | Todo proyecto y evento registra su creador y su fecha/hora de creación. |
| RN-15 | Toda asistencia registra al usuario que la acreditó y la fecha/hora de marcación. |
| RN-16 | Un evento debe comenzar en la fecha de inicio del proyecto o después, y terminar en la fecha de término del proyecto o antes. El último día del proyecto se considera completo. |
| RN-17 | Todo proyecto y evento debe tener al menos un responsable docente o voluntario. |
| RN-18 | Cada alta o baja de responsable se registra en el historial con persona, rol, fecha y actor. |
| RN-19 | Las coordenadas del evento se configuran después del alta y nunca son obligatorias. Se aceptan desde la ubicación actual o desde un enlace/par de coordenadas; una precisión mayor a 250 m se rechaza como referencia y una precisión mayor a la mitad del radio genera advertencia. |

## 5. Casos de uso

### CU-01 — Iniciar sesión

- **Actor:** Usuario registrado.
- **Precondición:** Usuario habilitado en Cognito.
- **Flujo principal:** ingresa correo y contraseña → Cognito valida → se decodifican grupos del JWT → se abre el panel del rol.
- **Alternativas:** contraseña temporal → cambio obligatorio; backend caído → acceso demo local por rol.
- **Postcondición:** sesión activa y rol identificado.

### CU-02 — Crear evento con región, comuna y radio

- **Actor:** Docente.
- **Precondición:** Sesión de docente y al menos un proyecto.
- **Flujo principal:** elige el proyecto en el selector del formulario → completa nombre, fechas con los selectores de calendario y reloj, cupos y recinto → selecciona región y comuna en listas dependientes → elige radio entre 20, 40, 60, 80 o 100 m → guarda.
- **Alternativas:** el evento se crea sin coordenadas y la asistencia queda `SIN_VALIDACION`; las coordenadas GPS se configuran después desde la tarjeta del evento (ubicación actual o enlace de Google Maps); si el término queda antes del inicio o el evento se sale del período del proyecto, el botón se deshabilita con un mensaje.
- **Postcondición:** evento vinculado al proyecto, dentro de su período, con región, comuna, radio permitido, creador y fecha/hora de creación.

### CU-03 — Inscribirse a un evento

- **Actor:** Beneficiario.
- **Precondición:** Sesión de beneficiario.
- **Flujo principal:** selecciona evento → “Inscribirme” → se crea/reutiliza la inscripción → se muestra el pase QR.
- **Alternativas:** inscripción cancelada previamente → se reactiva.
- **Postcondición:** inscripción `INSCRITO` y QR disponible.

### CU-04 — Acreditar asistencia con GPS

- **Actor:** Voluntario.
- **Precondición:** Evento con inscritos; permiso de ubicación y cámara.
- **Flujo principal:** abre el escáner del evento → la app obtiene GPS y muestra distancia/precisión → escanea el QR → valida evento, inscripción y radio → registra asistencia.
- **Alternativas:** fuera de radio → bloqueo; sin GPS → bloqueo si el evento tiene coordenadas; evento sin coordenadas → registra `SIN_VALIDACION`; QR de otro evento → rechazo.
- **Postcondición:** asistencia registrada y métricas actualizadas.

### CU-05 — Sincronizar operaciones pendientes

- **Actor:** Usuario autenticado / sistema.
- **Precondición:** operaciones en `pending_action` y conectividad.
- **Flujo principal:** presiona “Sincronizar ahora” o WorkManager se ejecuta → se envían asistencias/encuestas → se eliminan las pendientes exitosas.
- **Alternativas:** error 4xx permanente → se descarta; error de red → se incrementa el intento y se reintenta.
- **Postcondición:** cola reducida y contador actualizado.

### CU-06 — Responder encuesta y ver métricas

- **Actor:** Beneficiario / Docente.
- **Flujo principal:** beneficiario califica y comenta → se guarda local/remoto → docente abre Informes → ve asistencias, encuestas, promedio y comentarios.
- **Postcondición:** métricas consistentes con los registros.

### CU-07 — Crear proyecto con docentes a cargo

- **Actor:** Docente / Administrador.
- **Precondición:** Sesión activa y docentes registrados.
- **Flujo principal:** completa título, descripción y fechas con los selectores → marca uno o varios docentes → guarda.
- **Alternativas:** sin docente seleccionado → botón deshabilitado; término anterior al inicio → mensaje de validación.
- **Postcondición:** proyecto creado con creador, fecha/hora y docentes asignados; sus eventos heredan la asignación.

### CU-08 — Asignar responsables y revisar historial

- **Actor:** Docente.
- **Precondición:** Proyecto o evento existente y perfiles de docentes/voluntarios disponibles.
- **Flujo principal:** abre el formulario de proyecto o evento → marca docentes y voluntarios responsables → guarda → la tarjeta muestra el equipo actual → presiona **“Historial de responsables”** para ver altas y bajas.
- **Alternativas:** no hay responsables seleccionados → el botón de guardar se deshabilita; se quita un responsable → queda registrado como `ELIMINADO`.
- **Postcondición:** responsables actualizados y cambio registrado con fecha y actor.
- **Nota:** los perfiles se crean, editan o eliminan desde **Administrador → Usuarios**.

### CU-09 — Ir a los eventos de un proyecto

- **Actor:** Docente.
- **Precondición:** Sesión de docente y al menos un proyecto.
- **Flujo principal:** abre **Proyectos** → presiona **“Ver eventos”** en una tarjeta → se selecciona el proyecto → se abre la pestaña **Eventos** → se listan sus actividades.
- **Alternativas:** si el proyecto no tiene eventos, la tarjeta muestra “Aún no tiene eventos creados.” y la pestaña Eventos indica el nombre del proyecto sin actividades.
- **Postcondición:** pestaña Eventos filtrada por el proyecto elegido.

### CU-10 — Configurar la ubicación GPS de un evento

- **Actor:** Docente.
- **Precondición:** Evento existente; el alta no exigió coordenadas.
- **Flujo principal:** abre **Eventos** → presiona **“Configurar ubicación GPS”** en la tarjeta → elige **“Usar mi ubicación actual”** (otorga permiso si hace falta) o pega un enlace de Google Maps / par `latitud, longitud` → revisa el punto, la precisión y el radio → guarda.
- **Alternativas:** sin permiso de ubicación → mensaje y se puede pegar un enlace; precisión > 250 m → no se acepta como referencia; precisión > radio/2 → advertencia con radio sugerido; **“Quitar ubicación GPS”** → el evento vuelve a `SIN_VALIDACION`; **“Guardar sin GPS”** → el evento queda operable sin validación.
- **Postcondición:** el evento guarda `latitud`, `longitud` y `radio_validacion_metros`; la tarjeta muestra el estado GPS y las asistencias pueden validarse por distancia.

## 6. Matriz de trazabilidad
| Requerimiento | Implementación principal | Prueba / evidencia |
| --- | --- | --- |
| RF-01, RF-02 | `CognitoAuthManager`, `JwtUtils`, `MainActivity` | Manual con cuentas Cognito. |
| RF-03, RF-22, RF-23 | `DemoAuth`, `LocalSeed`, `LoginScreen`, `DashboardScreen` | Login demo y seed verificados en build. |
| RF-04, RF-05 | `DashboardScreen`, `AdminPanel`, `AdminViewModel` | Manual. |
| RF-06, RF-07, RF-08, RF-33, RF-34 | `DocentePanel`, `DocenteViewModel`, `SyncPassRepository`, `responsable_cache` | Manual; formulario con región/comuna, responsables y radio cerrado. |
| RF-09, RF-43 | `UbicacionEventoDialog`, `CoordenadasParser`, `SyncPassRepository.updateEventoUbicacion`, `DocentePanel` | `UbicacionEventoTest` (22 casos) + manual. |
| RF-37, RF-38 | `UbicacionesChile`, `RegionComunaFields`, `RadioValidacionDropdown`, `LocationValidator` | `LocationValidatorTest` (12 casos). |
| RF-39 | `FechaValidator`, `SyncPassRepository`, `DocentePanel` | `FechaValidatorTest` (12 casos, 5 de período de proyecto). |
| RF-40 | `ProyectosTab`, `EventosTab`, `DocenteViewModel` | Manual con proyecto con eventos y proyecto vacío. |
| RF-41 | `Responsables.kt`, `ResponsableEntity`, `ResponsableHistorialEntity`, repositorio y `DocentePanel` | `ResponsablesTest` (3 casos) + manual. |
| RF-42 | `DemoAuth`, `LoginScreen`, `MainActivity` | `DemoAuthTest` (4 casos). |
| RF-43 | `UbicacionEventoDialog`, `CoordenadasEvento`, `UbicacionEventoValidator`, `SyncPassRepository.updateEventoUbicacion` | `UbicacionEventoTest` (22 casos) + manual. |
| RF-10, RF-11 | `QrCode`, `QrScannerScreen`, `VoluntarioViewModel` | Manual con dos dispositivos o captura de QR. |
| RF-12, RF-13, RF-14 | `LocationValidator`, `LocationProvider`, `VoluntarioViewModel` | `LocationValidatorTest` (12 casos). |
| RF-15 a RF-18 | `BeneficiarioPanel`, `BeneficiarioViewModel`, repositorio | Manual + `MappersTest`. |
| RF-19 a RF-21 | `pending_action`, `SyncScheduler`, `SyncWorker`, `DashboardViewModel` | Manual offline/online. |
| RF-24 | `GpsStatusCard` en `VoluntarioPanel` | Manual. |
| RF-31, RF-32 | `DateField`, `DateTimeField`, `FechaValidator` | `FechaValidatorTest` (12 casos), `DateTimeUtilsTest` (7 casos). |
| RF-35 | Campos `creadoPor`/`creadoEn` y `asistenciaRegistradoPor`/`asistenciaFecha` | `MappersTest` (7 casos). |
| RF-36 | `LiveClock` en `DashboardScreen` | Manual. |
| RF-29 | Pendiente: API Spring Boot/EC2 | Documentado en `ALCANCE.md`. |
| RNF-13 | JaCoCo en `app/build.gradle.kts` | Reporte 82,7% instrucciones / 94,1% líneas. |
