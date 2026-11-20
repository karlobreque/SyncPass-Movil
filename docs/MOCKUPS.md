# Mockups y trazabilidad visual

> **Propósito:** documentar la estructura de todas las vistas del MVP y su correspondencia con el código Compose.
> **Naturaleza:** wireframes de baja fidelidad que representan la interfaz **implementada**, no un diseño gráfico separado.
> **Convención:** `[ ]` botón, `( )` campo, `▣` tarjeta, `●` estado, `…` contenido variable.
> **Datos:** los valores numéricos y algunas fechas de los wireframes son ilustrativos; la estructura, los textos de acción y los estados sí corresponden a la implementación.

---

## 1. Sistema de diseño

| Elemento | Decisión |
| --- | --- |
| Framework | Jetpack Compose + Material 3 |
| Color principal | Coral institucional (`BrandPrimary`) con degradado coral→rojo. |
| Fondo | Claro, con tarjetas blancas y bordes suaves. |
| Superficies | `surface` para tarjetas, `surfaceVariant` para bloques informativos. |
| Tipografía | Escala Material 3; títulos en `titleMedium`/`titleSmall` con `SemiBold`. |
| Espaciado | 16 dp en pantallas, 8–12 dp entre bloques, 14–16 dp dentro de tarjetas. |
| Radios | 20 dp en login, 14–20 dp en tarjetas, 12 dp en botones. |
| Navegación | Pestañas tipo pill en la parte superior de cada panel. |
| Estados | `StatusChip` verde/rojo, banners informativos y de error. |
| Accesibilidad | Textos en español, botones con verbo de acción y mensajes con causa. |

Componentes reutilizables principales:

| Componente | Uso |
| --- | --- |
| `PanelHeader` | Título y subtítulo de cada vista. |
| `PillTabRow` | Navegación entre pestañas del panel. |
| `MetricCard` / `MetricBox` | Indicadores numéricos. |
| `StatusChip` | Estado de usuario, evento o inscripción. |
| `InfoBanner` / `ErrorBanner` | Avisos de modo local, resultados y errores. |
| `EmptyMessage` / `LoadingMessage` | Estados vacíos y de carga. |
| `QrCodeImage` | Pase QR del beneficiario y QR del evento. |
| `GpsStatusCard` | Validación de ubicación en el escáner. |

---

## 2. Login

**Archivo:** `ui/login/LoginScreen.kt`

```text
┌──────────────────────────────────────────┐
│                                          │
│                  ┌────┐                  │
│                  │  S │  (degradado)     │
│                  └────┘                  │
│                 SyncPass                 │
│            GESTIÓN COMUNITARIA           │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ Bienvenido de nuevo                │  │
│  │ ( Correo electrónico             ) │  │
│  │ ( Contraseña                 👁  ) │  │
│  │ [        Iniciar sesión          ] │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ Acceso rápido de demostración      │  │
│  │ Elige un rol y luego un perfil:    │  │
│  │ [Admin] [Docente] [Voluntario] [Benef.]│
│  │ 3 perfiles para Voluntario         │  │
│  │ [ Voluntario Demo · correo       ] │  │
│  │ [ Voluntaria Acreditación · correo] │  │
│  │ [ Voluntario de Huerto · correo  ] │  │
│  └────────────────────────────────────┘  │
│                                          │
│      Acceso seguro mediante AWS Cognito  │
└──────────────────────────────────────────┘
```

**Variantes**

| Estado | Comportamiento visual |
| --- | --- |
| Credenciales inválidas | Banner rojo con el mensaje de Cognito. |
| Contraseña temporal | Formulario de cambio obligatorio. |
| Atributos faltantes | Solicita nombre y teléfono. |
| Login exitoso | Muestra tokens y botón “Continuar”. |
| Supabase configurado | Oculta la tarjeta de acceso demo. |

**Trazabilidad:** `DemoAuth.accountsFor(role)` alimenta la lista de perfiles; `MainActivity` convierte la cuenta elegida en `UserSession`.

