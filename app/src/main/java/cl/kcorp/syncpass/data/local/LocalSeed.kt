package cl.kcorp.syncpass.data.local

/**
 * Datos de demostración 100% ficticios para poder operar la app en modo local
 * cuando la API/EC2 o Supabase no están disponibles.
 *
 * El seed solo se ejecuta si la base Room está vacía. No contiene datos
 * personales reales: nombres, correos y coordenadas son referenciales.
 */
object LocalSeed {

    const val DEMO_EVENT_ID = 201
    const val DEMO_PROJECT_ID = 101

    suspend fun seedIfEmpty(dao: SyncPassDao) {
        if (dao.countProyectos() > 0 || dao.countUsuarios() > 0) return

        dao.upsertUsuarios(usuarios())
        dao.upsertProyectos(proyectos())
        dao.upsertResponsables(responsables())
        dao.upsertEventos(eventos())
        dao.upsertInscripciones(inscripciones())
        dao.upsertInscritos(inscritos())
    }

    private fun usuarios(): List<UsuarioEntity> = listOf(
        UsuarioEntity(
            idUsuario = 1,
            idRol = 1,
            nombreUsuario = "admin.demo",
            nombreMostrar = "Administrador Demo",
            correo = "pinkyfloresflores@gmail.com",
            estado = "ACTIVO",
            rolNombre = "Administrador",
        ),
        UsuarioEntity(
            idUsuario = 2,
            idRol = 2,
            nombreUsuario = "docente.demo",
            nombreMostrar = "Docente Demo",
            correo = "docente@duocuc.cl",
            estado = "ACTIVO",
            rolNombre = "Docente",
        ),
        UsuarioEntity(
            idUsuario = 3,
            idRol = 3,
            nombreUsuario = "voluntario.demo",
            nombreMostrar = "Voluntario Demo",
            correo = "voluntario@duocuc.cl",
            estado = "ACTIVO",
            rolNombre = "Voluntario",
        ),
        UsuarioEntity(
            idUsuario = 4,
            idRol = 4,
            nombreUsuario = "beneficiario.demo",
            nombreMostrar = "Beneficiario Demo",
            correo = "je.aquino@duouc.cl",
            estado = "ACTIVO",
            rolNombre = "Beneficiario",
        ),
        UsuarioEntity(
            idUsuario = 5,
            idRol = 4,
            nombreUsuario = "ana.rojas",
            nombreMostrar = "Ana Rojas (ficticia)",
            correo = "ana.rojas@demo.cl",
            estado = "ACTIVO",
            rolNombre = "Beneficiario",
        ),
        UsuarioEntity(
            idUsuario = 6,
            idRol = 4,
            nombreUsuario = "bruno.silva",
            nombreMostrar = "Bruno Silva (ficticio)",
            correo = "bruno.silva@demo.cl",
            estado = "ACTIVO",
            rolNombre = "Beneficiario",
        ),
        UsuarioEntity(
            idUsuario = 7,
            idRol = 4,
            nombreUsuario = "carla.mendez",
            nombreMostrar = "Carla Méndez (ficticia)",
            correo = "carla.mendez@demo.cl",
            estado = "ACTIVO",
            rolNombre = "Beneficiario",
        ),
    )

    private fun proyectos(): List<ProyectoEntity> = listOf(
        ProyectoEntity(
            idProyecto = DEMO_PROJECT_ID,
            idCoordinador = 2,
            idUnidad = 1,
            idArea = 1,
            titulo = "Alfabetización Digital para Vecinos",
            descripcion = "Talleres prácticos de uso de smartphone y trámites digitales para la comunidad.",
            fechaInicio = "2026-11-01",
            fechaTermino = "2026-12-15",
            estado = "EN_CURSO",
            creadoPor = "Docente Demo",
            creadoEn = "2026-10-25T09:00:00",
        ),
        ProyectoEntity(
            idProyecto = 102,
            idCoordinador = 2,
            idUnidad = 1,
            idArea = 2,
            titulo = "Taller de Huerto Comunitario",
            descripcion = "Jornadas de cultivo urbano y cuidado de espacios comunitarios.",
            fechaInicio = "2026-11-10",
            fechaTermino = "2026-12-20",
            estado = "EN_CURSO",
            creadoPor = "Docente Demo",
            creadoEn = "2026-10-28T11:15:00",
        ),
    )

