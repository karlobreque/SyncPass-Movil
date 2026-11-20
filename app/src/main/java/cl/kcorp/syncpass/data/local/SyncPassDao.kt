package cl.kcorp.syncpass.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncPassDao {

    @Query("SELECT * FROM proyecto_cache WHERE idCoordinador = :idCoordinador ORDER BY fechaInicio DESC")
    suspend fun proyectosByCoordinador(idCoordinador: Int): List<ProyectoEntity>

    @Query("SELECT * FROM proyecto_cache ORDER BY fechaInicio DESC")
    suspend fun proyectos(): List<ProyectoEntity>

    @Query("SELECT * FROM proyecto_cache WHERE idProyecto = :idProyecto LIMIT 1")
    suspend fun proyectoById(idProyecto: Int): ProyectoEntity?

    @Query("SELECT MIN(idProyecto) FROM proyecto_cache")
    suspend fun minProyectoId(): Int?

    @Query("SELECT COUNT(*) FROM proyecto_cache")
    suspend fun countProyectos(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProyectos(items: List<ProyectoEntity>)

    @Query("DELETE FROM proyecto_cache WHERE idCoordinador = :idCoordinador")
    suspend fun clearProyectos(idCoordinador: Int)

    @Query("DELETE FROM proyecto_cache WHERE idCoordinador = :idCoordinador AND idProyecto NOT IN (:ids)")
    suspend fun deleteStaleProyectos(idCoordinador: Int, ids: List<Int>)

    @Query("DELETE FROM proyecto_cache WHERE idProyecto = :idProyecto")
    suspend fun deleteProyecto(idProyecto: Int)

    @Query(
        "SELECT * FROM responsable_cache " +
            "WHERE tipoEntidad = :tipoEntidad AND idEntidad = :idEntidad " +
            "ORDER BY rol ASC, idUsuario ASC",
    )
    suspend fun responsables(tipoEntidad: String, idEntidad: Int): List<ResponsableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResponsables(items: List<ResponsableEntity>)

    @Query("DELETE FROM responsable_cache WHERE tipoEntidad = :tipoEntidad AND idEntidad = :idEntidad")
    suspend fun clearResponsables(tipoEntidad: String, idEntidad: Int)

    @Query(
        "DELETE FROM responsable_cache " +
            "WHERE tipoEntidad = :tipoEntidad AND idEntidad = :idEntidad AND idUsuario NOT IN (:ids)",
    )
    suspend fun deleteStaleResponsables(tipoEntidad: String, idEntidad: Int, ids: List<Int>)

    @Query(
        "SELECT r.idEntidad AS idProyecto, u.idUsuario AS idUsuario, " +
            "u.nombreMostrar AS nombre, u.correo AS correo, r.rol AS rol " +
            "FROM responsable_cache r INNER JOIN usuario_cache u ON u.idUsuario = r.idUsuario " +
            "WHERE r.tipoEntidad = 'PROYECTO' ORDER BY u.nombreMostrar ASC",
    )
    suspend fun responsablesPorProyecto(): List<ResponsableProyectoJoin>

    @Query(
        "SELECT r.idEntidad AS idEvento, u.idUsuario AS idUsuario, " +
            "u.nombreMostrar AS nombre, u.correo AS correo, r.rol AS rol " +
            "FROM responsable_cache r INNER JOIN usuario_cache u ON u.idUsuario = r.idUsuario " +
            "WHERE r.tipoEntidad = 'EVENTO' ORDER BY u.nombreMostrar ASC",
    )
    suspend fun responsablesPorEvento(): List<ResponsableEventoJoin>

    @Insert
    suspend fun insertHistorialResponsable(item: ResponsableHistorialEntity): Long

    @Query(
        "SELECT * FROM responsable_historial " +
            "WHERE tipoEntidad = :tipoEntidad AND idEntidad = :idEntidad " +
            "ORDER BY fecha DESC, id DESC",
    )
    suspend fun historialResponsables(tipoEntidad: String, idEntidad: Int): List<ResponsableHistorialEntity>

    @Query("DELETE FROM responsable_historial WHERE tipoEntidad = :tipoEntidad AND idEntidad = :idEntidad")
    suspend fun clearHistorialResponsables(tipoEntidad: String, idEntidad: Int)

    @Query(
        "DELETE FROM responsable_cache WHERE tipoEntidad = 'EVENTO' " +
            "AND idEntidad IN (SELECT idEvento FROM evento_cache WHERE idProyecto = :idProyecto)",
    )
    suspend fun clearResponsablesEventosByProyecto(idProyecto: Int)

    @Query(
        "DELETE FROM responsable_historial WHERE tipoEntidad = 'EVENTO' " +
            "AND idEntidad IN (SELECT idEvento FROM evento_cache WHERE idProyecto = :idProyecto)",
    )
    suspend fun clearHistorialEventosByProyecto(idProyecto: Int)

    @Query("SELECT * FROM evento_cache ORDER BY fechaInicio ASC")
    suspend fun eventos(): List<EventoEntity>

    @Query("SELECT * FROM evento_cache WHERE idProyecto = :idProyecto ORDER BY fechaInicio ASC")
    suspend fun eventosByProyecto(idProyecto: Int): List<EventoEntity>

    @Query("SELECT MIN(idEvento) FROM evento_cache")
    suspend fun minEventoId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEventos(items: List<EventoEntity>)

    @Query("DELETE FROM evento_cache")
    suspend fun clearEventos()

    @Query("DELETE FROM evento_cache WHERE idEvento NOT IN (:ids)")
    suspend fun deleteStaleEventos(ids: List<Int>)

    @Query("DELETE FROM evento_cache WHERE idProyecto = :idProyecto")
    suspend fun clearEventosByProyecto(idProyecto: Int)

    @Query("DELETE FROM evento_cache WHERE idEvento = :idEvento")
    suspend fun deleteEvento(idEvento: Int)

    @Query("SELECT * FROM inscripcion_cache WHERE idUsuario = :idUsuario")
    suspend fun inscripcionesByUsuario(idUsuario: Int): List<InscripcionEntity>

    @Query("SELECT * FROM inscripcion_cache WHERE idEvento = :idEvento")
    suspend fun inscripcionesByEvento(idEvento: Int): List<InscripcionEntity>

    @Query("SELECT * FROM inscripcion_cache WHERE idUsuario = :idUsuario AND idEvento = :idEvento LIMIT 1")
    suspend fun inscripcionByUsuarioEvento(idUsuario: Int, idEvento: Int): InscripcionEntity?

    @Query("SELECT MIN(idInscripcion) FROM inscripcion_cache")
    suspend fun minInscripcionId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInscripciones(items: List<InscripcionEntity>)

    @Query("DELETE FROM inscripcion_cache WHERE idUsuario = :idUsuario")
    suspend fun clearInscripciones(idUsuario: Int)

    @Query("DELETE FROM inscripcion_cache WHERE idUsuario = :idUsuario AND idInscripcion NOT IN (:ids)")
    suspend fun deleteStaleInscripciones(idUsuario: Int, ids: List<Int>)

    @Query(
        "UPDATE inscripcion_cache SET asistenciaEstado = :estado, " +
            "asistenciaRegistradoPor = :registradoPor, asistenciaFecha = :fecha " +
            "WHERE idInscripcion = :idInscripcion",
    )
    suspend fun markAsistencia(
        idInscripcion: Int,
        estado: String,
        registradoPor: String?,
        fecha: String?,
    )

    @Query("UPDATE inscripcion_cache SET estado = 'CANCELADO' WHERE idUsuario = :idUsuario AND idEvento = :idEvento")
    suspend fun markInscripcionCancelada(idUsuario: Int, idEvento: Int)

    @Query(
        "UPDATE inscripcion_cache SET encuestaCalificacion = :calificacion, " +
            "encuestaUtilidad = :utilidad, encuestaComentarios = :comentarios " +
            "WHERE idInscripcion = :idInscripcion",
    )
    suspend fun saveEncuestaLocal(
        idInscripcion: Int,
        calificacion: Int,
        utilidad: String,
        comentarios: String?,
    )

    @Query("SELECT * FROM inscrito_cache WHERE idEvento = :idEvento")
    suspend fun inscritosByEvento(idEvento: Int): List<InscritoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInscritos(items: List<InscritoEntity>)

    @Query("DELETE FROM inscrito_cache WHERE idEvento = :idEvento")
    suspend fun clearInscritos(idEvento: Int)

    @Query("DELETE FROM inscrito_cache WHERE idEvento = :idEvento AND idInscripcion NOT IN (:ids)")
    suspend fun deleteStaleInscritos(idEvento: Int, ids: List<Int>)

    @Query(
        "UPDATE inscrito_cache SET asistio = 1, " +
            "asistenciaRegistradoPor = :registradoPor, asistenciaFecha = :fecha " +
            "WHERE idInscripcion = :idInscripcion",
    )
    suspend fun markInscritoAsistio(
        idInscripcion: Int,
        registradoPor: String?,
        fecha: String?,
    )

    @Query("DELETE FROM inscrito_cache WHERE idInscripcion = :idInscripcion")
    suspend fun deleteInscrito(idInscripcion: Int)

    @Query("SELECT * FROM usuario_cache WHERE correo = :correo LIMIT 1")
    suspend fun usuarioByCorreo(correo: String): UsuarioEntity?

    @Query("SELECT * FROM usuario_cache WHERE idUsuario = :idUsuario LIMIT 1")
    suspend fun usuarioById(idUsuario: Int): UsuarioEntity?

    @Query("SELECT MIN(idUsuario) FROM usuario_cache")
    suspend fun minUsuarioId(): Int?

    @Query("SELECT COUNT(*) FROM usuario_cache")
    suspend fun countUsuarios(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsuario(item: UsuarioEntity)

    @Query("SELECT * FROM usuario_cache ORDER BY nombreMostrar ASC")
    suspend fun usuarios(): List<UsuarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsuarios(items: List<UsuarioEntity>)

    @Query("DELETE FROM usuario_cache")
    suspend fun clearUsuarios()

    @Query("DELETE FROM usuario_cache WHERE idUsuario NOT IN (:ids)")
    suspend fun deleteStaleUsuarios(ids: List<Int>)

    @Query("DELETE FROM usuario_cache WHERE idUsuario = :idUsuario")
    suspend fun deleteUsuario(idUsuario: Int)

    @Query("DELETE FROM responsable_cache WHERE idUsuario = :idUsuario")
    suspend fun deleteResponsablesByUsuario(idUsuario: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(action: PendingActionEntity): Long

    @Query("SELECT * FROM pending_action ORDER BY createdAt ASC")
    suspend fun pendingActions(): List<PendingActionEntity>

    @Delete
    suspend fun deletePending(action: PendingActionEntity)

    @Query("UPDATE pending_action SET intentos = intentos + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Query("SELECT COUNT(*) FROM pending_action")
    suspend fun pendingCount(): Int
}

/** Proyección de un responsable de proyecto con los datos del usuario. */
data class ResponsableProyectoJoin(
    val idProyecto: Int,
    val idUsuario: Int,
    val nombre: String,
    val correo: String,
    val rol: String,
)

/** Proyección de un responsable de evento con los datos del usuario. */
data class ResponsableEventoJoin(
    val idEvento: Int,
    val idUsuario: Int,
    val nombre: String,
    val correo: String,
    val rol: String,
)