---

## 3. Contenedor del dashboard

**Archivo:** `ui/dashboard/DashboardScreen.kt`

```text
┌──────────────────────────────────────────┐
│  ▰ SyncPass          [DOCENTE] (LM) Salir│
├──────────────────────────────────────────┤
│  ℹ Modo local/demo activo: datos ficticios│
│──────────────────────────────────────────│
│  SINCRONIZACIÓN                          │
│  Sin registros pendientes.               │
│                        [ Sincronizar ahora ]│
├──────────────────────────────────────────┤
│  [Usuarios][Docente][Voluntario][Benef.] │  ← solo Admin
│  o pestañas del rol correspondiente      │
│                                          │
│              (contenido del panel)       │
│                                          │
└──────────────────────────────────────────┘
```

**Elementos**

- Marca `SyncPass` y chip con el rol.
- Avatar con iniciales y botón **Salir**.
- Banner de modo local si Supabase no está configurado.
- Contador de pendientes y mensaje de la última sincronización.
- Botón **Sincronizar ahora** (deshabilitado mientras sincroniza).
- Contenido del panel según `Role`.

**Trazabilidad:** `DashboardViewModel` expone `pendingSync`, `syncing` y `syncMessage`.

---

## 4. Panel Administrador — Usuarios

**Archivo:** `ui/panels/AdminPanel.kt`

```text
┌──────────────────────────────────────────┐
│ [Usuarios][Docente][Voluntario][Benef.]  │
├──────────────────────────────────────────┤
│ Panel Administrador                      │
│ Gestión de usuarios y acceso a todas las │
│ vistas.                                  │
│ ℹ Modo local activo…                     │
│                                          │
│ Usuarios (4)              [ + Nuevo usuario ]│
│ ┌────────────────────────────────────┐   │
│ │ Nuevo usuario                      │   │
│ │ ( Nombre de usuario             )  │   │
│ │ ( Nombre a mostrar              )  │   │
│ │ ( Correo                        )  │   │
│ │ ( Rol        ▼ ) ( Estado     ▼ )  │   │
│ │ [Crear usuario] [Cancelar]         │   │
│ └────────────────────────────────────┘   │
│ [ Actualizar usuarios ]                  │
│                                          │
│ ┌────────────────────────────────────┐   │
│ │ Administrador Demo       ● ACTIVO  │   │
│ │ @admin.demo                        │   │
│ │ pinkyfloresflores@gmail.com        │   │
│ │ Rol: Administrador                 │   │
│ │ [Editar]  [Eliminar]               │   │
│ └────────────────────────────────────┘   │
│ ┌────────────────────────────────────┐   │
│ │ Docente Demo             ● ACTIVO  │   │
│ │ …                                  │   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

**Pestañas embebidas:** Docente, Voluntario y Beneficiario reutilizan sus paneles completos, lo que permite al administrador revisar todas las vistas.

**Diálogos**

```text
┌──────────────────────────────────┐
│ Editar usuario                   │
│ ( Nombre de usuario           )  │
│ ( Nombre a mostrar            )  │
│ ( Correo                      )  │
│ ( Rol ▼ )  ( Estado ▼ )          │
│           [Guardar] [Cancelar]   │
└──────────────────────────────────┘
```

**Trazabilidad:** `AdminViewModel` + `SyncPassRepository`; en local usa usuarios y roles sembrados.

---

## 5. Panel Docente — Resumen

**Archivo:** `ui/panels/DocentePanel.kt` → `ResumenTab`

```text
┌──────────────────────────────────────────┐
│ [Resumen][Proyectos][Eventos][Informes]  │
├──────────────────────────────────────────┤
│ Hola, Docente                            │
│ Este es el estado de tus proyectos y     │
│ actividades comunitarias.                │
│                                          │
│ ┌──────────────┐ ┌────────────────────┐  │
│ │ Proyectos    │ │ Inscritos          │  │
│ │ 2            │ │ 12                 │  │
│ │ 3 eventos    │ │ Participantes      │  │
│ └──────────────┘ └────────────────────┘  │
│ ┌──────────────┐ ┌────────────────────┐  │
│ │ Asistencia   │ │ Satisfacción       │  │
│ │ 58%          │ │ 3.8/4              │  │
│ │ 7 registros  │ │ 4 encuestas        │  │
│ └──────────────┘ └────────────────────┘  │
│                                          │
│ ┌ Próximo evento          [Gestionar] ┐  │
│ │ Taller de Smartphone Básico          │  │
│ │ 20 nov · Sede Vecinal Los Aromos     │  │
│ │ ● PROGRAMADO   ● 30 cupos            │  │
│ └──────────────────────────────────────┘  │
│                                          │
│ ┌ Proyectos activos        [Ver todos] ┐  │
│ │ Alfabetización Digital        [Ver]  │  │
│ │ 01 nov → 15 dic                      │  │
│ └──────────────────────────────────────┘  │
│                                          │
│ ┌ Actividad reciente        [Informes] ┐  │
│ │ Trámites Digitales en Línea     70%  │  │
│ │ 27 nov · 15:00                       │  │
│ └──────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