    private fun responsables(): List<ResponsableEntity> = listOf(
        ResponsableEntity(
            tipoEntidad = "PROYECTO",
            idEntidad = DEMO_PROJECT_ID,
            idUsuario = 2,
            rol = "DOCENTE",
            asignadoEn = "2026-10-25T09:00:00",
            asignadoPor = "Docente Demo",
        ),
        ResponsableEntity(
            tipoEntidad = "PROYECTO",
            idEntidad = DEMO_PROJECT_ID,
            idUsuario = 3,
            rol = "VOLUNTARIO",
            asignadoEn = "2026-10-26T10:00:00",
            asignadoPor = "Docente Demo",
        ),
        ResponsableEntity(
            tipoEntidad = "PROYECTO",
            idEntidad = 102,
            idUsuario = 2,
            rol = "DOCENTE",
            asignadoEn = "2026-10-28T11:15:00",
            asignadoPor = "Docente Demo",
        ),
        ResponsableEntity(
            tipoEntidad = "EVENTO",
            idEntidad = DEMO_EVENT_ID,
            idUsuario = 2,
            rol = "DOCENTE",
            asignadoEn = "2026-11-05T10:30:00",
            asignadoPor = "Docente Demo",
        ),
        ResponsableEntity(
            tipoEntidad = "EVENTO",
            idEntidad = DEMO_EVENT_ID,
            idUsuario = 3,
            rol = "VOLUNTARIO",
            asignadoEn = "2026-11-05T10:30:00",
            asignadoPor = "Docente Demo",
        ),
        ResponsableEntity(
            tipoEntidad = "EVENTO",
            idEntidad = 202,
            idUsuario = 2,
            rol = "DOCENTE",
            asignadoEn = "2026-11-06T12:10:00",
            asignadoPor = "Docente Demo",
        ),
        ResponsableEntity(
            tipoEntidad = "EVENTO",
            idEntidad = 203,
            idUsuario = 2,
            rol = "DOCENTE",
            asignadoEn = "2026-11-07T09:20:00",
            asignadoPor = "Docente Demo",
        ),
        ResponsableEntity(
            tipoEntidad = "EVENTO",
            idEntidad = 203,
            idUsuario = 3,
            rol = "VOLUNTARIO",
            asignadoEn = "2026-11-07T09:20:00",
            asignadoPor = "Docente Demo",
        ),
    )

    private fun eventos(): List<EventoEntity> = listOf(
        EventoEntity(
            idEvento = DEMO_EVENT_ID,
            idProyecto = DEMO_PROJECT_ID,
            nombre = "Taller de Smartphone Básico",
            descripcion = "Uso de cámara, aplicaciones y comunicación con la familia.",
            fechaInicio = "2026-11-20T10:00:00",
            fechaTermino = "2026-11-20T12:00:00",
            cuposMaximos = 30,
            recinto = "Sede Vecinal Los Aromos",
            direccion = "Av. Los Aromos 1234, Maipú",
            region = "Metropolitana de Santiago",
            comuna = "Maipú",
            latitud = null,
            longitud = null,
            radioValidacionMetros = 100,
            estado = "PROGRAMADO",
            proyectoTitulo = "Alfabetización Digital para Vecinos",
            creadoPor = "Docente Demo",
            creadoEn = "2026-11-05T10:30:00",
        ),
        EventoEntity(
            idEvento = 202,
            idProyecto = DEMO_PROJECT_ID,
            nombre = "Trámites Digitales en Línea",
            descripcion = "Simulación de trámites y uso seguro de plataformas públicas.",
            fechaInicio = "2026-11-27T15:00:00",
            fechaTermino = "2026-11-27T17:00:00",
            cuposMaximos = 25,
            recinto = "Biblioteca Comunitaria",
            direccion = "Pasaje Central 456, Maipú",
            region = "Metropolitana de Santiago",
            comuna = "Maipú",
            latitud = -33.5113,
            longitud = -70.7580,
            radioValidacionMetros = 100,
            estado = "PROGRAMADO",
            proyectoTitulo = "Alfabetización Digital para Vecinos",
            creadoPor = "Docente Demo",
            creadoEn = "2026-11-06T12:10:00",
        ),
        EventoEntity(
            idEvento = 203,
            idProyecto = 102,
            nombre = "Huerto Urbano: Primera Jornada",
            descripcion = "Preparación de almácigos y limpieza del espacio comunitario.",
            fechaInicio = "2026-11-21T09:30:00",
            fechaTermino = "2026-11-21T12:30:00",
            cuposMaximos = 20,
            recinto = "Plaza Los Aromos",
            direccion = "Plaza Los Aromos s/n, Maipú",
            region = "Metropolitana de Santiago",
            comuna = "Maipú",
            latitud = null,
            longitud = null,
            radioValidacionMetros = 100,
            estado = "PROGRAMADO",
            proyectoTitulo = "Taller de Huerto Comunitario",
            creadoPor = "Docente Demo",
            creadoEn = "2026-11-07T09:20:00",
        ),
    )

