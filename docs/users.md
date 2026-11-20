# Usuarios, roles y acceso a la demo

> **Documento interno de evaluación.**
> Todas las cuentas, nombres, correos y datos asociados son **ficticios**. No corresponden a personas reales ni deben usarse fuera del contexto académico.
>
> **Última actualización:** 2026-11-16

---

## 1. Contraseña de demostración

```text
Seafight1499.
```

Esta contraseña se usa únicamente con las cuentas de demostración de AWS Cognito. El **acceso rápido demo** de la app no solicita contraseña: crea una sesión local con el rol seleccionado.

---

## 2. Cuentas de demostración

El acceso rápido permite elegir un **rol** y luego uno de sus **perfiles ficticios**. Hay al menos dos perfiles por rol.

| Rol | Perfiles disponibles | Uso principal |
| --- | --- | --- |
| Administrador | Administrador Demo, Coordinador General | Gestionar usuarios y revisar todos los paneles. |
| Docente | Docente Demo, Docente de Huerto, Docente Vespertino | Crear proyectos/eventos, asignar responsables, ver QR e informes. |
| Voluntario | Voluntario Demo, Voluntaria de Acreditación, Voluntario de Huerto | Acreditar asistencia con QR + GPS. |
| Beneficiario | Beneficiario Demo, Ana Rojas, Bruno Silva, Carla Méndez | Inscribirse, mostrar QR y responder la encuesta. |

Perfiles base y correos:

| Rol | Correo | Nombre en la app |
| --- | --- | --- |
| Administrador | `pinkyfloresflores@gmail.com` | Administrador Demo |
| Docente | `docente@duocuc.cl` | Docente Demo |
| Voluntario | `voluntario@duocuc.cl` | Voluntario Demo |
| Beneficiario | `je.aquino@duouc.cl` | Beneficiario Demo |

> El correo del beneficiario se escribe **exactamente** como `je.aquino@duouc.cl`, tal como está sembrado en la base local y en `DemoAuth`. No lo “corrijas” a `@duocuc.cl` si vas a probar el acceso demo.
>
> Los perfiles adicionales se crean automáticamente en Room al entrar por primera vez con ellos.

## 2.1 Perfiles, responsables y voluntarios

- **Administrador → Usuarios** permite crear, editar y eliminar perfiles de docentes, voluntarios y beneficiarios.
- Los docentes pueden asignar **docentes y voluntarios responsables** a proyectos y eventos.
- Cada cambio de responsable queda en el **Historial de responsables** del proyecto o evento.
- Al eliminar un usuario, se retiran sus asignaciones actuales; el historial conserva el nombre registrado.

---

## 3. Cómo entrar

### 3.1 Acceso rápido local (recomendado para la demo)

1. Abrir la app sin credenciales de Supabase configuradas.
2. En la pantalla de login, bajar hasta **“Acceso rápido de demostración”**.
3. Elegir el rol en la fila de botones. Por defecto aparece **Voluntario**.
4. Elegir uno de los perfiles listados para ese rol.
5. La app abre el panel correspondiente con datos ficticios locales.

Este acceso:

- No requiere internet, Cognito ni Supabase.
- Permite probar distintos docentes y voluntarios sin crear cuentas.
- Sirve para recorrer todos los flujos del MVP.
- No reemplaza el login institucional; solo se muestra cuando Supabase no está configurado.

### 3.2 Login institucional con Cognito

1. Escribir el correo y la contraseña de la sección 1.
2. Si es el primer ingreso, Cognito puede pedir cambio de contraseña y datos adicionales.
3. Al autenticar, la app lee los grupos `cognito:groups` del token y abre el panel del rol.

Si el rol no está reconocido, la app muestra “No se reconoció tu rol. Contacta al administrador.”

---

## 4. Dataset de demostración

La base local se siembra automáticamente **solo si Room está vacía**.

| Elemento | Identificador | Detalle |
| --- | --- | --- |
| Proyecto | `101` | “Alfabetización Digital para Vecinos”. |
| Evento sin coordenadas | `201` | “Taller de Smartphone Básico”. Registra `SIN_VALIDACION`. |
| Evento georreferenciado | `202` | “Trámites Digitales en Línea”, radio 100 m. Permite probar validación y bloqueo. |
| Evento adicional | `203` | “Huerto Urbano: Primera Jornada”, sin coordenadas. |
| Beneficiario | `4` | `je.aquino@duouc.cl`. |
| Inscripciones | `301`–`305` | Con asistencias y encuestas de ejemplo. |

> Si la app ya tenía datos de una versión anterior, el seed no se ejecuta. Para forzar el dataset: desinstalar la app o borrar sus datos desde Ajustes.

---

## 5. Guion corto por rol