**Trazabilidad:** métricas desde `getMetricasEvento` (remotas o `metricasLocales`).

---

## 6. Panel Docente — Proyectos

**Archivo:** `ui/panels/DocentePanel.kt` → `ProyectosTab`

```text
┌──────────────────────────────────────────┐
│ Panel Docente                            │
│ Gestiona tus proyectos comunitarios.     │
│                                          │
│ Proyectos (2)             [ + Nuevo proyecto ]│
│ ┌────────────────────────────────────┐   │
│ │ Nuevo proyecto                     │   │
│ │ ( Título                        )  │   │
│ │ ( Descripción                   )  │   │
│ │ ( Fecha inicio  [calendario]    )  │   │
│ │ ( Fecha término [calendario]    )  │   │
│ │ ( Docentes a cargo: Docente Demo )  │   │
│ │ ( Voluntarios a cargo: Voluntario ) │   │
│ │ [Crear proyecto] [Cancelar]        │   │
│ └────────────────────────────────────┘   │
│                                          │
│ ┌────────────────────────────────────┐   │
│ │ Alfabetización Digital    ● EN_CURSO│  │
│ │ Proyecto de talleres comunitarios. │   │
│ │ 01 nov 2026 → 15 dic 2026          │   │
│ │ 2 eventos creados.                 │   │
│ │ Docentes a cargo: Docente Demo     │   │
│ │ Voluntarios a cargo: Voluntario Demo│  │
│ │ Creado por Docente Demo · 25 oct   │   │
│ │ Proyecto seleccionado              │   │
│ │ [Ver eventos] [Editar]             │   │
│ │ [Historial de responsables]        │   │
│ │ [Eliminar]                         │   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

**Comportamiento de “Ver eventos”:** selecciona ese proyecto y cambia automáticamente a la pestaña **Eventos**, mostrando solo sus actividades. Si el proyecto aún no tiene eventos, la tarjeta muestra **“Aún no tiene eventos creados.”** y la pestaña Eventos indica el nombre del proyecto sin actividades.

**Responsables:** docentes y voluntarios se agregan o quitan con selectores múltiples. Cada cambio queda en **“Historial de responsables”** con fecha, acción y actor.

**Diálogo de edición:** mismos campos + `Estado`, con **Guardar** y **Cancelar**. Los selectores de fecha abren un calendario Material 3; el rango inválido muestra un mensaje y deshabilita el guardado.

---

## 7. Panel Docente — Eventos

**Archivo:** `ui/panels/DocentePanel.kt` → `EventosTab`

```text
┌──────────────────────────────────────────┐
│ Eventos y actividades                    │
│ [Proyecto: Alfabetización Digital ▼]     │
│ Elige el proyecto y asígnale sus eventos.│
│                                          │
│ Eventos                        [ + Crear ]│
│ ┌────────────────────────────────────┐   │
│ │ Trámites Digitales en Línea ● PROG.│  │
│ │ 27 nov 15:00 · Biblioteca · Cupos: 25│ │
│ │ Ubicación: Maipú, Metropolitana    │  │
│ │ Creado por Docente Demo · 06 nov   │  │
│ │ Docentes responsables: Docente Demo│  │
│ │ Voluntarios responsables: Voluntario│ │
│ │ Taller para realizar trámites…     │  │
│ │ ┌ Asistencia ┐ ┌ Encuestas ┐        │  │
│ │ │ 7/12 (58%) │ │ 4/12 (33%)│        │  │
│ │ └────────────┘ └───────────┘        │  │
│ │ ┌ Satisfac. ┐ ┌ Comentarios ┐       │  │
│ │ │ 3.8/4     │ │ 4           │       │  │
│ │ └───────────┘ └─────────────┘       │  │
│ │ Comentarios                         │  │
│ │ • Muy útil y claro                  │  │
│ │ [Ver QR] [Editar]  [Eliminar]       │  │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

