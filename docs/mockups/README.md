# Mockups interactivos SyncPass

Presentación visual en un solo archivo HTML, inspirada en el ejemplo `~/Downloads/ej-mockup/index.html`.

## Abrir

```bash
xdg-open docs/mockups/index.html
```

O arrastrar `index.html` al navegador. No requiere servidor ni dependencias externas obligatorias (el tipado usa Google Fonts si hay conexión; si no, usa fuentes del sistema).

## Contenido

| Sección | Pantallas | Alcance |
| --- | --- | --- |
| **Android** | 15 | MVP implementado: acceso, docente, voluntario, beneficiario, admin, GPS, QR, encuesta y sincronización. |
| **Web** | 5 | **Propuesta visual** para gestión docente/administrador. No está implementada. |

El toggle superior alterna ambas secciones. Los códigos QR se generan en el navegador con JavaScript.

## Pantallas Android

01. Acceso (Cognito + demo)
02. Docente · Resumen
03. Docente · Proyectos
04. Docente · Eventos
05. Docente · Evento con GPS
06. Docente · Informes
07. Voluntario · Eventos
08. Voluntario · Inscritos
09. Escáner · GPS validada
10. Escáner · Bloqueo fuera de radio
11. Beneficiario · Eventos
12. Beneficiario · Pase QR
13. Beneficiario · Encuesta
14. Administrador · Usuarios
15. Sincronización

## Pantallas Web (propuesta)

01. Acceso institucional
02. Resumen docente
03. Proyectos
04. Evento + QR
05. Informe de impacto

## Notas

- Todos los datos son **ficticios**.
- La versión web es una propuesta visual; el prototipo ya existe fuera del proyecto Android, en `../../SyncPass-Web`.
- El detalle textual y la trazabilidad están en [`../MOCKUPS.md`](../MOCKUPS.md).
- El mapa de archivos del proyecto está en [`../MAP.md`](../MAP.md).
