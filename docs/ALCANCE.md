# Alcance del proyecto

## 1. Objetivo del MVP

Entregar una aplicación Android nativa que permita gestionar el ciclo básico de una actividad comunitaria —proyecto, evento, inscripción, acreditación QR con validación GPS y encuesta— operando de forma local cuando no haya backend disponible y sincronizando cuando la conexión se restablezca.

## 2. Alcance incluido

### 2.1 Módulos funcionales

| Módulo | Incluido en el MVP |
| --- | --- |
| Autenticación | Login Cognito, restauración de sesión y acceso demo local por rol. |
| Administración | Listado, alta, edición y baja de usuarios; catálogo de roles. |
| Proyectos | CRUD de proyectos con fechas, estado y responsable. |
| Eventos | CRUD de eventos, cupos, recinto, dirección, región, comuna, responsables docentes/voluntarios y radio GPS cerrado (20/40/60/80/100 m), validando que queden dentro del período del proyecto. Las coordenadas se configuran después del alta. |
| Responsables | Asignación múltiple de docentes y voluntarios a proyectos y eventos, con historial de altas y bajas. |
| Inscripciones | Inscripción, cancelación, reactivación y pase QR. |
| Acreditación | Escaneo QR con cámara, validación de evento e inscripción. |
| Validación GPS | Radio cerrado (20/40/60/80/100 m), tolerancia por precisión y bloqueo fuera de rango. |
| Encuestas | Calificación, utilidad y comentarios. |
| Informes | Asistencias, faltantes, porcentajes, encuestas, promedio y comentarios. |
| Offline-first | Room, cola `pending_action`, WorkManager y botón de sincronización. |
| Demo local | Seed ficticio y acceso rápido sin Supabase/EC2. |

### 2.2 Vistas incluidas

- Login institucional y acceso demo.
- Dashboard por rol.
- Panel Administrador: usuarios y roles.
- Panel Docente: resumen, proyectos, eventos e informes.
- Panel Voluntario: eventos, inscritos y escáner GPS/QR.
- Panel Beneficiario: eventos, mis inscripciones, pase QR, encuesta y perfil.
- Diálogos de creación/edición y confirmación.

Ver wireframes y trazabilidad en [MOCKUPS.md](MOCKUPS.md).

## 3. Fuera de alcance

| Elemento | Motivo |
| --- | --- |
| API Spring Boot desplegada en EC2 | No existe en este repositorio ni en el entorno de evaluación actual. |
| API Gateway | Opcional según la rúbrica. |
| Notificaciones push | No es parte del flujo crítico del MVP. |
| Exportación PDF/CSV | Mejora posterior; hoy el informe es textual en pantalla. |
| Portal web administrativo | El alcance de la app Android es móvil. Existe un prototipo visual independiente en `../../SyncPass-Web`, sin backend. |
| Integración con calendario o mapas externos | Mejora futura. |
| Firma digital o biometría | No requerida para acreditar presencia. |
| Datos reales de beneficiarios | Restricción académica: solo datos ficticios. |

## 4. Supuestos y restricciones

1. El dispositivo del voluntario tiene GPS y cámara funcionales.
2. El docente indica la ubicación con región y comuna; las coordenadas GPS son opcionales y se configuran después desde la tarjeta del evento, con la ubicación actual o un enlace de Google Maps.
3. Los eventos sin coordenadas se consideran válidos y registran `SIN_VALIDACION`.
4. La conectividad puede ser intermitente; la app no debe perder operaciones.
5. Supabase y las instancias EC2 pueden no estar disponibles durante la demo.
6. Los datos sembrados son ficticios y no representan personas reales.
7. El backend remoto esperado expone PostgREST; la app no usa Retrofit actualmente para el flujo principal.

## 5. Estado frente a la rúbrica

