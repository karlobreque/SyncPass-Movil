# Tecnologías y decisiones técnicas

## 1. Resumen del stack

| Capa | Tecnología | Versión | Uso |
| --- | --- | --- | --- |
| Lenguaje | Kotlin | 2.2.10 | Todo el código de la app. |
| Build | Android Gradle Plugin | 9.5.1 | Compilación Android. |
| Build | Gradle + Kotlin DSL | Wrapper del proyecto | Automatización de tareas. |
| UI | Jetpack Compose + Material 3 | BOM 2026.02.01 | Interfaz declarativa. |
| Arquitectura | MVVM + Repository | — | Separación estricta de capas. |
| Concurrencia | Kotlin Coroutines + StateFlow | 1.10.2 | Asincronía y estado reactivo. |
| Persistencia | Room | 2.8.5 | Base local offline-first. |
| KSP | KSP | 2.3.12 | Generación de código de Room. |
| Red | OkHttp | 4.12.0 | Cliente HTTP hacia Supabase/PostgREST. |
| JSON | Gson | 2.11.0 | Serialización/deserialización. |
| Backend actual | Supabase PostgREST | — | API REST sobre PostgreSQL. |
| Autenticación | AWS Cognito SDK para Android | 2.77.0 | Login, tokens y grupos. |
| Cámara | CameraX | 1.4.2 | Vista previa y análisis de imagen. |
| QR | ML Kit Barcode Scanning | 17.3.0 | Lectura de códigos QR. |
| QR (generación) | ZXing core | 3.5.3 | Generación del pase QR. |
| Background | WorkManager | 2.10.5 | Sincronización periódica. |
| Navegación por sesión | Activity Compose + ViewModel | 1.13.0 / 2.11.0 | Contenedor y ciclo de vida. |
| Testing | JUnit 4 | 4.13.2 | Pruebas unitarias JVM. |
| Cobertura | JaCoCo | 0.8.13 | Reporte de cobertura. |
| SDK Android | compile/target/min | 37 / 37 / 24 | Compatibilidad Android 7.0+. |
| JDK de compilación | JDK | 27 (target Java 11) | Toolchain local. |

## 2. Justificación de decisiones

### 2.1 Kotlin + Jetpack Compose

- Kotlin es el lenguaje oficial de Android y permite null-safety, coroutines y expresividad.
- Compose reduce el código de UI, facilita estados reactivos y mantiene consistencia con Material 3.
- La UI observa `StateFlow` desde los ViewModels, sin lógica de negocio en composables.

### 2.2 MVVM con Repository

- **View:** composables que renderizan estado y emiten eventos.
- **ViewModel:** orquesta casos de uso, valida y expone `UiState`.
- **Repository:** única fuente de verdad; decide remoto vs. caché/local y encola pendientes.
- **Data sources:** Room (local), Supabase/PostgREST (remoto) y `LocationProvider` (hardware).

Esto permite probar la lógica de dominio sin emulador y cambiar el backend sin reescribir pantallas.

### 2.3 Room como fuente local

- Soporta consultas tipadas y migraciones.
- Permite que la app arranque y opere sin red.
- Se usa junto a `pending_action` para registrar operaciones que deben viajar al backend.
- Los IDs locales son negativos para no colisionar con los IDs positivos del servidor.

### 2.4 Supabase/PostgREST

- Entrega una API REST inmediata sobre PostgreSQL, útil para el MVP.
- La app ya implementa operaciones CRUD, filtros y selección de columnas.
- La configuración se inyecta por `local.properties`/variables de entorno, sin hardcodear secretos en el código.

### 2.5 AWS Cognito

- Gestiona identidad y contraseñas sin almacenarlas en la app.
- Los grupos `cognito:groups` definen el rol.
- El SDK maneja tokens y restauración de sesión.
- Se añadió un acceso demo local para poder evaluar sin depender de la nube.

### 2.6 APIs nativas de Android

| API | Implementación | Requerimiento de la rúbrica |
| --- | --- | --- |
| Cámara | CameraX + ML Kit | Lectura de QR en tiempo real. |
| Geolocalización | `LocationManager` (GPS/NETWORK/PASSIVE) | Validación de presencia en el evento. |
| Background | WorkManager | Sincronización diferida. |

La validación de distancia se implementó como **Kotlin puro** (`LocationValidator`) para poder testearla con JUnit sin Android.

### 2.7 WorkManager

- Reintenta la sincronización sin depender de que la app esté abierta.
- Respeta restricciones de red.
- Se complementa con el botón manual “Sincronizar ahora”, requisito explícito de la rúbrica.

### 2.8 JaCoCo

- Genera reporte HTML/XML de cobertura.
- Se enfoca en la lógica de dominio testeable en JVM: validador GPS, cuentas demo, modelos y mappers.
- Las pantallas Compose y `LocationProvider` se excluyen del reporte porque requieren pruebas instrumentadas.

## 3. Dependencias no usadas o deuda técnica

| Dependencia | Estado | Observación |
| --- | --- | --- |
| Retrofit + converter-gson | Declaradas | La app usa un cliente HTTP propio sobre OkHttp para PostgREST. Se mantienen para la futura API Spring Boot. |
| Spring Boot / EC2 | No implementado | Deuda técnica principal frente a la rúbrica. |
| API Gateway | No implementado | Opcional. |
| Pruebas instrumentadas | Solo ejemplo | Pendiente cubrir GPS, cámara y flujos Compose. |

## 4. Configuración del entorno

### 4.1 Variables de Supabase

La app lee, en este orden:

1. `local.properties`
2. `../../SyncPass-Web/.env` o `.env.local`
3. Variables de entorno del sistema

Claves aceptadas:

```properties
SUPABASE_URL=https://<proyecto>.supabase.co
SUPABASE_ANON_KEY=<anon-key>
```

También se aceptan `VITE_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, entre otras variantes.

> Sin estas claves la app funciona en modo local/demo.

### 4.2 Comandos útiles

```bash
# Compilar APK debug
./gradlew :app:assembleDebug

# Ejecutar pruebas unitarias
./gradlew :app:testDebugUnitTest

# Generar reporte de cobertura
./gradlew :app:jacocoDebugUnitTestReport

# Limpiar y recompilar
./gradlew clean :app:assembleDebug
```

### 4.3 Seguridad

- No se deben versionar claves reales ni contraseñas.
- `docs/users.md` contiene credenciales de demostración; debe rotarse cualquier credencial expuesta.
- Los datos de demo son ficticios.
- La app usa HTTPS para el backend remoto y delega la autenticación a Cognito.

## 5. Compatibilidad

- **minSdk 24:** cobertura amplia de dispositivos, incluyendo equipos de gama baja usados en terreno.
- **targetSdk 37:** cumplimiento de políticas recientes de Android.
- **Java 11 como target:** compatibilidad con el toolchain, aunque el JDK local sea superior.
- **Sin dependencias de Google Play Services para GPS:** se usa `LocationManager`, lo que permite probar en emuladores y dispositivos sin servicios de Google.