### Administrador

1. Entrar con el botón de Administrador.
2. Revisar **Usuarios**: editar un usuario y guardar.
3. Cambiar a las pestañas Docente, Voluntario y Beneficiario para mostrar que el administrador puede supervisar todo.

### Docente

1. Entrar como Docente y elegir el perfil **Docente Demo**.
2. Ir a **Proyectos** y editar “Alfabetización Digital para Vecinos”.
3. Verificar docentes y voluntarios a cargo; agregar o quitar un voluntario.
4. Abrir **Historial de responsables** y mostrar la alta o baja registrada.
5. Ir a **Eventos** y editar “Trámites Digitales en Línea”.
6. Verificar que la región y la comuna se eligen desde listas (Metropolitana de Santiago / Maipú).
7. Definir radio de 100 m desde el selector y guardar.
8. Ir a **Informes** y mostrar asistencias, encuestas y comentarios.

> El evento `202` conserva coordenadas GPS del seed para demostrar la validación. Los eventos nuevos no piden GPS: quedan con `SIN_VALIDACION` hasta configurarlo más adelante.

### Beneficiario

1. Entrar con el botón de Beneficiario.
2. Ir a **Eventos** e inscribirse en “Trámites Digitales en Línea”.
3. Abrir **“Mostrar QR”** o la pestaña **Mi pase QR**.
4. Compartir el QR para que el voluntario lo escanee.

### Voluntario

1. Entrar con el botón de Voluntario.
2. Abrir **Escanear QR** en “Trámites Digitales en Línea”.
3. Verificar la tarjeta GPS: distancia, precisión y estado.
4. Escanear el QR del beneficiario.
5. Confirmar “Asistencia confirmada” o el bloqueo si está fuera del radio.
6. Repetir con el evento sin coordenadas para mostrar `SIN_VALIDACION`.

---

## 6. Recomendaciones de seguridad

1. **No publicar este documento** en repositorios públicos ni en entregables abiertos.
2. **Rotar la contraseña** de las cuentas Cognito después de la evaluación; es una credencial compartida.
3. **No versionar** `local.properties` ni claves de Supabase.
4. **Revocar cualquier token expuesto.** Se detectó un token de GitHub en un archivo externo al proyecto (`~/GitHub/GitHub-KarlObreque.md`); no se utilizó, pero debe revocarse y rotarse desde GitHub.
5. Usar exclusivamente datos ficticios en capturas, videos y demo.
6. Cerrar sesión al terminar la demostración en dispositivos compartidos.
7. Revisar los permisos de ubicación y cámara antes de grabar; la app los solicita en contexto.

---

## 7. Preguntas frecuentes

**¿Por qué no veo el bloque de acceso demo?**
Porque `SUPABASE_URL` y `SUPABASE_ANON_KEY` están configurados. En ese caso se usa el login Cognito y, si el backend responde, el flujo remoto.

**¿Puedo usar el login demo con Supabase configurado?**
No. El acceso demo se oculta a propósito para no mezclar una sesión local con datos remotos.

**¿Qué pasa si escaneo un QR de otro evento?**
La app lo rechaza con “El QR no corresponde a este evento”.

**¿Qué pasa si el evento no tiene coordenadas?**
La asistencia se registra como `SIN_VALIDACION` y se cuenta en las métricas. Así se conserva el flujo QR anterior. Los eventos nuevos se crean sin coordenadas; puedes configurarlas después desde la tarjeta del evento con **“Configurar ubicación GPS”** (ubicación actual o enlace de Google Maps).

**¿Puedo escribir cualquier radio?**
No. El formulario solo permite **20, 40, 60, 80 o 100 m**. Si un evento antiguo tiene otro valor, se ajusta al permitido más cercano.

**¿Qué pasa si estoy fuera del radio?**
La asistencia se bloquea y se muestra la distancia calculada y el radio permitido. No se registra nada.

**¿Los datos se pierden al cerrar la app?**
No. Room conserva los datos locales. Las operaciones pendientes quedan en `pending_action` hasta que se sincronicen.

**¿Dónde veo quién cambió los responsables?**
En cada tarjeta de proyecto o evento, botón **“Historial de responsables”**.

**¿Puedo entrar con otro voluntario?**
Sí. En el acceso demo elige el rol **Voluntario** y luego el perfil que quieras probar.

---

## 8. Contacto y responsabilidad

- **Responsable del proyecto:** autor del entregable SyncPass Móvil.
- **Contexto:** asignatura de Desarrollo de Aplicaciones Móviles Nativas, Duoc UC, Evaluación 3.
- **Uso permitido:** demostración y evaluación académica.
- **Datos:** ficticios; cualquier parecido con personas reales es coincidencia.