### 7.1 Formulario de evento

```text
┌──────────────────────────────────────────┐
│ Datos del evento                         │
│ ( Evento para: Alfabetización ▼ )        │
│ ( Nombre del evento                  )   │
│ ( Descripción                        )   │
│ ( Inicio   [calendario + reloj]      )   │
│ ( Término  [calendario + reloj]      )   │
│ Período del proyecto: 01-11 al 15-12.    │
│ ⚠ El evento no puede salir del período. │
│ ( Cupos )   ( Recinto/Sala )             │
│ ( Dirección (opcional)              )     │
│ ( Región: Metropolitana de Santiago ▼ )  │
│ ( Comuna: Maipú ▼ )                      │
│ ( Radio de validación: 100 m ▼ )         │
│ Validación GPS opcional; se puede        │
│ configurar más adelante.                 │
│ ( Docentes responsables: Docente Demo )  │
│ ( Voluntarios responsables: Voluntario ) │
│ [Crear evento]  [Cancelar]               │
└──────────────────────────────────────────┘
```

**Reglas visuales**

- Las fechas y horas se eligen con calendario y reloj; no se escriben a mano.
- Se muestra el período del proyecto y el evento debe quedar dentro de ese rango.
- Si el término queda antes del inicio o el evento se sale del período del proyecto, se muestra el mensaje y se deshabilita el botón.
- El último día del proyecto se admite completo, a cualquier hora.
- El selector de proyecto permite crear el evento sin salir de la pestaña.
- La región se elige de una lista cerrada y la comuna se filtra según la región.
- El radio solo ofrece **20, 40, 60, 80 o 100 m**; no se escribe a mano.
- No se piden latitud, longitud ni ubicación actual al crear el evento.
- Los eventos nuevos quedan sin coordenadas y registran `SIN_VALIDACION` hasta configurar GPS.
- El evento exige al menos un docente o voluntario responsable; se preseleccionan los docentes del proyecto.

### 7.2 Diálogo QR del evento

```text
┌──────────────────────────────────┐
│ QR del evento                    │
│      ┌──────────────────┐        │
│      │   ██ ▄▄ ██ ▄▄    │        │
│      │   QR DEL EVENTO  │        │
│      └──────────────────┘        │
│  Trámites Digitales en Línea     │
│           [Cerrar]               │
└──────────────────────────────────┘
```

### 7.3 Historial de responsables