| Criterio de la rúbrica | Estado | Evidencia / observación |
| --- | --- | --- |
| Base de datos Supabase configurada y conectada | **Parcial / fase posterior** | Cliente PostgREST implementado y configurable por `local.properties`; este entorno no tiene credenciales y Supabase está offline. La etapa actual valida CRUD y reglas en Room. |
| API REST Spring Boot en AWS EC2 | **Pendiente** | No existe código Spring Boot en el repositorio. La app consume Supabase directamente. Documentado como deuda técnica prioritaria. |
| Autenticación y roles con AWS Cognito | **Implementado** | Login, grupos `cognito:groups`, restauración de sesión y paneles por rol. |
| App 100% Kotlin | **Implementado** | Todo el código de la app está en Kotlin. |
| Al menos 2 APIs nativas de Android | **Implementado** | Cámara (CameraX + ML Kit) y geolocalización (LocationManager). WorkManager como API de background. |
| MVVM estricto | **Implementado** | ViewModels exponen `StateFlow`; pantallas solo observan estado y envían eventos; repositorio centraliza datos. |
| Persistencia local y offline-first | **Implementado** | Room 2.8.5, seed local y caché de proyectos/eventos/inscripciones/usuarios. |
| Mecanismo explícito de sincronización | **Implementado** | Botón “Sincronizar ahora” en dashboard + contador de pendientes + WorkManager. |
| `/docs/ARQUITECTURA.md` con C4 (C1, C2, C3) | **Implementado** | Diagramas Mermaid y explicación de contenedores/componentes. |
| `/docs/ALCANCE.md` con RF, RNF, casos de uso y flujos | **Implementado** | Este documento + `REQUISITOS.md`. |
| Mockups de todas las vistas y trazabilidad | **Implementado** | `MOCKUPS.md` con wireframes y estado por vista. |
| Unit tests | **Implementado** | 72 pruebas JUnit (71 de dominio + 1 de sanidad del entorno). |
| Cobertura JaCoCo ≥ 30% | **Implementado** | 82,7% instrucciones y 94,1% líneas en el perímetro de dominio medido. |
| Defensa individual | **Pendiente** | Preparar explicación de MVVM, offline-first, GPS y autenticación. |

## 6. Criterios de aceptación del MVP

El MVP se considera aceptado cuando:

1. La app compila y genera APK debug sin errores.
2. Se puede iniciar sesión con Cognito o con el acceso demo.
3. Se puede elegir rol y perfil ficticio en el acceso demo.
4. Cada rol accede únicamente a su panel.
5. El docente puede crear un evento con región, comuna, al menos un responsable y radio permitido, sin exigir GPS, siempre que quede dentro del período del proyecto.
6. El docente puede cambiar los responsables de un proyecto o evento y consultar el historial de cambios.
7. El beneficiario puede inscribirse y obtener su QR.
8. El voluntario puede escanear el QR y registrar asistencia.
9. Fuera del radio GPS la asistencia se bloquea con un mensaje claro.
10. Un evento sin coordenadas registra `SIN_VALIDACION` sin romper el flujo.
11. Las métricas del docente reflejan asistencias y encuestas.
12. Las operaciones offline quedan pendientes y se pueden sincronizar.
13. Las pruebas unitarias pasan y el reporte JaCoCo supera el 30%.
14. El docente puede configurar, cambiar o quitar el GPS de un evento desde su tarjeta, con la ubicación actual o un enlace, sin que sea obligatorio al crear.

## 7. Entregables

- Código fuente Android Kotlin.
- APK debug.
- Suite de pruebas unitarias y reporte JaCoCo.
- Documentación en `/docs` (problema, requisitos, alcance, tecnologías, arquitectura, metodología, avances, cambios, mockups y credenciales).
- Guion de demo y cuentas ficticias.

## 8. Roadmap posterior al MVP

| Prioridad | Actividad | Resultado esperado |
| --- | --- | --- |
| Alta | Implementar API Spring Boot y desplegarla en EC2. | Cumplir el contenedor backend de la rúbrica. |
| Alta | Configurar Supabase y validar el flujo remoto completo. | Sincronización real verificada. |
| Alta | Pruebas instrumentadas de GPS/cámara y sincronización. | Cobertura end-to-end. |
| Media | Exportación de informes PDF/CSV. | Evidencia entregable a coordinación. |
| Media | Notificaciones y recordatorios. | Mayor participación. |
| Media | Panel de auditoría de asistencias. | Trazabilidad reforzada. |
| Media | Sincronizar las coordenadas GPS configuradas desde la tarjeta con el esquema remoto. | Validación consistente entre dispositivos. |
| Baja | Mapas y sugerencia de recinto. | Mejor experiencia de configuración. |

## 9. Gestión de cambios de alcance

Todo cambio debe:

1. Registrarse en [CAMBIOS.md](CAMBIOS.md) con motivo e impacto.
2. Actualizar los requisitos afectados en [REQUISITOS.md](REQUISITOS.md).
3. Indicar si modifica el MVP, el modelo de datos o la compatibilidad offline.
4. Añadir o ajustar pruebas cuando corresponda.
