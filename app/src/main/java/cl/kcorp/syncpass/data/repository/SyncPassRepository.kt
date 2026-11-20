package cl.kcorp.syncpass.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import cl.kcorp.syncpass.data.auth.Role
import cl.kcorp.syncpass.data.location.LocationValidator
import cl.kcorp.syncpass.data.local.InscripcionEntity
import cl.kcorp.syncpass.data.local.InscritoEntity
import cl.kcorp.syncpass.data.local.LocalSeed
import cl.kcorp.syncpass.data.local.PENDING_ASISTENCIA
import cl.kcorp.syncpass.data.local.PENDING_ENCUESTA
import cl.kcorp.syncpass.data.local.PendingActionEntity
import cl.kcorp.syncpass.data.local.ResponsableEntity
import cl.kcorp.syncpass.data.local.ResponsableHistorialEntity
import cl.kcorp.syncpass.data.local.SyncPassDao
import cl.kcorp.syncpass.data.local.SyncPassDatabase
import cl.kcorp.syncpass.data.local.toEntity
import cl.kcorp.syncpass.data.local.toItem
import cl.kcorp.syncpass.data.local.toModel
import cl.kcorp.syncpass.data.model.AccionResponsable
import cl.kcorp.syncpass.data.model.AsistenciaRef
import cl.kcorp.syncpass.data.model.CambioResponsable
import cl.kcorp.syncpass.data.model.ClaveResponsable
import cl.kcorp.syncpass.data.model.CoordenadasEvento
import cl.kcorp.syncpass.data.model.DocenteRef
import cl.kcorp.syncpass.data.model.Encuesta
import cl.kcorp.syncpass.data.model.Evento
import cl.kcorp.syncpass.data.model.FechaValidator
import cl.kcorp.syncpass.data.model.Inscripcion
import cl.kcorp.syncpass.data.model.Inscrito
import cl.kcorp.syncpass.data.model.MetricasEvento
import cl.kcorp.syncpass.data.model.Proyecto
import cl.kcorp.syncpass.data.model.ProyectoRef
import cl.kcorp.syncpass.data.model.Rol
import cl.kcorp.syncpass.data.model.RolResponsable
import cl.kcorp.syncpass.data.model.TipoEntidadResponsable
import cl.kcorp.syncpass.data.model.Usuario
import cl.kcorp.syncpass.data.model.UsuarioItem
import cl.kcorp.syncpass.data.model.calcularCambiosResponsables
import cl.kcorp.syncpass.data.remote.SupabaseClient
import cl.kcorp.syncpass.data.remote.SupabaseConfig
import cl.kcorp.syncpass.data.remote.SupabaseException
import cl.kcorp.syncpass.data.sync.SyncScheduler
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object SyncPassRepository {

    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private val syncMutex = Mutex()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var database: SyncPassDatabase? = null

    fun init(context: Context) {
        if (database != null) return
        synchronized(this) {
            if (database != null) return
            appContext = context.applicationContext
            val db = SyncPassDatabase.get(context)
            database = db
            // Datos ficticios para poder operar sin EC2/Supabase.
            // Solo se insertan si la base local está vacía.
            runBlocking { LocalSeed.seedIfEmpty(db.dao()) }
        }
    }

    private val dao: SyncPassDao? get() = database?.dao()

    private fun isOnline(): Boolean {
        val context = appContext ?: return true
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isRemoteAvailable(): Boolean = SupabaseConfig.isConfigured && isOnline()

    /**
     * Los registros creados sin conexión usan IDs negativos para no chocar con
     * los IDs positivos que entrega Supabase/Postgres.
     */
    private fun nextLocalId(minId: Int?): Int = (minId ?: 0).coerceAtMost(0) - 1

    private fun roleId(role: Role): Int = when (role) {
        Role.ADMINISTRADOR -> 1
        Role.DOCENTE -> 2
        Role.VOLUNTARIO -> 3
        Role.BENEFICIARIO -> 4
        Role.DESCONOCIDO -> 4
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun <T> parseList(array: JsonArray, clazz: Class<T>): List<T> =
        array.mapNotNull { runCatching { gson.fromJson(it, clazz) }.getOrNull() }

    private fun nowIso(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    private fun Throwable.isConnectivityError(): Boolean =
        this is IOException ||
            (this is SupabaseException && (code == null || status >= 500))

    /**
     * Reintenta una escritura quitando columnas que no existan en el esquema remoto.
     * Así el cliente sigue funcionando aunque la base tenga una versión anterior del DDL.
     */
    private suspend fun <T> retryWithoutUnknownColumns(body: JsonObject, block: suspend () -> T): T {
        var attempts = 0
        while (true) {
            try {
                return block()
            } catch (error: SupabaseException) {
                val missing = Regex("'([^']+)' column")
                    .find(error.message.orEmpty())
                    ?.groupValues
                    ?.getOrNull(1)
                if (error.status == 400 && attempts < 5 && missing != null && body.has(missing)) {
                    body.remove(missing)
                    attempts++
                    continue
                }
                throw error
            }
        }
    }

    // ---------------------------------------------------------------------
    // Caché local
    // ---------------------------------------------------------------------

    private suspend fun cacheProyectos(idCoordinador: Int, items: List<Proyecto>) {
        val d = dao ?: return
        if (items.isEmpty()) {
            d.clearProyectos(idCoordinador)
            return
        }
        d.upsertProyectos(items.map { it.toEntity() })
        d.deleteStaleProyectos(idCoordinador, items.map { it.idProyecto })
    }

    private suspend fun cacheProyecto(item: Proyecto) {
        dao?.upsertProyectos(listOf(item.toEntity()))
    }

    /** Adjunta docentes y voluntarios a cargo desde la tabla local de responsables. */
    private suspend fun withResponsables(proyectos: List<Proyecto>): List<Proyecto> {
        val d = dao ?: return proyectos
        val porProyecto = d.responsablesPorProyecto().groupBy { it.idProyecto }
        return proyectos.map { proyecto ->
            val filas = porProyecto[proyecto.idProyecto].orEmpty()
            proyecto.copy(
                docentes = filas
                    .filter { it.rol == RolResponsable.DOCENTE }
                    .map { DocenteRef(it.idUsuario, it.nombre, it.correo, it.rol) },
                voluntarios = filas
                    .filter { it.rol == RolResponsable.VOLUNTARIO }
                    .map { DocenteRef(it.idUsuario, it.nombre, it.correo, it.rol) },
            )
        }
    }

    /** Adjunta los responsables actuales a cada evento. */
    private suspend fun withResponsablesEventos(eventos: List<Evento>): List<Evento> {
        val d = dao ?: return eventos
        val porEvento = d.responsablesPorEvento().groupBy { it.idEvento }
        return eventos.map { evento ->
            evento.copy(
                responsables = porEvento[evento.idEvento].orEmpty().map { fila ->
                    DocenteRef(fila.idUsuario, fila.nombre, fila.correo, fila.rol)
                },
            )
        }
    }

    /**
     * Reemplaza los responsables actuales y registra en el historial solo las
     * altas y bajas efectivas.
     */
    private suspend fun guardarResponsables(
        tipoEntidad: String,
        idEntidad: Int,
        docentesIds: List<Int>,
        voluntariosIds: List<Int>,
        actor: String?,
    ) {
        val d = dao ?: return
        val actuales = d.responsables(tipoEntidad, idEntidad)
        val nuevos = (
            docentesIds.distinct().map { ClaveResponsable(RolResponsable.DOCENTE, it) } +
                voluntariosIds.distinct().map { ClaveResponsable(RolResponsable.VOLUNTARIO, it) }
            ).distinct()
        val cambios = calcularCambiosResponsables(
            actuales = actuales.map { ClaveResponsable(it.rol, it.idUsuario) },
            nuevos = nuevos,
        )

        d.clearResponsables(tipoEntidad, idEntidad)
        if (nuevos.isNotEmpty()) {
            val fecha = nowIso()
            d.upsertResponsables(
                nuevos.map { clave ->
                    ResponsableEntity(
                        tipoEntidad = tipoEntidad,
                        idEntidad = idEntidad,
                        idUsuario = clave.idUsuario,
                        rol = clave.rol,
                        asignadoEn = fecha,
                        asignadoPor = actor,
                    )
                },
            )
        }

        registrarHistorial(tipoEntidad, idEntidad, cambios.agregados, AccionResponsable.AGREGADO, actor)
        registrarHistorial(tipoEntidad, idEntidad, cambios.eliminados, AccionResponsable.ELIMINADO, actor)
    }

    private suspend fun registrarHistorial(
        tipoEntidad: String,
        idEntidad: Int,
        claves: Set<ClaveResponsable>,
        accion: String,
        actor: String?,
    ) {
        val d = dao ?: return
        val fecha = nowIso()
        claves.forEach { clave ->
            val usuario = d.usuarioById(clave.idUsuario)
            d.insertHistorialResponsable(
                ResponsableHistorialEntity(
                    tipoEntidad = tipoEntidad,
                    idEntidad = idEntidad,
                    idUsuario = clave.idUsuario,
                    nombreUsuario = usuario?.nombreMostrar?.ifBlank { usuario.correo }
                        ?: "Usuario ${clave.idUsuario}",
                    rol = clave.rol,
                    accion = accion,
                    fecha = fecha,
                    actor = actor,
                ),
            )
        }
    }

    private suspend fun cacheEventos(items: List<Evento>) {
        val d = dao ?: return
        if (items.isEmpty()) {
            d.clearEventos()
            return
        }
        d.upsertEventos(items.map { it.toEntity() })
        d.deleteStaleEventos(items.map { it.idEvento })
    }

    private suspend fun cacheEventosByProyecto(idProyecto: Int, items: List<Evento>) {
        val d = dao ?: return
        d.clearEventosByProyecto(idProyecto)
        if (items.isNotEmpty()) {
            d.upsertEventos(items.map { it.toEntity() })
        }
    }

    private suspend fun cacheEvento(item: Evento) {
        dao?.upsertEventos(listOf(item.toEntity()))
    }

    private suspend fun cacheInscripciones(idUsuario: Int, items: List<Inscripcion>) {
        val d = dao ?: return
        if (items.isEmpty()) {
            d.clearInscripciones(idUsuario)
            return
        }
        d.upsertInscripciones(items.map { it.toEntity() })
        d.deleteStaleInscripciones(idUsuario, items.map { it.idInscripcion })
    }

    private suspend fun cacheInscripcion(item: Inscripcion) {
        dao?.upsertInscripciones(listOf(item.toEntity()))
    }

    private suspend fun cacheInscritos(idEvento: Int, items: List<Inscrito>) {
        val d = dao ?: return
        if (items.isEmpty()) {
            d.clearInscritos(idEvento)
            return
        }
        d.upsertInscritos(items.map { it.toEntity() })
        d.deleteStaleInscritos(idEvento, items.map { it.idInscripcion })
    }

    private suspend fun cacheUsuarios(items: List<UsuarioItem>) {
        val d = dao ?: return
        if (items.isEmpty()) {
            d.clearUsuarios()
            return
        }
        d.upsertUsuarios(items.map { it.toEntity() })
        d.deleteStaleUsuarios(items.map { it.idUsuario })
    }

    private suspend fun cacheUsuario(usuario: Usuario, rolNombre: String?) {
        dao?.upsertUsuario(usuario.toEntity(rolNombre))
    }

    private suspend fun markAsistenciaLocal(
        idInscripcion: Int,
        estadoValidacion: String,
        registradoPor: String?,
        fecha: String,
    ) {
        val d = dao ?: return
        d.markAsistencia(idInscripcion, estadoValidacion, registradoPor, fecha)
        d.markInscritoAsistio(idInscripcion, registradoPor, fecha)
    }

    private suspend fun saveEncuestaLocal(
        idInscripcion: Int,
        calificacion: Int,
        utilidad: String,
        comentarios: String?,
    ) {
        dao?.saveEncuestaLocal(idInscripcion, calificacion, utilidad, comentarios)
    }

    // ---------------------------------------------------------------------
    // Usuarios
    // ---------------------------------------------------------------------

    suspend fun getOrCreateUsuario(email: String, nombre: String, role: Role): Usuario? {
        if (email.isBlank()) return null
        val cached = dao?.usuarioByCorreo(email)?.toModel()
        if (!isRemoteAvailable()) {
            return cached ?: ensureLocalUsuario(email, nombre, role)
        }

        val remote = runCatching {
            val found = SupabaseClient.select("usuario", "select=*&correo=eq.${enc(email)}")
            val existing = found.firstOrNull()?.let { gson.fromJson(it, Usuario::class.java) }
            if (existing != null) {
                cacheUsuario(existing, role.label)
                return@runCatching existing
            }

            val bdRoleName = when (role) {
                Role.ADMINISTRADOR -> "Administrador"
                Role.DOCENTE -> "Docente"
                Role.VOLUNTARIO -> "Voluntario"
                Role.BENEFICIARIO -> "Beneficiario"
                Role.DESCONOCIDO -> "Beneficiario"
            }
            val rol = SupabaseClient.select("rol", "select=id_rol&nombre=eq.${enc(bdRoleName)}")
                .firstOrNull()
                ?.asJsonObject
                ?.get("id_rol")
                ?.asInt
                ?: 1

            val body = JsonObject().apply {
                addProperty("id_rol", rol)
                addProperty("nombre_usuario", email.substringBefore("@") + "_" + (System.currentTimeMillis() % 10_000))
                addProperty("nombre_mostrar", nombre.ifBlank { email })
                addProperty("correo", email)
                addProperty("estado", "ACTIVO")
            }
            val created = retryWithoutUnknownColumns(body) {
                SupabaseClient.insert("usuario", body)
            }
            created.firstOrNull()
                ?.let { gson.fromJson(it, Usuario::class.java) }
                ?.also { cacheUsuario(it, bdRoleName) }
        }.getOrNull()

        return remote ?: cached ?: ensureLocalUsuario(email, nombre, role)
    }

    /**
     * Crea una identidad local cuando Supabase no está disponible. Así el login
     * (Cognito o modo demo) siempre puede usar los paneles y el modo offline.
     */
    private suspend fun ensureLocalUsuario(email: String, nombre: String, role: Role): Usuario? {
        val d = dao ?: return null
        d.usuarioByCorreo(email)?.let { return it.toModel() }

        val usuario = Usuario(
            idUsuario = nextLocalId(d.minUsuarioId()),
            idRol = roleId(role),
            nombreUsuario = email.substringBefore("@").ifBlank { "usuario_local" },
            nombreMostrar = nombre.ifBlank { email },
            correo = email,
            estado = "ACTIVO",
        )
        d.upsertUsuario(usuario.toEntity(role.label))

        if (role == Role.BENEFICIARIO) ensureDemoEnrollment(usuario)
        return usuario
    }

    /**
     * Deja al beneficiario local inscrito en el primer evento disponible para
     * que el flujo QR + encuesta sea demostrable sin depender de Supabase.
     */
    private suspend fun ensureDemoEnrollment(usuario: Usuario) {
        val d = dao ?: return
        val evento = d.eventos().firstOrNull() ?: return
        if (d.inscripcionByUsuarioEvento(usuario.idUsuario, evento.idEvento) != null) return

        val idInscripcion = nextLocalId(d.minInscripcionId())
        val fecha = nowIso()
        d.upsertInscripciones(
            listOf(
                InscripcionEntity(
                    idInscripcion = idInscripcion,
                    idUsuario = usuario.idUsuario,
                    idEvento = evento.idEvento,
                    estado = "INSCRITO",
                    fechaInscripcion = fecha,
                    asistenciaEstado = null,
                    encuestaCalificacion = null,
                    encuestaUtilidad = null,
                    encuestaComentarios = null,
                ),
            ),
        )
        d.upsertInscritos(
            listOf(
                InscritoEntity(
                    idInscripcion = idInscripcion,
                    idEvento = evento.idEvento,
                    idUsuario = usuario.idUsuario,
                    nombre = usuario.nombreMostrar,
                    correo = usuario.correo,
                    estado = "INSCRITO",
                    asistio = false,
                ),
            ),
        )
    }

    suspend fun getUsuarios(): List<UsuarioItem> {
        val cached = dao?.usuarios()?.map { it.toItem() } ?: emptyList()
        if (!SupabaseConfig.isConfigured || !isOnline()) return cached

        return runCatching {
            val data = SupabaseClient.select(
                "usuario",
                "select=id_usuario,nombre_usuario,nombre_mostrar,correo,id_rol,estado,rol:rol(nombre)&order=nombre_mostrar.asc",
            )
            val items = data.mapNotNull { element ->
                val obj = element.asJsonObject
                val rol = when (val rolEl = obj.get("rol")) {
                    is JsonObject -> rolEl.get("nombre")?.asString.orEmpty()
                    is JsonArray -> rolEl.firstOrNull()?.asJsonObject?.get("nombre")?.asString.orEmpty()
                    else -> ""
                }
                UsuarioItem(
                    idUsuario = obj.get("id_usuario")?.asInt ?: 0,
                    nombreUsuario = obj.get("nombre_usuario")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
                    nombreMostrar = obj.get("nombre_mostrar")?.asString.orEmpty(),
                    correo = obj.get("correo")?.asString.orEmpty(),
                    idRol = obj.get("id_rol")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
                    rol = rol,
                    estado = obj.get("estado")?.asString.orEmpty(),
                )
            }
            cacheUsuarios(items)
            items
        }.getOrElse { cached }
    }

    suspend fun getRoles(): List<Rol> {
        val local = listOf(
            Rol(1, "Administrador", "Gestión completa del sistema"),
            Rol(2, "Docente", "Coordinación de proyectos y eventos"),
            Rol(3, "Voluntario", "Acreditación de asistentes en terreno"),
            Rol(4, "Beneficiario", "Participación en eventos y encuestas"),
        )
        if (!isRemoteAvailable()) return local
        return runCatching {
            val data = SupabaseClient.select("rol", "select=id_rol,nombre,descripcion&order=id_rol.asc")
            parseList(data, Rol::class.java)
        }.getOrElse { local }
    }

    /** Docentes disponibles para asignarlos a cargo de uno o varios proyectos. */
    suspend fun getDocentes(): List<DocenteRef> =
        getUsuarios()
            .filter { it.idRol == 2 || it.rol.equals("Docente", ignoreCase = true) }
            .map {
                DocenteRef(
                    idDocente = it.idUsuario,
                    nombre = it.nombreMostrar.ifBlank { it.nombreUsuario },
                    correo = it.correo,
                    rol = RolResponsable.DOCENTE,
                )
            }
            .distinctBy { it.idDocente }

    /** Voluntarios disponibles para asignarlos a proyectos y eventos. */
    suspend fun getVoluntarios(): List<DocenteRef> =
        getUsuarios()
            .filter { it.idRol == 3 || it.rol.equals("Voluntario", ignoreCase = true) }
            .map {
                DocenteRef(
                    idDocente = it.idUsuario,
                    nombre = it.nombreMostrar.ifBlank { it.nombreUsuario },
                    correo = it.correo,
                    rol = RolResponsable.VOLUNTARIO,
                )
            }
            .distinctBy { it.idDocente }

    suspend fun getHistorialResponsables(
        tipoEntidad: String,
        idEntidad: Int,
    ): List<CambioResponsable> =
        dao?.historialResponsables(tipoEntidad, idEntidad).orEmpty().map {
            CambioResponsable(
                id = it.id,
                tipoEntidad = it.tipoEntidad,
                idEntidad = it.idEntidad,
                idUsuario = it.idUsuario,
                nombreUsuario = it.nombreUsuario,
                rol = it.rol,
                accion = it.accion,
                fecha = it.fecha,
                actor = it.actor,
            )
        }

    suspend fun createUsuario(
        idRol: Int,
        nombreUsuario: String,
        nombreMostrar: String,
        correo: String,
        estado: String = "ACTIVO",
        rolNombre: String? = null,
    ): Boolean {
        if (!isRemoteAvailable()) {
            val item = Usuario(
                idUsuario = nextLocalId(dao?.minUsuarioId()),
                idRol = idRol,
                nombreUsuario = nombreUsuario.ifBlank { correo.substringBefore("@") },
                nombreMostrar = nombreMostrar.ifBlank { correo },
                correo = correo,
                estado = estado,
            )
            cacheUsuario(item, rolNombre)
            return true
        }
        return runCatching {
            val body = JsonObject().apply {
                addProperty("id_rol", idRol)
                addProperty("nombre_usuario", nombreUsuario)
                addProperty("nombre_mostrar", nombreMostrar)
                addProperty("correo", correo)
                addProperty("estado", estado)
            }
            val created = retryWithoutUnknownColumns(body) {
                SupabaseClient.insert("usuario", body)
            }.firstOrNull()?.let { gson.fromJson(it, Usuario::class.java) }
            if (created != null) {
                cacheUsuario(created, rolNombre)
                true
            } else {
                false
            }
        }.getOrElse { false }
    }

    suspend fun updateUsuario(
        idUsuario: Int,
        idRol: Int,
        nombreUsuario: String,
        nombreMostrar: String,
        correo: String,
        estado: String,
        rolNombre: String? = null,
    ): Boolean {
        if (!isRemoteAvailable()) {
            val current = dao?.usuarioById(idUsuario) ?: return false
            dao?.upsertUsuario(
                current.copy(
                    idRol = idRol,
                    nombreUsuario = nombreUsuario,
                    nombreMostrar = nombreMostrar,
                    correo = correo,
                    estado = estado,
                    rolNombre = rolNombre ?: current.rolNombre,
                ),
            )
            return true
        }
        return runCatching {
            val body = JsonObject().apply {
                addProperty("id_rol", idRol)
                addProperty("nombre_usuario", nombreUsuario)
                addProperty("nombre_mostrar", nombreMostrar)
                addProperty("correo", correo)
                addProperty("estado", estado)
            }
            val updated = retryWithoutUnknownColumns(body) {
                SupabaseClient.update("usuario", "id_usuario=eq.$idUsuario", body)
            }.firstOrNull()?.let { gson.fromJson(it, Usuario::class.java) }
            if (updated != null) {
                cacheUsuario(updated, rolNombre)
                true
            } else {
                false
            }
        }.getOrElse { false }
    }

    suspend fun deleteUsuario(idUsuario: Int): Boolean {
        if (!isRemoteAvailable()) {
            dao?.deleteResponsablesByUsuario(idUsuario)
            dao?.deleteUsuario(idUsuario)
            return true
        }
        return runCatching {
            SupabaseClient.delete("usuario", "id_usuario=eq.$idUsuario")
            dao?.deleteResponsablesByUsuario(idUsuario)
            dao?.deleteUsuario(idUsuario)
            true
        }.getOrElse { false }
    }

    // ---------------------------------------------------------------------
    // Proyectos
    // ---------------------------------------------------------------------

    suspend fun getProyectosByCoordinador(idUsuario: Int): List<Proyecto> {
        val cached = dao?.proyectosByCoordinador(idUsuario)?.map { it.toModel() } ?: emptyList()
        if (!isRemoteAvailable()) {
            // En modo local cualquier docente puede ver el catálogo demo.
            val local = cached.ifEmpty { dao?.proyectos()?.map { it.toModel() } ?: emptyList() }
            return withResponsables(local)
        }

        return runCatching {
            val data = SupabaseClient.select(
                "proyecto",
                "select=*&id_coordinador=eq.$idUsuario&order=fecha_inicio.desc",
            )
            val items = parseList(data, Proyecto::class.java)
            cacheProyectos(idUsuario, items)
            withResponsables(items)
        }.getOrElse { withResponsables(cached) }
    }

    suspend fun createProyecto(
        idCoordinador: Int,
        titulo: String,
        descripcion: String?,
        fechaInicio: String,
        fechaTermino: String,
        estado: String = "PLANIFICADO",
        docentesIds: List<Int> = emptyList(),
        voluntariosIds: List<Int> = emptyList(),
        creadoPor: String? = null,
        actor: String? = null,
    ): Proyecto? {
        if (!FechaValidator.esRangoValido(fechaInicio, fechaTermino)) return null
        val asignados = docentesIds.ifEmpty { listOf(idCoordinador) }.distinct()
        val voluntariosAsignados = voluntariosIds.distinct()
        val docentes = personasLocal(asignados, RolResponsable.DOCENTE)
        val voluntarios = personasLocal(voluntariosAsignados, RolResponsable.VOLUNTARIO)
        val creadoEn = nowIso()

        if (!isRemoteAvailable()) {
            val item = Proyecto(
                idProyecto = nextLocalId(dao?.minProyectoId()),
                idCoordinador = idCoordinador,
                idUnidad = 1,
                idArea = 1,
                titulo = titulo,
                descripcion = descripcion,
                fechaInicio = fechaInicio,
                fechaTermino = fechaTermino,
                estado = estado,
                docentes = docentes,
                voluntarios = voluntarios,
                creadoPor = creadoPor,
                creadoEn = creadoEn,
            )
            cacheProyecto(item)
            guardarResponsables(
                TipoEntidadResponsable.PROYECTO,
                item.idProyecto,
                asignados,
                voluntariosAsignados,
                actor ?: creadoPor,
            )
            return item
        }
        return runCatching {
            val body = JsonObject().apply {
                addProperty("id_coordinador", asignados.first())
                addProperty("id_unidad", 1)
                addProperty("id_area", 1)
                addProperty("titulo", titulo)
                if (descripcion.isNullOrBlank()) add("descripcion", JsonNull.INSTANCE) else addProperty("descripcion", descripcion)
                addProperty("fecha_inicio", fechaInicio)
                addProperty("fecha_termino", fechaTermino)
                addProperty("estado", estado)
                if (!creadoPor.isNullOrBlank()) addProperty("creado_por", creadoPor)
                addProperty("creado_en", creadoEn)
            }
            val creado = retryWithoutUnknownColumns(body) {
                SupabaseClient.insert("proyecto", body)
            }.firstOrNull()
                ?.let { gson.fromJson(it, Proyecto::class.java) }
                ?.copy(
                    docentes = docentes,
                    voluntarios = voluntarios,
                    creadoPor = creadoPor,
                    creadoEn = creadoEn,
                )
            if (creado != null) {
                cacheProyecto(creado)
                guardarResponsables(
                    TipoEntidadResponsable.PROYECTO,
                    creado.idProyecto,
                    asignados,
                    voluntariosAsignados,
                    actor ?: creadoPor,
                )
            }
            creado
        }.getOrNull()
    }

    /** Resuelve personas desde la caché local de usuarios y les asigna un rol. */
    private suspend fun personasLocal(ids: List<Int>, rol: String): List<DocenteRef> {
        val d = dao ?: return emptyList()
        return ids.distinct().mapNotNull { id ->
            d.usuarioById(id)?.let {
                DocenteRef(it.idUsuario, it.nombreMostrar, it.correo, rol)
            }
        }
    }

    suspend fun updateProyecto(
        idProyecto: Int,
        titulo: String,
        descripcion: String?,
        fechaInicio: String,
        fechaTermino: String,
        estado: String,
        docentesIds: List<Int> = emptyList(),
        voluntariosIds: List<Int> = emptyList(),
        actor: String? = null,
    ): Proyecto? {
        if (!FechaValidator.esRangoValido(fechaInicio, fechaTermino)) return null
        val docentes = personasLocal(docentesIds, RolResponsable.DOCENTE)
        val voluntarios = personasLocal(voluntariosIds, RolResponsable.VOLUNTARIO)

        if (!isRemoteAvailable()) {
            val current = dao?.proyectoById(idProyecto)?.toModel() ?: return null
            val updated = current.copy(
                titulo = titulo,
                descripcion = descripcion,
                fechaInicio = fechaInicio,
                fechaTermino = fechaTermino,
                estado = estado,
                docentes = docentes,
                voluntarios = voluntarios,
            )
            cacheProyecto(updated)
            guardarResponsables(
                TipoEntidadResponsable.PROYECTO,
                idProyecto,
                docentesIds,
                voluntariosIds,
                actor,
            )
            return updated
        }
        return runCatching {
            val body = JsonObject().apply {
                addProperty("titulo", titulo)
                if (descripcion.isNullOrBlank()) add("descripcion", JsonNull.INSTANCE) else addProperty("descripcion", descripcion)
                addProperty("fecha_inicio", fechaInicio)
                addProperty("fecha_termino", fechaTermino)
                addProperty("estado", estado)
                docentesIds.firstOrNull()?.let { addProperty("id_coordinador", it) }
            }
            val actualizado = retryWithoutUnknownColumns(body) {
                SupabaseClient.update("proyecto", "id_proyecto=eq.$idProyecto", body)
            }.firstOrNull()
                ?.let { gson.fromJson(it, Proyecto::class.java) }
                ?.copy(docentes = docentes, voluntarios = voluntarios)
            if (actualizado != null) {
                cacheProyecto(actualizado)
                guardarResponsables(
                    TipoEntidadResponsable.PROYECTO,
                    idProyecto,
                    docentesIds,
                    voluntariosIds,
                    actor,
                )
            }
            actualizado
        }.getOrNull()
    }

    suspend fun deleteProyecto(idProyecto: Int): Boolean {
        suspend fun limpiarLocal() {
            val d = dao ?: return
            d.clearHistorialEventosByProyecto(idProyecto)
            d.clearResponsablesEventosByProyecto(idProyecto)
            d.clearEventosByProyecto(idProyecto)
            d.clearHistorialResponsables(TipoEntidadResponsable.PROYECTO, idProyecto)
            d.clearResponsables(TipoEntidadResponsable.PROYECTO, idProyecto)
            d.deleteProyecto(idProyecto)
        }
        if (!isRemoteAvailable()) {
            limpiarLocal()
            return true
        }
        return runCatching {
            SupabaseClient.delete("evento", "id_proyecto=eq.$idProyecto")
            SupabaseClient.delete("proyecto", "id_proyecto=eq.$idProyecto")
            limpiarLocal()
            true
        }.getOrElse { false }
    }

    // ---------------------------------------------------------------------
    // Eventos
    // ---------------------------------------------------------------------

    suspend fun getEventos(): List<Evento> {
        val cached = dao?.eventos()?.map { it.toModel() } ?: emptyList()
        if (!SupabaseConfig.isConfigured || !isOnline()) return withResponsablesEventos(cached)

        return runCatching {
            val data = SupabaseClient.select(
                "evento",
                "select=*,proyecto:proyecto(titulo)&order=fecha_inicio.asc",
            )
            val items = parseList(data, Evento::class.java)
            cacheEventos(items)
            withResponsablesEventos(items)
        }.getOrElse { withResponsablesEventos(cached) }
    }

    suspend fun getEventosByProyecto(idProyecto: Int): List<Evento> {
        val cached = dao?.eventosByProyecto(idProyecto)?.map { it.toModel() } ?: emptyList()
        if (!SupabaseConfig.isConfigured || !isOnline()) return withResponsablesEventos(cached)

        return runCatching {
            val data = SupabaseClient.select(
                "evento",
                "select=*&id_proyecto=eq.$idProyecto&order=fecha_inicio.asc",
            )
            val items = parseList(data, Evento::class.java)
            cacheEventosByProyecto(idProyecto, items)
            withResponsablesEventos(items)
        }.getOrElse { withResponsablesEventos(cached) }
    }

    suspend fun createEvento(
        idProyecto: Int,
        nombre: String,
        descripcion: String?,
        fechaInicio: String,
        fechaTermino: String,
        cuposMaximos: Int,
        recinto: String?,
        direccion: String? = null,
        region: String? = null,
        comuna: String? = null,
        latitud: Double? = null,
        longitud: Double? = null,
        radioValidacionMetros: Int = 100,
        estado: String = "PROGRAMADO",
        docentesIds: List<Int> = emptyList(),
        voluntariosIds: List<Int> = emptyList(),
        creadoPor: String? = null,
        actor: String? = null,
    ): Evento? {
        if (!FechaValidator.esRangoValido(fechaInicio, fechaTermino)) return null
        val creadoEn = nowIso()
        val radio = LocationValidator.normalizarRadio(radioValidacionMetros)
        val responsables = personasLocal(docentesIds, RolResponsable.DOCENTE) +
            personasLocal(voluntariosIds, RolResponsable.VOLUNTARIO)
        val proyecto = dao?.proyectoById(idProyecto)
        if (proyecto != null &&
            !FechaValidator.rangoDentroDePeriodo(
                periodoInicio = proyecto.fechaInicio,
                periodoTermino = proyecto.fechaTermino,
                fechaInicio = fechaInicio,
                fechaTermino = fechaTermino,
            )
        ) {
            return null
        }

        if (!isRemoteAvailable()) {
            val item = Evento(
                idEvento = nextLocalId(dao?.minEventoId()),
                idProyecto = idProyecto,
                nombre = nombre,
                descripcion = descripcion,
                fechaInicio = fechaInicio,
                fechaTermino = fechaTermino,
                cuposMaximos = cuposMaximos,
                recinto = recinto,
                direccion = direccion,
                region = region,
                comuna = comuna,
                latitud = latitud,
                longitud = longitud,
                radioValidacionMetros = radio,
                estado = estado,
                proyecto = proyecto?.let { ProyectoRef(it.titulo) },
                responsables = responsables,
                creadoPor = creadoPor,
                creadoEn = creadoEn,
            )
            cacheEvento(item)
            guardarResponsables(
                TipoEntidadResponsable.EVENTO,
                item.idEvento,
                docentesIds,
                voluntariosIds,
                actor ?: creadoPor,
            )
            return item
        }
        return runCatching {
            val body = JsonObject().apply {
                addProperty("id_proyecto", idProyecto)
                addProperty("nombre", nombre)
                if (descripcion.isNullOrBlank()) add("descripcion", JsonNull.INSTANCE) else addProperty("descripcion", descripcion)
                addProperty("fecha_inicio", fechaInicio)
                addProperty("fecha_termino", fechaTermino)
                addProperty("cupos_maximos", cuposMaximos)
                if (recinto.isNullOrBlank()) add("recinto", JsonNull.INSTANCE) else addProperty("recinto", recinto)
                if (direccion.isNullOrBlank()) add("direccion", JsonNull.INSTANCE) else addProperty("direccion", direccion)
                if (region.isNullOrBlank()) add("region", JsonNull.INSTANCE) else addProperty("region", region)
                if (comuna.isNullOrBlank()) add("comuna", JsonNull.INSTANCE) else addProperty("comuna", comuna)
                if (latitud == null) add("latitud", JsonNull.INSTANCE) else addProperty("latitud", latitud)
                if (longitud == null) add("longitud", JsonNull.INSTANCE) else addProperty("longitud", longitud)
                addProperty("radio_validacion_metros", radio)
                addProperty("estado", estado)
                if (!creadoPor.isNullOrBlank()) addProperty("creado_por", creadoPor)
                addProperty("creado_en", creadoEn)
            }
            val creado = retryWithoutUnknownColumns(body) {
                SupabaseClient.insert("evento", body)
            }.firstOrNull()
                ?.let { gson.fromJson(it, Evento::class.java) }
                ?.let { evento ->
                    evento.copy(
                        responsables = responsables,
                        creadoPor = evento.creadoPor ?: creadoPor,
                        creadoEn = evento.creadoEn ?: creadoEn,
                    )
                }
            if (creado != null) {
                cacheEvento(creado)
                guardarResponsables(
                    TipoEntidadResponsable.EVENTO,
                    creado.idEvento,
                    docentesIds,
                    voluntariosIds,
                    actor ?: creadoPor,
                )
            }
            creado
        }.getOrNull()
    }

    suspend fun updateEvento(
        idEvento: Int,
        nombre: String,
        descripcion: String?,
        fechaInicio: String,
        fechaTermino: String,
        cuposMaximos: Int,
        recinto: String?,
        direccion: String? = null,
        region: String? = null,
        comuna: String? = null,
        latitud: Double? = null,
        longitud: Double? = null,
        radioValidacionMetros: Int = 100,
        estado: String,
        docentesIds: List<Int> = emptyList(),
        voluntariosIds: List<Int> = emptyList(),
        actor: String? = null,
    ): Evento? {
        if (!FechaValidator.esRangoValido(fechaInicio, fechaTermino)) return null
        val radio = LocationValidator.normalizarRadio(radioValidacionMetros)
        val responsables = personasLocal(docentesIds, RolResponsable.DOCENTE) +
            personasLocal(voluntariosIds, RolResponsable.VOLUNTARIO)
        val currentEvento = dao?.eventos()?.firstOrNull { it.idEvento == idEvento }
        val proyecto = currentEvento?.let { dao?.proyectoById(it.idProyecto) }
        if (proyecto != null &&
            !FechaValidator.rangoDentroDePeriodo(
                periodoInicio = proyecto.fechaInicio,
                periodoTermino = proyecto.fechaTermino,
                fechaInicio = fechaInicio,
                fechaTermino = fechaTermino,
            )
        ) {
            return null
        }

        if (!isRemoteAvailable()) {
            val current = currentEvento ?: return null
            val updated = current.copy(
                nombre = nombre,
                descripcion = descripcion,
                fechaInicio = fechaInicio,
                fechaTermino = fechaTermino,
                cuposMaximos = cuposMaximos,
                recinto = recinto,
                direccion = direccion,
                region = region,
                comuna = comuna,
                latitud = latitud,
                longitud = longitud,
                radioValidacionMetros = radio,
                estado = estado,
            )
            dao?.upsertEventos(listOf(updated))
            guardarResponsables(
                TipoEntidadResponsable.EVENTO,
                idEvento,
                docentesIds,
                voluntariosIds,
                actor,
            )
            return updated.toModel().copy(responsables = responsables)
        }
        return runCatching {
            val body = JsonObject().apply {
                addProperty("nombre", nombre)
                if (descripcion.isNullOrBlank()) add("descripcion", JsonNull.INSTANCE) else addProperty("descripcion", descripcion)
                addProperty("fecha_inicio", fechaInicio)
                addProperty("fecha_termino", fechaTermino)
                addProperty("cupos_maximos", cuposMaximos)
                if (recinto.isNullOrBlank()) add("recinto", JsonNull.INSTANCE) else addProperty("recinto", recinto)
                if (direccion.isNullOrBlank()) add("direccion", JsonNull.INSTANCE) else addProperty("direccion", direccion)
                if (region.isNullOrBlank()) add("region", JsonNull.INSTANCE) else addProperty("region", region)
                if (comuna.isNullOrBlank()) add("comuna", JsonNull.INSTANCE) else addProperty("comuna", comuna)
                if (latitud == null) add("latitud", JsonNull.INSTANCE) else addProperty("latitud", latitud)
                if (longitud == null) add("longitud", JsonNull.INSTANCE) else addProperty("longitud", longitud)
                addProperty("radio_validacion_metros", radio)
                addProperty("estado", estado)
            }
            val actualizado = retryWithoutUnknownColumns(body) {
                SupabaseClient.update("evento", "id_evento=eq.$idEvento", body)
            }.firstOrNull()
                ?.let { gson.fromJson(it, Evento::class.java) }
                ?.copy(responsables = responsables)
            if (actualizado != null) {
                cacheEvento(actualizado)
                guardarResponsables(
                    TipoEntidadResponsable.EVENTO,
                    idEvento,
                    docentesIds,
                    voluntariosIds,
                    actor,
                )
            }
            actualizado
        }.getOrNull()
    }

    /**
     * Guarda o quita el punto de referencia GPS de un evento.
     *
     * Se separa de [updateEvento] porque configurar la ubicación no requiere
     * revalidar fechas, cupos ni responsables: el evento puede existir desde
     * antes y el docente configura el GPS cuando está en el recinto o cuando
     * consigue un enlace con las coordenadas.
     *
     * `latitud` y `longitud` deben venir ambos o ninguno; pasar `null` en ambos
     * desactiva la validación GPS y deja las asistencias como `SIN_VALIDACION`.
     */
    suspend fun updateEventoUbicacion(
        idEvento: Int,
        latitud: Double?,
        longitud: Double?,
        radioValidacionMetros: Int,
    ): Boolean {
        val parCompleto = (latitud == null) == (longitud == null)
        if (!parCompleto) return false
        if (latitud != null && longitud != null &&
            !CoordenadasEvento.esParValido(latitud, longitud)
        ) {
            return false
        }

        val radio = LocationValidator.normalizarRadio(radioValidacionMetros)
        val current = dao?.eventos()?.firstOrNull { it.idEvento == idEvento } ?: return false
        val updated = current.copy(
            latitud = latitud,
            longitud = longitud,
            radioValidacionMetros = radio,
        )
        dao?.upsertEventos(listOf(updated))

        if (!isRemoteAvailable()) return true

        return runCatching {
            val body = JsonObject().apply {
                if (latitud == null) add("latitud", JsonNull.INSTANCE) else addProperty("latitud", latitud)
                if (longitud == null) add("longitud", JsonNull.INSTANCE) else addProperty("longitud", longitud)
                addProperty("radio_validacion_metros", radio)
            }
            SupabaseClient.update("evento", "id_evento=eq.$idEvento", body)
            true
        }.getOrElse {
            // La operación ya quedó guardada en Room; se mantiene el modo
            // offline-first y la sincronización remota se reintenta más adelante.
            true
        }
    }

    suspend fun deleteEvento(idEvento: Int): Boolean {
        suspend fun limpiarLocal() {
            dao?.clearHistorialResponsables(TipoEntidadResponsable.EVENTO, idEvento)
            dao?.clearResponsables(TipoEntidadResponsable.EVENTO, idEvento)
            dao?.deleteEvento(idEvento)
        }
        if (!isRemoteAvailable()) {
            limpiarLocal()
            return true
        }
        return runCatching {
            SupabaseClient.delete("evento", "id_evento=eq.$idEvento")
            limpiarLocal()
            true
        }.getOrElse { false }
    }

    // ---------------------------------------------------------------------
    // Inscripciones
    // ---------------------------------------------------------------------

    suspend fun getInscripciones(idUsuario: Int): List<Inscripcion> {
        val cached = dao?.inscripcionesByUsuario(idUsuario)?.map { it.toModel() } ?: emptyList()
        if (!SupabaseConfig.isConfigured || !isOnline()) return cached

        return runCatching {
            val data = SupabaseClient.select(
                "inscripcion",
                "select=*,asistencia:asistencia(*),encuesta:encuesta(calificacion,utilidad,comentarios)&id_usuario=eq.$idUsuario",
            )
            val items = data.mapNotNull { parseInscripcion(it.asJsonObject) }
            cacheInscripciones(idUsuario, items)
            items
        }.getOrElse { cached }
    }

    private fun parseInscripcion(obj: JsonObject): Inscripcion? {
        val id = obj.get("id_inscripcion")?.takeIf { !it.isJsonNull }?.asInt ?: return null
        val asistencia = parseAsistencia(obj)
        val encuesta = when (val el = obj.get("encuesta")) {
            is JsonObject -> parseEncuesta(el)
            is JsonArray -> el.firstOrNull()?.asJsonObject?.let { parseEncuesta(it) }
            else -> null
        }
        return Inscripcion(
            idInscripcion = id,
            idUsuario = obj.get("id_usuario")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
            idEvento = obj.get("id_evento")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
            estado = obj.get("estado")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
            fechaInscripcion = obj.get("fecha_inscripcion")?.takeIf { !it.isJsonNull }?.asString,
            asistencia = asistencia,
            encuesta = encuesta,
        )
    }

    private fun parseAsistencia(obj: JsonObject): AsistenciaRef? {
        val el = when (val raw = obj.get("asistencia")) {
            is JsonObject -> raw
            is JsonArray -> raw.firstOrNull { it.isJsonObject }?.asJsonObject
            else -> null
        } ?: return null
        val estado = el.get("estado_validacion")?.takeIf { !it.isJsonNull }?.asString ?: return null
        return AsistenciaRef(
            estadoValidacion = estado,
            registradoPor = el.get("registrado_por")?.takeIf { !it.isJsonNull }?.asString,
            fechaMarcacion = el.get("fecha_marcacion")?.takeIf { !it.isJsonNull }?.asString,
        )
    }

    private fun parseEncuesta(obj: JsonObject): Encuesta? {
        val calificacion = obj.get("calificacion")?.takeIf { !it.isJsonNull }?.asInt ?: return null
        return Encuesta(
            calificacion = calificacion,
            utilidad = obj.get("utilidad")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
            comentarios = obj.get("comentarios")?.takeIf { !it.isJsonNull }?.asString,
        )
    }

    suspend fun inscribirse(idUsuario: Int, idEvento: Int): Inscripcion? {
        if (!isRemoteAvailable()) return inscribirseLocal(idUsuario, idEvento)
        return runCatching {
            val body = JsonObject().apply {
                addProperty("id_usuario", idUsuario)
                addProperty("id_evento", idEvento)
                addProperty("estado", "INSCRITO")
            }
            retryWithoutUnknownColumns(body) {
                SupabaseClient.upsert("inscripcion", body, "id_usuario,id_evento")
            }.firstOrNull()?.asJsonObject?.let { parseInscripcion(it) }
                ?.also { cacheInscripcion(it) }
        }.getOrNull() ?: inscribirseLocal(idUsuario, idEvento)
    }

    private suspend fun inscribirseLocal(idUsuario: Int, idEvento: Int): Inscripcion? {
        val d = dao ?: return null
        val existente = d.inscripcionByUsuarioEvento(idUsuario, idEvento)
        if (existente != null) {
            val reactivada = existente.copy(estado = "INSCRITO")
            d.upsertInscripciones(listOf(reactivada))
            val usuario = d.usuarioById(idUsuario)
            if (usuario != null) {
                d.upsertInscritos(
                    listOf(
                        InscritoEntity(
                            idInscripcion = reactivada.idInscripcion,
                            idEvento = idEvento,
                            idUsuario = idUsuario,
                            nombre = usuario.nombreMostrar,
                            correo = usuario.correo,
                            estado = "INSCRITO",
                            asistio = reactivada.asistenciaEstado == "VALIDADA",
                        ),
                    ),
                )
            }
            return reactivada.toModel()
        }

        val idInscripcion = nextLocalId(d.minInscripcionId())
        val inscripcion = InscripcionEntity(
            idInscripcion = idInscripcion,
            idUsuario = idUsuario,
            idEvento = idEvento,
            estado = "INSCRITO",
            fechaInscripcion = nowIso(),
            asistenciaEstado = null,
            encuestaCalificacion = null,
            encuestaUtilidad = null,
            encuestaComentarios = null,
        )
        d.upsertInscripciones(listOf(inscripcion))

        val usuario = d.usuarioById(idUsuario)
        d.upsertInscritos(
            listOf(
                InscritoEntity(
                    idInscripcion = idInscripcion,
                    idEvento = idEvento,
                    idUsuario = idUsuario,
                    nombre = usuario?.nombreMostrar ?: "Usuario local",
                    correo = usuario?.correo ?: "",
                    estado = "INSCRITO",
                    asistio = false,
                ),
            ),
        )
        return inscripcion.toModel()
    }

    suspend fun desinscribirse(idUsuario: Int, idEvento: Int): Boolean {
        if (!isRemoteAvailable()) {
            dao?.markInscripcionCancelada(idUsuario, idEvento)
            dao?.inscripcionByUsuarioEvento(idUsuario, idEvento)?.let {
                dao?.deleteInscrito(it.idInscripcion)
            }
            return true
        }
        return runCatching {
            val body = JsonObject().apply { addProperty("estado", "CANCELADO") }
            retryWithoutUnknownColumns(body) {
                SupabaseClient.update(
                    "inscripcion",
                    "id_usuario=eq.$idUsuario&id_evento=eq.$idEvento",
                    body,
                )
            }
            dao?.markInscripcionCancelada(idUsuario, idEvento)
            true
        }.getOrElse { false }
    }

    suspend fun getInscritosByEvento(idEvento: Int): List<Inscrito> {
        val cached = dao?.inscritosByEvento(idEvento)?.map { it.toModel() } ?: emptyList()
        if (!SupabaseConfig.isConfigured || !isOnline()) return cached

        return runCatching {
            val data = SupabaseClient.select(
                "inscripcion",
                "select=id_inscripcion,id_usuario,estado,usuario:usuario(nombre_mostrar,correo),asistencia:asistencia(*)&id_evento=eq.$idEvento",
            )
            val items = data.mapNotNull { element ->
                val obj = element.asJsonObject
                val usuario = when (val el = obj.get("usuario")) {
                    is JsonObject -> el
                    is JsonArray -> el.firstOrNull()?.asJsonObject
                    else -> null
                }
                val asistencias = when (val el = obj.get("asistencia")) {
                    is JsonObject -> listOf(el)
                    is JsonArray -> el.mapNotNull { it as? JsonObject }
                    else -> emptyList()
                }
                val asistenciaValida = asistencias.firstOrNull {
                    it.get("estado_validacion")?.asString == "VALIDADA"
                }
                Inscrito(
                    idInscripcion = obj.get("id_inscripcion")?.asInt ?: 0,
                    idUsuario = obj.get("id_usuario")?.asInt ?: 0,
                    idEvento = idEvento,
                    nombre = usuario?.get("nombre_mostrar")?.takeIf { !it.isJsonNull }?.asString ?: "-",
                    correo = usuario?.get("correo")?.takeIf { !it.isJsonNull }?.asString ?: "-",
                    estado = obj.get("estado")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
                    asistio = asistenciaValida != null,
                    asistenciaRegistradoPor = asistenciaValida
                        ?.get("registrado_por")?.takeIf { !it.isJsonNull }?.asString,
                    asistenciaFecha = asistenciaValida
                        ?.get("fecha_marcacion")?.takeIf { !it.isJsonNull }?.asString,
                )
            }
            cacheInscritos(idEvento, items)
            items
        }.getOrElse { cached }
    }

    // ---------------------------------------------------------------------
    // Asistencia y encuestas
    // ---------------------------------------------------------------------

    suspend fun registrarAsistencia(
        idInscripcion: Int,
        latitud: Double? = null,
        longitud: Double? = null,
        estadoValidacion: String = "VALIDADA",
        registradoPor: String? = null,
    ): Result<Boolean> {
        val fechaMarcacion = nowIso()
        val identificador = UUID.randomUUID().toString()
        val estado = estadoValidacion
            .takeIf { it == "VALIDADA" || it == "SIN_VALIDACION" }
            ?: "VALIDADA"

        if (!SupabaseConfig.isConfigured || !isOnline()) {
            if (database == null) {
                return Result.failure(IllegalStateException("Sin conexión y sin almacenamiento local disponible"))
            }
            enqueueAsistencia(
                idInscripcion = idInscripcion,
                latitud = latitud,
                longitud = longitud,
                fechaMarcacion = fechaMarcacion,
                identificador = identificador,
                estadoValidacion = estado,
                registradoPor = registradoPor,
            )
            markAsistenciaLocal(idInscripcion, estado, registradoPor, fechaMarcacion)
            return Result.success(false)
        }

        return registrarAsistenciaRemote(
            idInscripcion = idInscripcion,
            latitud = latitud,
            longitud = longitud,
            fechaMarcacion = fechaMarcacion,
            identificador = identificador,
            estadoValidacion = estado,
            registradoPor = registradoPor,
        ).fold(
            onSuccess = {
                markAsistenciaLocal(idInscripcion, estado, registradoPor, fechaMarcacion)
                Result.success(true)
            },
            onFailure = { error ->
                if (error.isConnectivityError()) {
                    enqueueAsistencia(
                        idInscripcion = idInscripcion,
                        latitud = latitud,
                        longitud = longitud,
                        fechaMarcacion = fechaMarcacion,
                        identificador = identificador,
                        estadoValidacion = estado,
                        registradoPor = registradoPor,
                    )
                    markAsistenciaLocal(idInscripcion, estado, registradoPor, fechaMarcacion)
                    Result.success(false)
                } else {
                    Result.failure(error)
                }
            },
        )
    }

    private suspend fun registrarAsistenciaRemote(
        idInscripcion: Int,
        latitud: Double?,
        longitud: Double?,
        fechaMarcacion: String,
        identificador: String,
        estadoValidacion: String,
        registradoPor: String? = null,
    ): Result<Unit> = runCatching {
        val body = JsonObject().apply {
            addProperty("id_inscripcion", idInscripcion)
            addProperty("fecha_marcacion", fechaMarcacion)
            if (latitud == null) add("latitud", JsonNull.INSTANCE) else addProperty("latitud", latitud)
            if (longitud == null) add("longitud", JsonNull.INSTANCE) else addProperty("longitud", longitud)
            addProperty("estado_validacion", estadoValidacion)
            addProperty("estado_sincronizacion", "SINCRONIZADO")
            addProperty("identificador_operacion", identificador)
            if (registradoPor.isNullOrBlank()) {
                add("registrado_por", JsonNull.INSTANCE)
            } else {
                addProperty("registrado_por", registradoPor)
            }
        }
        retryWithoutUnknownColumns(body) {
            SupabaseClient.insert("asistencia", body)
        }
        Unit
    }.recoverCatching { error ->
        val duplicate = error is SupabaseException &&
            (error.code == "23505" || error.message?.contains("duplicate", ignoreCase = true) == true)
        if (duplicate) {
            Unit
        } else {
            throw error
        }
    }

    private suspend fun enqueueAsistencia(
        idInscripcion: Int,
        latitud: Double?,
        longitud: Double?,
        fechaMarcacion: String,
        identificador: String,
        estadoValidacion: String,
        registradoPor: String? = null,
    ) {
        val payload = AsistenciaPayload(
            idInscripcion = idInscripcion,
            latitud = latitud,
            longitud = longitud,
            fechaMarcacion = fechaMarcacion,
            identificadorOperacion = identificador,
            estadoValidacion = estadoValidacion,
            registradoPor = registradoPor,
        )
        dao?.enqueue(
            PendingActionEntity(
                tipo = PENDING_ASISTENCIA,
                clave = "$PENDING_ASISTENCIA:$idInscripcion",
                payload = gson.toJson(payload),
            ),
        )
        appContext?.let { SyncScheduler.syncNow(it) }
    }

    suspend fun registrarEncuesta(
        idInscripcion: Int,
        calificacion: Int,
        utilidad: String = "MUY_UTIL",
        comentarios: String? = null,
    ): Result<Boolean> {
        val identificador = UUID.randomUUID().toString()

        if (!SupabaseConfig.isConfigured || !isOnline()) {
            if (database == null) {
                return Result.failure(IllegalStateException("Sin conexión y sin almacenamiento local disponible"))
            }
            enqueueEncuesta(idInscripcion, calificacion, utilidad, comentarios, identificador)
            saveEncuestaLocal(idInscripcion, calificacion, utilidad, comentarios)
            return Result.success(false)
        }

        return registrarEncuestaRemote(
            idInscripcion = idInscripcion,
            calificacion = calificacion,
            utilidad = utilidad,
            comentarios = comentarios,
            identificador = identificador,
        ).fold(
            onSuccess = {
                saveEncuestaLocal(idInscripcion, calificacion, utilidad, comentarios)
                Result.success(true)
            },
            onFailure = { error ->
                if (error.isConnectivityError()) {
                    enqueueEncuesta(idInscripcion, calificacion, utilidad, comentarios, identificador)
                    saveEncuestaLocal(idInscripcion, calificacion, utilidad, comentarios)
                    Result.success(false)
                } else {
                    Result.failure(error)
                }
            },
        )
    }

    private suspend fun registrarEncuestaRemote(
        idInscripcion: Int,
        calificacion: Int,
        utilidad: String,
        comentarios: String?,
        identificador: String,
    ): Result<Unit> = runCatching {
        val body = JsonObject().apply {
            addProperty("id_inscripcion", idInscripcion)
            addProperty("calificacion", calificacion)
            addProperty("utilidad", utilidad)
            if (comentarios.isNullOrBlank()) add("comentarios", JsonNull.INSTANCE) else addProperty("comentarios", comentarios)
            addProperty("identificador_operacion", identificador)
        }
        retryWithoutUnknownColumns(body) {
            SupabaseClient.upsert("encuesta", body, "id_inscripcion")
        }
        Unit
    }

    private suspend fun enqueueEncuesta(
        idInscripcion: Int,
        calificacion: Int,
        utilidad: String,
        comentarios: String?,
        identificador: String,
    ) {
        val payload = EncuestaPayload(
            idInscripcion = idInscripcion,
            calificacion = calificacion,
            utilidad = utilidad,
            comentarios = comentarios,
            identificadorOperacion = identificador,
        )
        dao?.enqueue(
            PendingActionEntity(
                tipo = PENDING_ENCUESTA,
                clave = "$PENDING_ENCUESTA:$idInscripcion",
                payload = gson.toJson(payload),
            ),
        )
        appContext?.let { SyncScheduler.syncNow(it) }
    }

    suspend fun syncPending(): Int = syncMutex.withLock {
        val d = dao ?: return@withLock 0
        if (!SupabaseConfig.isConfigured || !isOnline()) return@withLock 0

        var synced = 0
        d.pendingActions().forEach { action ->
            val result: Result<Unit> = when (action.tipo) {
                PENDING_ASISTENCIA -> {
                    val payload = runCatching {
                        gson.fromJson(action.payload, AsistenciaPayload::class.java)
                    }.getOrNull()
                    if (payload == null) {
                        Result.success(Unit)
                    } else {
                        registrarAsistenciaRemote(
                            idInscripcion = payload.idInscripcion,
                            latitud = payload.latitud,
                            longitud = payload.longitud,
                            fechaMarcacion = payload.fechaMarcacion,
                            identificador = payload.identificadorOperacion,
                            estadoValidacion = payload.estadoValidacion ?: "VALIDADA",
                            registradoPor = payload.registradoPor,
                        )
                    }
                }

                PENDING_ENCUESTA -> {
                    val payload = runCatching {
                        gson.fromJson(action.payload, EncuestaPayload::class.java)
                    }.getOrNull()
                    if (payload == null) {
                        Result.success(Unit)
                    } else {
                        registrarEncuestaRemote(
                            idInscripcion = payload.idInscripcion,
                            calificacion = payload.calificacion,
                            utilidad = payload.utilidad,
                            comentarios = payload.comentarios,
                            identificador = payload.identificadorOperacion,
                        )
                    }
                }

                else -> Result.success(Unit)
            }

            if (result.isSuccess) {
                d.deletePending(action)
                synced++
            } else {
                val error = result.exceptionOrNull()
                val permanent = error is SupabaseException &&
                    error.status in 400..499 &&
                    error.status != 408 &&
                    error.status != 429
                if (permanent) {
                    d.deletePending(action)
                } else {
                    d.incrementAttempts(action.id)
                }
            }
        }
        synced
    }

    suspend fun pendingCount(): Int = dao?.pendingCount() ?: 0

    suspend fun getMetricasEvento(idEvento: Int): MetricasEvento {
        if (!SupabaseConfig.isConfigured || !isOnline()) return metricasLocales(idEvento)
        return runCatching {
            val inscripciones = SupabaseClient.select(
                "inscripcion",
                "select=id_inscripcion&id_evento=eq.$idEvento",
            )
            val totalInscritos = inscripciones.size()
            val ids = inscripciones.mapNotNull {
                it.asJsonObject.get("id_inscripcion")?.takeIf { id -> !id.isJsonNull }?.asInt
            }
            var totalAsistencias = 0
            var encuestasRespondidas = 0
            var suma = 0
            val comentarios = mutableListOf<String>()

            if (ids.isNotEmpty()) {
                val inFilter = ids.joinToString(",")
                // Se cuentan tanto las asistencias validadas por GPS como las
                // registradas sin validación (eventos sin coordenadas).
                val asistencias = SupabaseClient.select(
                    "asistencia",
                    "select=id_asistencia&id_inscripcion=in.($inFilter)" +
                        "&estado_validacion=in.(VALIDADA,SIN_VALIDACION)",
                )
                totalAsistencias = asistencias.size()

                val encuestas = SupabaseClient.select(
                    "encuesta",
                    "select=calificacion,comentarios&id_inscripcion=in.($inFilter)",
                )
                encuestasRespondidas = encuestas.size()
                encuestas.forEach { element ->
                    val obj = element.asJsonObject
                    suma += obj.get("calificacion")?.takeIf { !it.isJsonNull }?.asInt ?: 0
                    obj.get("comentarios")?.takeIf { !it.isJsonNull }?.asString
                        ?.takeIf { it.isNotBlank() }?.let { comentarios.add(it) }
                }
            }

            MetricasEvento(
                totalInscritos = totalInscritos,
                totalAsistencias = totalAsistencias,
                porcentajeAsistencia = if (totalInscritos > 0) (totalAsistencias * 100) / totalInscritos else 0,
                encuestasRespondidas = encuestasRespondidas,
                porcentajeEncuestas = if (totalInscritos > 0) (encuestasRespondidas * 100) / totalInscritos else 0,
                satisfaccionPromedio = if (encuestasRespondidas > 0) {
                    Math.round(suma.toDouble() / encuestasRespondidas * 100.0) / 100.0
                } else {
                    0.0
                },
                comentarios = comentarios,
            )
        }.getOrElse { metricasLocales(idEvento) }
    }

    /**
     * Métricas calculadas con la caché Room. Es la fuente principal cuando no
     * hay Supabase/EC2 disponibles y el respaldo si la consulta remota falla.
     */
    private suspend fun metricasLocales(idEvento: Int): MetricasEvento {
        val d = dao ?: return MetricasEvento()
        val inscripciones = d.inscripcionesByEvento(idEvento)
            .filter { it.estado.uppercase() != "CANCELADO" }
        val inscritos = d.inscritosByEvento(idEvento)

        val totalInscritos = if (inscripciones.isNotEmpty()) {
            inscripciones.size
        } else {
            inscritos.size
        }
        val asistenciasEnInscripciones = inscripciones.count {
            !it.asistenciaEstado.isNullOrBlank()
        }
        val asistenciasEnInscritos = inscritos.count { it.asistio }
        val totalAsistencias = maxOf(asistenciasEnInscripciones, asistenciasEnInscritos)
            .coerceAtMost(totalInscritos)

        val encuestadas = inscripciones.filter { (it.encuestaCalificacion ?: 0) > 0 }
        val suma = encuestadas.sumOf { it.encuestaCalificacion ?: 0 }
        val comentarios = encuestadas.mapNotNull { it.encuestaComentarios }
            .filter { it.isNotBlank() }

        return MetricasEvento(
            totalInscritos = totalInscritos,
            totalAsistencias = totalAsistencias,
            porcentajeAsistencia = if (totalInscritos > 0) {
                (totalAsistencias * 100) / totalInscritos
            } else {
                0
            },
            encuestasRespondidas = encuestadas.size,
            porcentajeEncuestas = if (totalInscritos > 0) {
                (encuestadas.size * 100) / totalInscritos
            } else {
                0
            },
            satisfaccionPromedio = if (encuestadas.isNotEmpty()) {
                Math.round(suma.toDouble() / encuestadas.size * 100.0) / 100.0
            } else {
                0.0
            },
            comentarios = comentarios,
        )
    }

    private data class AsistenciaPayload(
        val idInscripcion: Int,
        val latitud: Double?,
        val longitud: Double?,
        val fechaMarcacion: String,
        val identificadorOperacion: String,
        val estadoValidacion: String? = null,
        val registradoPor: String? = null,
    )

    private data class EncuestaPayload(
        val idInscripcion: Int,
        val calificacion: Int,
        val utilidad: String,
        val comentarios: String?,
        val identificadorOperacion: String,
    )
}