```text
┌──────────────────────────────────────────┐
│ Historial de responsables                │
│ Alfabetización Digital                    │
│ ┌──────────────────────────────────────┐ │
│ │ AGREGADO · VOLUNTARIO                │ │
│ │ Voluntario Demo                      │ │
│ │ 16 nov 2026 10:30                    │ │
│ │ Registrado por Docente Demo          │ │
│ └──────────────────────────────────────┘ │
│ ┌──────────────────────────────────────┐ │
│ │ ELIMINADO · DOCENTE                  │ │
│ │ Docente de Huerto                    │ │
│ │ 16 nov 2026 11:00                    │ │
│ │ Registrado por Docente Demo          │ │
│ └──────────────────────────────────────┘ │
│                              [Cerrar]     │
└──────────────────────────────────────────┘
```

### 7.4 Diálogo “Ubicación GPS del evento”

**Archivo:** `ui/components/UbicacionEventoDialog.kt`

```text
┌──────────────────────────────────────────┐
│ Ubicación GPS del evento                 │
│ Trámites Digitales en Línea              │
│ ℹ Configurar el GPS no es obligatorio.   │
│   Sin GPS: asistencias SIN VALIDACIÓN.   │
│                                          │
│ Punto del evento                         │
│ -33.51130, -70.75800                     │
│ Precisión de la lectura: ±12 m           │
│                                          │
│ [ Usar mi ubicación actual            ]  │
│ ( Enlace de Google Maps o coordenadas )   │
│ [ Leer coordenadas del texto o enlace ]  │
│ ( Radio de validación: 100 m ▼ )         │
│ ⚠ Si la precisión supera el radio/2,     │
│   se sugiere subir el radio.             │
│ [Quitar ubicación GPS]                   │
│                                          │
│        [Cancelar]  [Guardar ubicación]   │
└──────────────────────────────────────────┘
```

**Reglas visuales**

- Se abre desde el botón **“Configurar ubicación GPS”** / **“Cambiar ubicación GPS”** de la tarjeta.
- El texto de estado de la tarjeta muestra el punto y el radio, o “sin coordenadas”.
- “Usar mi ubicación actual” pide permiso de ubicación y muestra la precisión.
- El enlace aceptado puede ser de Google Maps (`@`, `?q=`, `?ll=`, `!3d!4d`) o un par `lat, lng`; los enlaces cortos se explican y no se resuelven sin red.
- Una precisión mayor a 250 m bloquea el guardado como referencia, pero se puede guardar sin GPS.
- El radio se puede cambiar en el mismo diálogo.
- “Guardar sin GPS” mantiene el evento operativo con asistencias `SIN_VALIDACION`.

---

## 8. Panel Docente — Informes

**Archivo:** `ui/panels/DocentePanel.kt` → `InformesTab`

```text
┌──────────────────────────────────────────┐
│ Informes de impacto                      │
│ Alfabetización Digital para Vecinos      │
│ [Proyecto 1] [Proyecto 2]                │
│                                          │
│ ┌────────────────────────────────────┐   │
│ │ Resumen del proyecto               │   │
│ │ Eventos                  3         │   │
│ │ Inscritos                12        │   │
│ │ Asistencias              7         │   │
│ │ Encuestas respondidas    4         │   │
│ │ Satisfacción promedio    3.8/4     │   │
│ │ ─────────────────────────────────  │   │
│ │ Comentarios                        │   │
│ │ • Muy útil y claro                 │   │
│ └────────────────────────────────────┘   │
│                                          │
│ ( Redacta el informe de impacto      )   │
│ (                                    )   │
│ [ Guardar informe ]                      │
│ Informe guardado en esta sesión.         │
└──────────────────────────────────────────┘
```

> El informe textual se mantiene en memoria de la sesión. La exportación PDF/CSV está fuera del MVP.

---

## 9. Panel Voluntario — Eventos

**Archivo:** `ui/panels/VoluntarioPanel.kt` → `EventosVoluntario`

