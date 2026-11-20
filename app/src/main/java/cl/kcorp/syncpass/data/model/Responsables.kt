package cl.kcorp.syncpass.data.model

/** Tipos de entidad que pueden tener responsables asignados. */
object TipoEntidadResponsable {
    const val PROYECTO = "PROYECTO"
    const val EVENTO = "EVENTO"
}

/** Roles que pueden quedar a cargo de un proyecto o evento. */
object RolResponsable {
    const val DOCENTE = "DOCENTE"
    const val VOLUNTARIO = "VOLUNTARIO"
}

/** Acciones registradas en el historial de responsables. */
object AccionResponsable {
    const val AGREGADO = "AGREGADO"
    const val ELIMINADO = "ELIMINADO"
}

/** Clave única de una asignación: una persona con un rol concreto. */
data class ClaveResponsable(
    val rol: String,
    val idUsuario: Int,
)

data class CambiosResponsables(
    val agregados: Set<ClaveResponsable>,
    val eliminados: Set<ClaveResponsable>,
)

/** Calcula qué asignaciones se agregaron y cuáles se quitaron. */
fun calcularCambiosResponsables(
    actuales: Collection<ClaveResponsable>,
    nuevos: Collection<ClaveResponsable>,
): CambiosResponsables {
    val actualesSet = actuales.toSet()
    val nuevosSet = nuevos.toSet()
    return CambiosResponsables(
        agregados = nuevosSet - actualesSet,
        eliminados = actualesSet - nuevosSet,
    )
}
