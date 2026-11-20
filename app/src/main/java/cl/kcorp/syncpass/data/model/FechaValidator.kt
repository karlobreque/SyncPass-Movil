package cl.kcorp.syncpass.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Validación de rangos de fecha y hora.
 *
 * Regla de negocio: el término puede ser el mismo día (o la misma hora) que el
 * inicio, pero nunca puede quedar antes. Aplica a proyectos (solo fecha) y a
 * eventos (fecha y hora). Es código puro, testeable en JVM.
 */
object FechaValidator {

    private const val PATRON_FECHA = "yyyy-MM-dd"
    private const val PATRON_FECHA_HORA = "yyyy-MM-dd'T'HH:mm"
    private const val PATRON_FECHA_HORA_SEGUNDOS = "yyyy-MM-dd'T'HH:mm:ss"

    /** Reconoce los formatos que usa la app: `AAAA-MM-DD` y `AAAA-MM-DDTHH:mm` (opcional segundos). */
    fun esValido(value: String): Boolean = parse(value) != null

    /** Indica si el rango es válido: ambas fechas existen y el término no es anterior al inicio. */
    fun esRangoValido(fechaInicio: String, fechaTermino: String): Boolean {
        val inicio = parse(fechaInicio) ?: return false
        val termino = parse(fechaTermino) ?: return false
        return !termino.before(inicio)
    }

    /** Mensaje de error listo para mostrar en la UI; `null` si el rango es válido. */
    fun mensajeError(fechaInicio: String, fechaTermino: String): String? {
        if (!esValido(fechaInicio)) return "La fecha de inicio no es válida."
        if (!esValido(fechaTermino)) return "La fecha de término no es válida."
        return if (esRangoValido(fechaInicio, fechaTermino)) {
            null
        } else {
            "La fecha de término no puede ser anterior a la de inicio."
        }
    }

    /**
     * Indica si un evento cae dentro del período del proyecto.
     *
     * El proyecto usa fechas (`yyyy-MM-dd`), por lo que su término cubre hasta el
     * final de ese día. El evento puede usar fecha y hora (`yyyy-MM-ddTHH:mm`).
     */
    fun rangoDentroDePeriodo(
        periodoInicio: String,
        periodoTermino: String,
        fechaInicio: String,
        fechaTermino: String,
    ): Boolean {
        val inicioPeriodo = parse(periodoInicio) ?: return false
        val finPeriodo = parseFinPeriodo(periodoTermino) ?: return false
        val inicio = parse(fechaInicio) ?: return false
        val termino = parse(fechaTermino) ?: return false
        return !inicio.before(inicioPeriodo) && !termino.after(finPeriodo)
    }

    /** Mensaje de error si el evento se sale del período del proyecto; `null` si está dentro. */
    fun mensajeErrorPeriodo(
        periodoInicio: String,
        periodoTermino: String,
        fechaInicio: String,
        fechaTermino: String,
    ): String? {
        if (!esValido(periodoInicio) || !esValido(periodoTermino)) return null
        if (!esValido(fechaInicio) || !esValido(fechaTermino)) return null

        val inicioPeriodo = parse(periodoInicio) ?: return null
        val finPeriodo = parseFinPeriodo(periodoTermino) ?: return null
        val inicio = parse(fechaInicio) ?: return null
        val termino = parse(fechaTermino) ?: return null

        return when {
            inicio.before(inicioPeriodo) ->
                "El evento no puede comenzar antes del inicio del proyecto (${fechaCorta(periodoInicio)})."

            termino.after(finPeriodo) ->
                "El evento no puede terminar después del término del proyecto (${fechaCorta(periodoTermino)})."

            else -> null
        }
    }

    /** Si el valor trae solo fecha, extiende el límite hasta el final de ese día. */
    private fun parseFinPeriodo(value: String): Date? {
        val fecha = parse(value) ?: return null
        val soloFecha = !value.trim().contains('T') && !value.trim().contains(' ')
        return if (soloFecha) Date(fecha.time + 86_400_000L - 1) else fecha
    }

    /** Formatea una fecha ISO como `dd/MM/yyyy` para los mensajes de la UI. */
    private fun fechaCorta(value: String): String {
        val fecha = parse(value) ?: return value.trim().take(10)
        return SimpleDateFormat("dd/MM/yyyy", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(fecha)
    }

    /** Convierte un valor de la app a [Date] en UTC; `null` si no es reconocible. */
    fun parse(value: String): Date? {
        val text = value.trim().replace(' ', 'T')
        val patron = when {
            Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$").matches(text) -> PATRON_FECHA_HORA_SEGUNDOS
            Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$").matches(text) -> PATRON_FECHA_HORA
            Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(text) -> PATRON_FECHA
            else -> return null
        }
        return runCatching {
            SimpleDateFormat(patron, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(text)
        }.getOrNull()
    }
}