```text
┌──────────────────────────────────────────┐
│ Panel Voluntario                         │
│ Eventos, scanner de asistencia y lista   │
│ de inscritos.                            │
│ ℹ Modo local activo…                     │
│                                          │
│ ┌────────────────────────────────────┐   │
│ │ Taller de Smartphone Básico ● PROG.│  │
│ │ 18 nov 10:00 · Sala 101 · Cupos: 20│  │
│ │ ┌ Asistentes ┐ ┌ Faltantes ┐       │  │
│ │ │ 6          │ │ 4         │       │  │
│ │ └────────────┘ └───────────┘       │  │
│ │ [Lista de inscritos] [Escanear QR] │  │
│ └────────────────────────────────────┘   │
│ ┌────────────────────────────────────┐   │
│ │ Trámites Digitales en Línea ● PROG.│  │
│ │ 20 nov 10:00 · Sala 204 · Cupos: 30│  │
│ │ [Lista de inscritos] [Escanear QR] │  │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

---

## 10. Panel Voluntario — Inscritos

**Archivo:** `ui/panels/VoluntarioPanel.kt` → `InscritosVoluntario`

```text
┌──────────────────────────────────────────┐
│ ← Volver                                 │
│ Inscritos · Trámites Digitales en Línea  │
│ 12 personas                              │
│ [ Actualizar lista ]                     │
│                                          │
│ ┌────────────────────────────────────┐   │
│ │ Persona Demo             ● ASISTIÓ │   │
│ │ persona.demo@example.cl            │   │
│ ├────────────────────────────────────┤   │
│ │ Otra Persona             ● INSCRITO│   │
│ │ otra.persona@example.cl            │   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

---

## 11. Panel Voluntario — Escáner QR + GPS

**Archivo:** `ui/panels/VoluntarioPanel.kt` → vista `SCANNER`

```text
┌──────────────────────────────────────────┐
│ Trámites Digitales en Línea   ← Volver   │
│ Escáner de asistencia                    │
│ ┌────────────────────────────────────┐   │
│ │ Ubicación validada                 │   │
│ │ Estás a 42 m del evento (radio 100 │   │
│ │ m, precisión ±8 m).   [Actualizar GPS]│
│ └────────────────────────────────────┘   │
│ ┌────────────────────────────────────┐   │
│ │                                    │   │
│ │        ┌──────────────┐            │   │
│ │        │              │            │   │
│ │        │   [  QR  ]   │            │   │
│ │        │              │            │   │
│ │        └──────────────┘            │   │
│ │        Apunta al código QR         │   │
│ │                          [ Cerrar ]│   │
│ └────────────────────────────────────┘   │
│ ┌────────────────────────────────────┐   │
│ │ ✅ Asistencia confirmada           │   │
│ │ Ubicación validada: estás a 42 m…  │   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

### 11.1 Estados de `GpsStatusCard`

| Estado | Título | Color | ¿Permite registrar? |
| --- | --- | --- | --- |
| `VALIDADA` | Ubicación validada | Primario | Sí → `VALIDADA` |
| `EVENTO_SIN_COORDENADAS` | Evento sin coordenadas | Neutro | Sí → `SIN_VALIDACION` |
| `SIN_UBICACION` | Ubicación no disponible | Error | No |
| `PRECISION_INSUFICIENTE` | Precisión GPS insuficiente | Error | No |
| `FUERA_DE_RADIO` | Fuera del radio permitido | Error | No |
| Sin lectura aún | Obteniendo ubicación… | Neutro | Espera |

### 11.2 Mensajes de bloqueo

```text
❌ Estás a 250 m del evento; el radio permitido es de 100 m.
   Acércate al lugar para registrar la asistencia.

❌ La señal GPS es muy imprecisa (±400 m). Espera unos segundos
   y vuelve a escanear.

❌ No se pudo obtener tu ubicación. Activa el GPS y concede el
   permiso de ubicación para validar la asistencia.

