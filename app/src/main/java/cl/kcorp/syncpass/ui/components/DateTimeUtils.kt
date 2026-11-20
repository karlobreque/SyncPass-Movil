package cl.kcorp.syncpass.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Hora por defecto cuando un valor no incluye una hora válida. */
private const val DEFAULT_HOUR = 9

/**
 * Utilidades puras de fecha y hora para los selectores de la app.
 *
 * Se mantienen fuera de los composables para poder probarlas en JVM sin Android.
 * El formato guardado es ISO: `yyyy-MM-dd` (proyectos) o `yyyy-MM-ddTHH:mm` (eventos).
 */

/** Convierte `yyyy-MM-dd` a millis UTC de esa fecha (medianoche UTC). */
fun dateStringToUtcMillis(value: String): Long? {
    val parts = value.trim().take(10).split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (year !in 1900..2200 || month !in 1..12 || day !in 1..31) return null
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.clear()
    calendar.set(year, month - 1, day)
    return calendar.timeInMillis
}

/** Formatea millis UTC como `yyyy-MM-dd`. */
fun millisToUtcDate(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

/** Extrae (hora, minuto) de `yyyy-MM-ddTHH:mm` o `yyyy-MM-dd HH:mm`. */
fun parseTimeParts(value: String): Pair<Int, Int> {
    val match = Regex("(\\d{1,2}):(\\d{2})").find(value) ?: return DEFAULT_HOUR to 0
    val hour = match.groupValues[1].toIntOrNull() ?: return DEFAULT_HOUR to 0
    val minute = match.groupValues[2].toIntOrNull() ?: return DEFAULT_HOUR to 0
    if (hour !in 0..23 || minute !in 0..59) return DEFAULT_HOUR to 0
    return hour to minute
}

/** Formatea hora y minuto como `HH:mm` (24 h). */
fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)

/** Fecha larga en español, p. ej. `lunes, 16 de noviembre de 2026`. */
fun formatLongDate(millis: Long): String =
    SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CL")).format(Date(millis))

/** Hora corta 24 h, p. ej. `14:32:05`. */
fun formatShortTime(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(millis))

/** Fecha de hoy del dispositivo en formato `yyyy-MM-dd`. */
fun todayIsoDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
