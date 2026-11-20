package cl.kcorp.syncpass.data.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lectura de ubicación normalizada e independiente de Android.
 * Permite testear la validación GPS con JUnit sin emulador.
 */
data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val provider: String? = null,
) {
    fun esReciente(maxAgeMillis: Long = 60_000L): Boolean =
        System.currentTimeMillis() - timestampMillis <= maxAgeMillis
}

enum class EstadoValidacionUbicacion {
    /** El asistente está dentro del radio permitido. */
    VALIDADA,

    /** El evento no tiene coordenadas configuradas; se registra sin validación GPS. */
    EVENTO_SIN_COORDENADAS,

    /** No se pudo obtener una lectura GPS utilizable. */
    SIN_UBICACION,

    /** La lectura existe pero su margen de error es demasiado alto para decidir. */
    PRECISION_INSUFICIENTE,

    /** El asistente está claramente fuera del radio del evento. */
    FUERA_DE_RADIO,
}

data class ValidacionUbicacion(
    val estado: EstadoValidacionUbicacion,
    val distanciaMetros: Double? = null,
    val precisionMetros: Double? = null,
    val radioMetros: Int = LocationValidator.DEFAULT_RADIO_METROS,
    val mensaje: String = "",
) {
    /** Solo se permite marcar asistencia si la ubicación es válida o el evento no exige GPS. */
    val permiteRegistro: Boolean
        get() = estado == EstadoValidacionUbicacion.VALIDADA ||
            estado == EstadoValidacionUbicacion.EVENTO_SIN_COORDENADAS

    /** Estado que se persiste en la tabla asistencia. */
    val estadoAsistencia: String
        get() = if (estado == EstadoValidacionUbicacion.VALIDADA) "VALIDADA" else "SIN_VALIDACION"
}

/**
 * Validación de proximidad basada en la fórmula de Haversine.
 *
 * Regla aplicada:
 *  - Se calcula la distancia real entre el evento y el asistente.
 *  - Se tolera el margen de error informado por el GPS, pero acotado a la mitad
 *    del radio (máximo 75 m) para que una lectura muy imprecisa no permita
 *    validar desde lejos.
 *  - Si el margen de error supera [PRECISION_MAXIMA_METROS], se rechaza la
 *    lectura y se pide reintentar: es más honesto que aprobar a ciegas.
 */
object LocationValidator {

    const val DEFAULT_RADIO_METROS = 100
    const val PRECISION_MAXIMA_METROS = 250.0
    const val TOLERANCIA_MAXIMA_METROS = 75.0

    /** Valores permitidos para el radio de validación, en metros. */
    val RADIOS_VALIDOS = listOf(20, 40, 60, 80, 100)

    private const val EARTH_RADIUS_METROS = 6_371_000.0

    /**
     * Ajusta un radio arbitrario al valor permitido más cercano.
     * Evita que valores escritos a mano queden fuera del catálogo.
     */
    fun normalizarRadio(radioMetros: Int): Int =
        RADIOS_VALIDOS.firstOrNull { it == radioMetros }
            ?: RADIOS_VALIDOS.minByOrNull { kotlin.math.abs(it - radioMetros) }
            ?: DEFAULT_RADIO_METROS

    fun distanciaMetros(
        latitudOrigen: Double,
        longitudOrigen: Double,
        latitudDestino: Double,
        longitudDestino: Double,
    ): Double {
        val deltaLat = Math.toRadians(latitudDestino - latitudOrigen)
        val deltaLon = Math.toRadians(longitudDestino - longitudOrigen)
        val lat1 = Math.toRadians(latitudOrigen)
        val lat2 = Math.toRadians(latitudDestino)

        val a = sin(deltaLat / 2).pow(2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
        val c = 2 * asin(min(1.0, sqrt(a)))
        return EARTH_RADIUS_METROS * c
    }

    fun validar(
        eventoLatitud: Double?,
        eventoLongitud: Double?,
        radioMetros: Int,
        ubicacion: LocationSnapshot?,
        precisionMaximaMetros: Double = PRECISION_MAXIMA_METROS,
    ): ValidacionUbicacion {
        val radio = normalizarRadio(radioMetros.takeIf { it > 0 } ?: DEFAULT_RADIO_METROS)

        if (eventoLatitud == null || eventoLongitud == null) {
            return ValidacionUbicacion(
                estado = EstadoValidacionUbicacion.EVENTO_SIN_COORDENADAS,
                radioMetros = radio,
                mensaje = "El evento no tiene coordenadas configuradas; la asistencia se guarda como SIN VALIDACIÓN GPS.",
            )
        }

        if (ubicacion == null) {
            return ValidacionUbicacion(
                estado = EstadoValidacionUbicacion.SIN_UBICACION,
                radioMetros = radio,
                mensaje = "No se pudo obtener tu ubicación. Activa el GPS y concede el permiso de ubicación para validar la asistencia.",
            )
        }

        val precision = ubicacion.accuracyMeters
        if (precision != null && precision > precisionMaximaMetros) {
            return ValidacionUbicacion(
                estado = EstadoValidacionUbicacion.PRECISION_INSUFICIENTE,
                precisionMetros = precision,
                radioMetros = radio,
                mensaje = "La señal GPS es muy imprecisa (±${precision.toInt()} m). Espera unos segundos y vuelve a escanear.",
            )
        }

        val distancia = distanciaMetros(
            ubicacion.latitude,
            ubicacion.longitude,
            eventoLatitud,
            eventoLongitud,
        )
        val tolerancia = min(
            precision ?: 0.0,
            min(TOLERANCIA_MAXIMA_METROS, radio * 0.5),
        )
        val distanciaEfectiva = (distancia - tolerancia).coerceAtLeast(0.0)

        return if (distanciaEfectiva <= radio) {
            ValidacionUbicacion(
                estado = EstadoValidacionUbicacion.VALIDADA,
                distanciaMetros = distancia,
                precisionMetros = precision,
                radioMetros = radio,
                mensaje = buildString {
                    append("Ubicación validada: estás a ${distancia.toInt()} m del evento (radio ${radio} m")
                    if (precision != null) append(", precisión ±${precision.toInt()} m")
                    append(").")
                },
            )
        } else {
            ValidacionUbicacion(
                estado = EstadoValidacionUbicacion.FUERA_DE_RADIO,
                distanciaMetros = distancia,
                precisionMetros = precision,
                radioMetros = radio,
                mensaje = "Estás a ${distancia.toInt()} m del evento; el radio permitido es de $radio m. Acércate al lugar para registrar la asistencia.",
            )
        }
    }
}