❌ El QR no corresponde a este evento.
❌ No está en la lista de inscritos.
```

---

## 12. Panel Beneficiario — Mi información

**Archivo:** `ui/panels/BeneficiarioPanel.kt` → `InfoTab`

```text
┌──────────────────────────────────────────┐
│ [Mi información][Eventos][Mi pase QR]    │
├──────────────────────────────────────────┤
│ Panel Beneficiario                       │
│ Tu información y eventos disponibles.    │
│ ℹ Modo local activo…                     │
│ ┌────────────────────────────────────┐   │
│ │ Nombre        Beneficiario Demo    │   │
│ │ Correo        je.aquino@duouc.cl   │   │
│ │ Rol           Beneficiario         │   │
│ │ Estado        ACTIVO               │   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

---

## 13. Panel Beneficiario — Eventos

**Archivo:** `ui/panels/BeneficiarioPanel.kt` → `EventosTab`

```text
┌──────────────────────────────────────────┐
│ Eventos                                  │
│ Inscríbete, muestra tu QR y responde la  │
│ encuesta.                                │
│ ┌────────────────────────────────────┐   │
│ │ Taller de Smartphone Básico        │   │
│ │                    ● No inscrito   │   │
│ │ 18 nov 10:00 · Sala 101            │   │
│ │ Aprende a usar tu teléfono.        │   │
│ │ [Inscribirme]                      │   │
│ └────────────────────────────────────┘   │
│ ┌────────────────────────────────────┐   │
│ │ Trámites Digitales en Línea        │   │
│ │                    ● Inscrito       │   │
│ │ 20 nov 10:00 · Sala 204            │   │
│ │ [Mostrar QR]  [Desuscribirme]      │   │
│ │ ─────────────────────────────────  │   │
│ │ Encuesta de satisfacción           │   │
│ │ ★ ★ ★ ★ ☆                          │   │
│ │ Utilidad: [Muy útil ▼]             │   │
│ │ ( Comentarios (opcional)        )  │   │
│ │ [ Enviar encuesta ]                │   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

**Reglas**

- “Inscribirme” solo aparece si no hay inscripción activa.
- “Mostrar QR” y “Desuscribirme” aparecen si está inscrito y aún no tiene asistencia.
- La encuesta aparece si está inscrito y ya tiene asistencia.
- Si ya respondió: “Encuesta enviada ⭐”.

---

## 14. Panel Beneficiario — Mi pase QR

**Archivo:** `ui/panels/BeneficiarioPanel.kt` → `QrTab`

```text
┌──────────────────────────────────────────┐
│ Mi pase QR                               │
│ Muestra este código al ingresar al evento.│
│                                          │
│ ┌────────────────────────────────────┐   │
│ │ Trámites Digitales en Línea        │   │
│ │ 20 nov 10:00 · Sala 204            │   │
│ │        ┌──────────────────┐        │   │
│ │        │                  │        │   │
│ │        │       QR         │        │   │
│ │        │    240 x 240     │        │   │
│ │        │                  │        │   │
│ │        └──────────────────┘        │   │
│ │ syncpass:asistencia:4:202          │   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

**Formato del contenido:** `syncpass:asistencia:<idUsuario>:<idEvento>`.

---

## 15. Estados transversales

### 15.1 Carga

```text
┌──────────────────────────────────┐
│         ◌ Cargando…              │
└──────────────────────────────────┘
```

### 15.2 Vacío

```text
┌──────────────────────────────────┐
│  No hay eventos disponibles.     │
└──────────────────────────────────┘
```

### 15.3 Error

```text
┌──────────────────────────────────┐
│ ⚠ No se pudo cargar la lista.    │
└──────────────────────────────────┘
```

### 15.4 Modo local

```text
┌──────────────────────────────────────────┐
│ ℹ Modo local/demo activo: datos ficticios │
│   guardados en el dispositivo.            │
└──────────────────────────────────────────┘
```

### 15.5 Confirmación de eliminación

```text
┌──────────────────────────────────┐
│ ¿Eliminar "Taller de…"?          │
│                                  │
│          [Eliminar] [Cancelar]   │
└──────────────────────────────────┘
```

