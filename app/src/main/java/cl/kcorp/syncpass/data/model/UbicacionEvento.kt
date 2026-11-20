package cl.kcorp.syncpass.data.model

import cl.kcorp.syncpass.data.location.LocationValidator
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Punto geográfico de referencia de un evento.
 *
 * Código puro, sin dependencias de Android, para poder probarse en JVM.
 * No se guarda como texto: la app persiste `latitud` y `longitud` por separado.
 */
data class CoordenadasEvento(
    val latitud: Double,
    val longitud: Double,
) {
    /** Representación legible con cinco decimales (precisión aproximada de 1 m). */
    val texto: String
        get() = String.format(Locale.US, "%.5f, %.5f", latitud, longitud)

    fun esValida(): Boolean = esLatitudValida(latitud) && esLongitudValida(longitud)

    companion object {
        fun esLatitudValida(latitud: Double): Boolean = latitud in -90.0..90.0

        fun esLongitudValida(longitud: Double): Boolean = longitud in -180.0..180.0

        fun esParValido(latitud: Double?, longitud: Double?): Boolean =
            latitud != null && longitud != null &&
                esLatitudValida(latitud) && esLongitudValida(longitud)
    }
}

/**
 * Lee coordenadas desde un texto libre o desde un enlace de Google Maps.
 *
 * Sirve como alternativa cuando el docente no está en el recinto: puede pegar
 * el enlace que le compartieron por mensajería. No resuelve enlaces cortos
 * (por ejemplo `maps.app.goo.gl`) porque eso requiere red; en ese caso devuelve
 * `null` y la UI explica que debe pegar el enlace completo.
 */
object CoordenadasParser {

    private const val NUMERO = """-?\d{1,3}(?:\.\d+)?"""

    private val patrones = listOf(
        // Formato interno de Google Maps: !3d-33.5113!4d-70.7580
        Regex("""!3d($NUMERO)!4d($NUMERO)"""),
        // Esquema geo: -33.5113,-70.7580
        Regex("""geo:($NUMERO),($NUMERO)"""),
        // Parámetros de URL: ?q=..., ?ll=..., ?destination=...
        Regex("""[?&](?:q|query|ll|center|destination|sll|daddr)=($NUMERO),($NUMERO)"""),
        // Enlace con arroba: /@-33.5113,-70.7580,17z
        Regex("""@($NUMERO),($NUMERO)"""),
        // Par simple en cualquier parte del texto: -33.5113, -70.7580
        Regex("""(-?\d{1,3}\.\d+)\s*[,;]\s*(-?\d{1,3}\.\d+)"""),
    )

    fun parsear(texto: String?): CoordenadasEvento? {
        if (texto.isNullOrBlank()) return null
        val normalizado = texto
            .replace('\u2212', '-')
            .replace('\u00A0', ' ')
            .trim()

        patrones.forEach { patron ->
            val resultado = patron.find(normalizado) ?: return@forEach
            val latitud = resultado.groupValues[1].toDoubleOrNull() ?: return@forEach
            val longitud = resultado.groupValues[2].toDoubleOrNull() ?: return@forEach
            val coordenadas = CoordenadasEvento(latitud, longitud)
            if (coordenadas.esValida()) return coordenadas
        }
        return null
    }

    fun esEnlaceCorto(texto: String?): Boolean {
        val valor = texto?.trim().orEmpty()
        return valor.contains("goo.gl/") || valor.contains("maps.app.goo.gl")
    }
}

/**
 * Reglas de calidad para el punto de referencia del evento.
 *
 * Si el punto se captura con poca precisión, el radio elegido puede rechazar
 * asistencias reales: la tolerancia del validador cubre el error del escaneo,
 * no el error con que se fijó la referencia. Por eso se sugiere un radio
 * proporcional a la precisión de la captura.
 */
object UbicacionEventoValidator {

    /** Una lectura con error mayor a 250 m no sirve como referencia del evento. */
    fun esPrecisionUtil(precisionMetros: Double?): Boolean =
        precisionMetros == null || precisionMetros <= LocationValidator.PRECISION_MAXIMA_METROS

    fun advertenciaPrecision(radioMetros: Int, precisionMetros: Double?): String? {
        if (precisionMetros == null) return null
        val radio = LocationValidator.normalizarRadio(radioMetros)
        val precision = precisionMetros.roundToInt()

        if (precisionMetros > LocationValidator.PRECISION_MAXIMA_METROS) {
            return "La lectura tiene una precisión de ±$precision m, insuficiente para fijar el " +
                "punto del evento. Intenta al aire libre o usa un enlace de Google Maps."
        }
        if (precisionMetros <= radio / 2.0) return null

        val recomendado = radioRecomendado(precisionMetros)
        return "La lectura tiene ±$precision m de precisión y el radio es de $radio m. " +
            "Con esa combinación podrías rechazar asistencias válidas; se recomienda un " +
            "radio de $recomendado m."
    }

    /**
     * Primer radio permitido que cubre el doble de la precisión de captura.
     * Si ninguno alcanza, devuelve el máximo del catálogo.
     */
    fun radioRecomendado(precisionMetros: Double): Int {
        val requerido = precisionMetros * 2
        val candidato = LocationValidator.RADIOS_VALIDOS.firstOrNull { it >= requerido }
        return candidato ?: LocationValidator.RADIOS_VALIDOS.last()
    }
}
