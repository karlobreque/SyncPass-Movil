# SyncPass Móvil

Aplicación Android nativa en Kotlin para la gestión de proyectos de vinculación con el medio, inscripción de beneficiarios, acreditación de asistencia mediante QR y validación de presencia por GPS.

> **Estado del entregable:** MVP funcional en modo local/demo. Todos los paneles operan con datos ficticios almacenados en Room. La integración remota con Supabase se activa automáticamente al configurar credenciales; la capa Spring Boot/EC2 está planificada y documentada, pero no forma parte del código de este repositorio.

---

## 1. Índice de documentación

| Documento | Contenido |
| --- | --- |
| [REGISTRO.md](REGISTRO.md) | **Registro de trabajo:** qué se hizo en cada sesión, archivos tocados y verificación. |
| [PROBLEMATICA.md](PROBLEMATICA.md) | Contexto, problema, actores y propuesta de valor. |
| [REQUISITOS.md](REQUISITOS.md) | Requerimientos funcionales y no funcionales, reglas de negocio y criterios de aceptación. |
| [ALCANCE.md](ALCANCE.md) | Alcance del MVP, entregables, estado frente a la rúbrica y backlog. |
| [TECNOLOGIAS.md](TECNOLOGIAS.md) | Stack, versiones, APIs nativas y justificación técnica. |
| [ARQUITECTURA.md](ARQUITECTURA.md) | Modelo C4 (C1, C2, C3), modelo de datos, offline-first y algoritmo GPS. |
| [METODOLOGIA.md](METODOLOGIA.md) | Forma de trabajo, iteraciones, Definition of Done, testing y uso de IA. |
| [AVANCES.md](AVANCES.md) | Bitácora de avances, métricas de calidad y estado actual. |
| [CAMBIOS.md](CAMBIOS.md) | Registro de cambios significativos y compatibilidad. |
| [MAP.md](MAP.md) | **Mapa del proyecto:** qué contiene cada archivo y si debe modificarse. |
| [MOCKUPS.md](MOCKUPS.md) | Wireframes en texto y trazabilidad visual. |
| [mockups/index.html](mockups/index.html) | **Presentación visual interactiva** (15 pantallas Android + 5 Web). |
| [users.md](users.md) | Credenciales y cuentas de demostración (datos ficticios). |

---

## 2. Inicio rápido

### Requisitos

- Android Studio con JDK 17 o superior (el proyecto compila con JDK 27).
- Android SDK con `compileSdk 37`.
- Dispositivo o emulador con Android 7.0 (API 24) o superior.
- GPS y cámara para probar los flujos de asistencia.

### Compilar

```bash
./gradlew :app:assembleDebug
```

APK generado en `app/build/outputs/apk/debug/app-debug.apk`.

### Ver los mockups

Abrir en el navegador:

```bash
xdg-open docs/mockups/index.html
```

El archivo incluye un toggle **Android / Web** y los QR se generan en el navegador.

