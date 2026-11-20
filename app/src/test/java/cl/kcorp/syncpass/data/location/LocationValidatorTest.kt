package cl.kcorp.syncpass.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationValidatorTest {

    private val eventoLat = -33.5113
    private val eventoLng = -70.7580

    /** 0.0009 grados de latitud equivalen aproximadamente a 100 metros. */
    private fun ubicacionDesplazada(metros: Double, precision: Double? = 5.0): LocationSnapshot {
        val deltaLat = metros / 111_320.0
        return LocationSnapshot(
            latitude = eventoLat + deltaLat,
            longitude = eventoLng,
            accuracyMeters = precision,
        )
    }

    @Test
    fun `distancia entre el mismo punto es cero`() {
        val distancia = LocationValidator.distanciaMetros(
            eventoLat,
            eventoLng,
            eventoLat,
            eventoLng,
        )
        assertEquals(0.0, distancia, 0.001)
    }

    @Test
    fun `distancia Santiago Valparaiso es cercana a 97 km`() {
        val distancia = LocationValidator.distanciaMetros(
            -33.4489,
            -70.6693,
            -33.0472,
            -71.6127,
        )
        assertTrue("Distancia inesperada: $distancia", distancia in 90_000.0..110_000.0)
    }

    @Test
    fun `evento sin coordenadas permite registro y queda sin validacion`() {
        val resultado = LocationValidator.validar(
            eventoLatitud = null,
            eventoLongitud = null,
            radioMetros = 100,
            ubicacion = ubicacionDesplazada(10.0),
        )

        assertEquals(EstadoValidacionUbicacion.EVENTO_SIN_COORDENADAS, resultado.estado)
        assertTrue(resultado.permiteRegistro)
        assertEquals("SIN_VALIDACION", resultado.estadoAsistencia)
    }

    @Test
    fun `sin lectura de ubicacion no se permite registrar`() {
        val resultado = LocationValidator.validar(
            eventoLatitud = eventoLat,
            eventoLongitud = eventoLng,
            radioMetros = 100,
            ubicacion = null,
        )

        assertEquals(EstadoValidacionUbicacion.SIN_UBICACION, resultado.estado)
        assertFalse(resultado.permiteRegistro)
        assertNull(resultado.distanciaMetros)
    }

    @Test
    fun `asistente dentro del radio queda validado`() {
        val resultado = LocationValidator.validar(
            eventoLatitud = eventoLat,
            eventoLongitud = eventoLng,
            radioMetros = 120,
            ubicacion = ubicacionDesplazada(40.0),
        )

        assertEquals(EstadoValidacionUbicacion.VALIDADA, resultado.estado)
        assertTrue(resultado.permiteRegistro)
        assertEquals("VALIDADA", resultado.estadoAsistencia)
        assertTrue((resultado.distanciaMetros ?: 999.0) < 50.0)
    }

    @Test
    fun `precision del GPS amplia el margen cerca del borde del radio`() {
        // ~140 m reales, radio 100 m, precision 80 m => margen acotado 50 m
        val resultado = LocationValidator.validar(
            eventoLatitud = eventoLat,
            eventoLongitud = eventoLng,
            radioMetros = 100,
            ubicacion = ubicacionDesplazada(140.0, precision = 80.0),
        )

        assertEquals(EstadoValidacionUbicacion.VALIDADA, resultado.estado)
        assertTrue(resultado.permiteRegistro)
    }

    @Test
    fun `asistente claramente fuera del radio no puede registrar`() {
        val resultado = LocationValidator.validar(
            eventoLatitud = eventoLat,
            eventoLongitud = eventoLng,
            radioMetros = 100,
            ubicacion = ubicacionDesplazada(250.0, precision = 20.0),
        )

        assertEquals(EstadoValidacionUbicacion.FUERA_DE_RADIO, resultado.estado)
        assertFalse(resultado.permiteRegistro)
        assertTrue((resultado.distanciaMetros ?: 0.0) > 200.0)
    }

    @Test
    fun `precision excesiva se rechaza para no validar a ciegas`() {
        val resultado = LocationValidator.validar(
            eventoLatitud = eventoLat,
            eventoLongitud = eventoLng,
            radioMetros = 100,
            ubicacion = ubicacionDesplazada(10.0, precision = 400.0),
        )

        assertEquals(EstadoValidacionUbicacion.PRECISION_INSUFICIENTE, resultado.estado)
        assertFalse(resultado.permiteRegistro)
    }

    @Test
    fun `radio invalido usa el valor por defecto`() {
        val resultado = LocationValidator.validar(
            eventoLatitud = eventoLat,
            eventoLongitud = eventoLng,
            radioMetros = 0,
            ubicacion = ubicacionDesplazada(20.0),
        )

        assertEquals(LocationValidator.DEFAULT_RADIO_METROS, resultado.radioMetros)
        assertEquals(EstadoValidacionUbicacion.VALIDADA, resultado.estado)
    }

    @Test
    fun `radio se normaliza a los valores permitidos`() {
        assertEquals(20, LocationValidator.normalizarRadio(20))
        assertEquals(40, LocationValidator.normalizarRadio(35))
        assertEquals(60, LocationValidator.normalizarRadio(59))
        assertEquals(80, LocationValidator.normalizarRadio(90))
        assertEquals(100, LocationValidator.normalizarRadio(120))
    }

    @Test
    fun `radio fuera de catalogo se valida con el valor normalizado`() {
        val resultado = LocationValidator.validar(
            eventoLatitud = eventoLat,
            eventoLongitud = eventoLng,
            radioMetros = 120,
            ubicacion = ubicacionDesplazada(40.0),
        )

        assertEquals(100, resultado.radioMetros)
        assertEquals(EstadoValidacionUbicacion.VALIDADA, resultado.estado)
    }

    @Test
    fun `ubicacion reciente se detecta por timestamp`() {
        val reciente = LocationSnapshot(0.0, 0.0, timestampMillis = System.currentTimeMillis())
        val antigua = LocationSnapshot(0.0, 0.0, timestampMillis = System.currentTimeMillis() - 120_000)

        assertTrue(reciente.esReciente(60_000))
        assertFalse(antigua.esReciente(60_000))
    }
}