---

## 16. Matriz de trazabilidad visual

| # | Vista | Archivo / composable | Estado | Evidencia |
| --- | --- | --- | --- | --- |
| 1 | Login institucional | `ui/login/LoginScreen.kt` | Implementado | Compila; Cognito manual. |
| 2 | Acceso demo | `LoginScreen` → `DemoAccessCard` | Implementado | `DemoAuthTest`. |
| 3 | Shell del dashboard | `ui/dashboard/DashboardScreen.kt` | Implementado | Reloj en vivo; manual. |
| 4 | Sincronización manual | `DashboardScreen` + `DashboardViewModel` | Implementado | Manual. |
| 5 | Admin — Usuarios | `ui/panels/AdminPanel.kt` | Implementado | Local + manual. |
| 6 | Admin — pestañas embebidas | `AdminPanel` | Implementado | Reutiliza paneles. |
| 7 | Docente — Resumen | `DocentePanel` → `ResumenTab` | Implementado | Métricas locales. |
| 8 | Docente — Proyectos | `DocentePanel` → `ProyectosTab` | Implementado | CRUD local + docentes N:M. |
| 9 | Docente — Eventos | `DocentePanel` → `EventosTab` | Implementado | CRUD + selector de proyecto + GPS. |
| 10 | Formulario de evento | `EventoForm` / `EventoEditDialog` | Implementado | Fechas con pickers + validación. |
| 11 | Docente — Informes | `DocentePanel` → `InformesTab` | Implementado | Métricas locales. |
| 12 | Voluntario — Eventos | `VoluntarioPanel` → `EventosVoluntario` | Implementado | Manual. |
| 13 | Voluntario — Inscritos | `VoluntarioPanel` → `InscritosVoluntario` | Implementado | Muestra quién registró la asistencia. |
| 14 | Voluntario — Escáner | `QrScannerScreen` + `GpsStatusCard` | Implementado | `LocationValidatorTest`. |
| 15 | Beneficiario — Info | `BeneficiarioPanel` → `InfoTab` | Implementado | Manual. |
| 16 | Beneficiario — Eventos | `BeneficiarioPanel` → `EventosTab` | Implementado | Inscripción local. |
| 17 | Beneficiario — QR | `BeneficiarioPanel` → `QrTab` + `QrCodeImage` | Implementado | QR ZXing. |
| 18 | Diálogos de confirmación | `ConfirmDialog` | Implementado | Manual. |
| 19 | Diálogo de ubicación GPS | `ui/components/UbicacionEventoDialog.kt` | Implementado | `UbicacionEventoTest` (22 casos) + manual. |

---

## 17. Pendientes de diseño

| Elemento | Estado | Nota |
| --- | --- | --- |
| Mockups de alta fidelidad | No incluidos | Los wireframes representan la UI real. |
| Vista de mapas | Pendiente | Mejora futura para elegir recinto. |
| Exportación de informes | Pendiente | El botón guarda solo en sesión. |
| Notificaciones | Pendiente | Requiere canal y permisos. |
| Modo oscuro | No verificado | El tema existe, pero no se probó en todos los paneles. |
| Tabletas y horizontal | No verificado | Diseño optimizado para teléfono vertical. |

---

## 18. Cómo validar los mockups contra la app

> También existe una versión interactiva en HTML: [`mockups/index.html`](mockups/index.html), con toggle Android/Web y QR generados en el navegador.

1. Compilar e instalar el APK.
2. Entrar con cada cuenta demo.
3. Recorrer todas las pestañas y comparar con este documento.
4. Verificar los estados de GPS con y sin coordenadas.
5. Revisar que los banners de modo local aparezcan en Admin, Docente, Voluntario y Beneficiario.
6. Confirmar que los textos y botones coinciden con los wireframes.

Las diferencias menores de espaciado o tipografía son esperables; la estructura, los flujos y los mensajes son los documentados aquí.