### Pruebas y cobertura

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:jacocoDebugUnitTestReport
```

Reporte HTML: `app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html`.

---

## 3. Modo local/demo

Si `SUPABASE_URL` y `SUPABASE_ANON_KEY` están vacíos (situación actual), la app:

1. Siembra datos ficticios en Room la primera vez que arranca.
2. Permite iniciar sesión desde el bloque **Acceso rápido de demostración**.
3. Ejecuta altas, bajas, modificaciones, inscripciones, QR, asistencia, encuestas e informes contra la base local.
4. Encola las operaciones en `pending_action` para sincronizarlas cuando exista backend remoto.

### Cuentas de demostración

| Rol | Correo | Panel |
| --- | --- | --- |
| Administrador | `pinkyfloresflores@gmail.com` | Usuarios y roles |
| Docente | `docente@duocuc.cl` | Proyectos, eventos, QR e informes |
| Voluntario | `voluntario@duocuc.cl` | Escáner QR y validación GPS |
| Beneficiario | `je.aquino@duouc.cl` | Inscripciones, pase QR y encuesta |

Las cuentas institucionales de Cognito siguen disponibles desde el formulario normal. Ver [users.md](users.md).

---

## 4. Demostración sugerida (5–8 minutos)

1. **Login demo**: elegir el rol y un perfil ficticio (por ejemplo, Voluntario) y mostrar su panel.
2. **Crear o editar un evento** eligiendo región, comuna y radio (20/40/60/80/100 m). No se piden coordenadas ni ubicación actual.
3. **Configurar la ubicación GPS** del evento desde su tarjeta: **“Usar mi ubicación actual”** o pegar un enlace de Google Maps. Es opcional.
4. **Login demo** como Beneficiario, inscribirse al evento y abrir el **pase QR**.
5. **Login demo** como Voluntario, abrir el escáner del evento con GPS.
6. Mostrar el estado **“Ubicación validada”** con distancia y precisión.
7. Escanear el QR del beneficiario: la asistencia queda registrada.
8. Si el evento tiene coordenadas y el teléfono está fuera del radio, el registro se **bloquea** con el mensaje de distancia.
9. Volver al panel Docente y mostrar **Informes** actualizados.
10. Mostrar el botón **Sincronizar ahora** y el aviso de modo local.

> Para una demo sin permisos de GPS se puede usar el evento **“Taller de Smartphone Básico”**, que no tiene coordenadas y registra la asistencia como `SIN_VALIDACION`. El evento **“Trámites Digitales en Línea”** conserva coordenadas del seed y radio de 100 m para demostrar el bloqueo/validación GPS. Los eventos nuevos se crean sin coordenadas y pueden georreferenciarse después desde su tarjeta.

---

## 5. Funcionalidades implementadas

- Autenticación con AWS Cognito y sesión persistente.
- Acceso demo local por rol cuando no hay backend.
- Gestión de usuarios y roles (administrador).
- CRUD de proyectos y eventos (docente), con responsables docentes/voluntarios, historial de asignaciones, región, comuna, radio cerrado y validación del período del proyecto.
- Acceso demo con selector de rol y varios perfiles ficticios por rol.
- Generación y lectura de códigos QR de asistencia.
- Validación GPS con radio de 20/40/60/80/100 m, tolerancia por precisión y bloqueo fuera de rango.
- Configuración del punto GPS del evento desde su tarjeta: ubicación actual o enlace de Google Maps, con aviso de precisión y opción de quitar. No se exige al crear.
- Inscripción/cancelación de beneficiarios y encuesta de satisfacción.
- Métricas e informes por evento y proyecto.
- Persistencia offline con Room y cola explícita de sincronización.
- Sincronización manual desde el dashboard y automática con WorkManager.
- Pruebas unitarias y reporte JaCoCo.

## 6. Limitaciones conocidas

- **Supabase no está configurado** en este entorno; la app opera en modo local.
- **API Spring Boot en EC2 no está implementada** en este repositorio. La app usa PostgREST de Supabase como backend remoto cuando hay credenciales.
- La sincronización remota no puede verificarse sin instancias disponibles.
- La cobertura JaCoCo se enfoca en lógica de dominio testeable en JVM; las pantallas Compose y `LocationProvider` requieren pruebas instrumentadas/manuales.
- **Fase actual de pruebas locales:** Supabase está offline. Las validaciones, el CRUD, los responsables y el historial se prueban en Room. La sincronización con el modelo de Supabase se implementará en una fase posterior.

**Proyecto hermano:** el prototipo web vive fuera de este repositorio en [`../../SyncPass-Web`](../../SyncPass-Web) (HTML/CSS/JS estático, sin backend). No forma parte del MVP Android.

El detalle y el plan de cierre están en [ALCANCE.md](ALCANCE.md) y [AVANCES.md](AVANCES.md).

---

## 7. Estructura del proyecto

```text
SyncPass-Movil/
├── app/src/main/java/cl/kcorp/syncpass/
│   ├── data/
│   │   ├── auth/         # Cognito, JWT, sesión y acceso demo
│   │   ├── local/        # Room: entidades, DAO, seed y mappers
│   │   ├── location/     # GPS y validador Haversine puro
│   │   ├── model/        # Modelos de dominio
│   │   ├── remote/       # Cliente Supabase/PostgREST
│   │   ├── repository/   # Única fuente de verdad y fallback local
│   │   └── sync/         # WorkManager y cola pendiente
│   ├── ui/
│   │   ├── components/   # Componentes reutilizables
│   │   ├── dashboard/    # Contenedor por rol y sincronización
│   │   ├── login/        # Login Cognito + acceso demo
│   │   ├── panels/       # Paneles Admin/Docente/Voluntario/Beneficiario
│   │   ├── scanner/      # Cámara + ML Kit
│   │   └── theme/        # Tema Compose
│   └── MainActivity.kt
├── app/src/test/         # Pruebas unitarias JVM
└── docs/                 # Documentación del proyecto
```

---

## 8. Autoría y contexto académico

Proyecto desarrollado para la asignatura de Desarrollo de Aplicaciones Móviles Nativas (Duoc UC), Evaluación 3. Los datos de demostración son ficticios y no contienen información personal real.