    private fun inscripciones(): List<InscripcionEntity> = listOf(
        InscripcionEntity(
            idInscripcion = 301,
            idUsuario = 4,
            idEvento = DEMO_EVENT_ID,
            estado = "INSCRITO",
            fechaInscripcion = "2026-11-10T09:00:00.000Z",
            asistenciaEstado = null,
            encuestaCalificacion = null,
            encuestaUtilidad = null,
            encuestaComentarios = null,
        ),
        InscripcionEntity(
            idInscripcion = 302,
            idUsuario = 5,
            idEvento = DEMO_EVENT_ID,
            estado = "INSCRITO",
            fechaInscripcion = "2026-11-10T09:05:00.000Z",
            asistenciaEstado = "VALIDADA",
            asistenciaRegistradoPor = "Voluntario Demo",
            asistenciaFecha = "2026-11-20T10:05:00",
            encuestaCalificacion = null,
            encuestaUtilidad = null,
            encuestaComentarios = null,
        ),
        InscripcionEntity(
            idInscripcion = 303,
            idUsuario = 6,
            idEvento = DEMO_EVENT_ID,
            estado = "INSCRITO",
            fechaInscripcion = "2026-11-10T09:10:00.000Z",
            asistenciaEstado = "VALIDADA",
            asistenciaRegistradoPor = "Voluntario Demo",
            asistenciaFecha = "2026-11-20T10:08:00",
            encuestaCalificacion = 5,
            encuestaUtilidad = "MUY_UTIL",
            encuestaComentarios = "Taller claro y práctico.",
        ),
        InscripcionEntity(
            idInscripcion = 304,
            idUsuario = 7,
            idEvento = DEMO_EVENT_ID,
            estado = "INSCRITO",
            fechaInscripcion = "2026-11-10T09:15:00.000Z",
            asistenciaEstado = null,
            encuestaCalificacion = null,
            encuestaUtilidad = null,
            encuestaComentarios = null,
        ),
        InscripcionEntity(
            idInscripcion = 305,
            idUsuario = 4,
            idEvento = 203,
            estado = "INSCRITO",
            fechaInscripcion = "2026-11-11T12:00:00.000Z",
            asistenciaEstado = null,
            encuestaCalificacion = null,
            encuestaUtilidad = null,
            encuestaComentarios = null,
        ),
    )

    private fun inscritos(): List<InscritoEntity> = listOf(
        InscritoEntity(
            idInscripcion = 301,
            idEvento = DEMO_EVENT_ID,
            idUsuario = 4,
            nombre = "Beneficiario Demo",
            correo = "je.aquino@duouc.cl",
            estado = "INSCRITO",
            asistio = false,
        ),
        InscritoEntity(
            idInscripcion = 302,
            idEvento = DEMO_EVENT_ID,
            idUsuario = 5,
            nombre = "Ana Rojas (ficticia)",
            correo = "ana.rojas@demo.cl",
            estado = "INSCRITO",
            asistio = true,
            asistenciaRegistradoPor = "Voluntario Demo",
            asistenciaFecha = "2026-11-20T10:05:00",
        ),
        InscritoEntity(
            idInscripcion = 303,
            idEvento = DEMO_EVENT_ID,
            idUsuario = 6,
            nombre = "Bruno Silva (ficticio)",
            correo = "bruno.silva@demo.cl",
            estado = "INSCRITO",
            asistio = true,
            asistenciaRegistradoPor = "Voluntario Demo",
            asistenciaFecha = "2026-11-20T10:08:00",
        ),
        InscritoEntity(
            idInscripcion = 304,
            idEvento = DEMO_EVENT_ID,
            idUsuario = 7,
            nombre = "Carla Méndez (ficticia)",
            correo = "carla.mendez@demo.cl",
            estado = "INSCRITO",
            asistio = false,
        ),
        InscritoEntity(
            idInscripcion = 305,
            idEvento = 203,
            idUsuario = 4,
            nombre = "Beneficiario Demo",
            correo = "je.aquino@duouc.cl",
            estado = "INSCRITO",
            asistio = false,
        ),
    )
}
