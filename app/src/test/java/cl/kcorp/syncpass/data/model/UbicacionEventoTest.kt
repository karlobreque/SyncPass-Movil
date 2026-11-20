package cl.kcorp.syncpass.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UbicacionEventoTest {

    // --- Lectura de coordenadas ---

    @Test
    fun `lee un par simple separado por coma`() {
        val coordenadas = CoordenadasParser.parsear("-33.51130, -70.75800")

        assertNotNull(coordenadas)
        assertEquals(-33.51130, coordenadas!!.latitud, 0.000001)
        assertEquals(-70.75800, coordenadas.longitud, 0.000001)
    }

    @Test
    fun `lee un par separado por punto y coma`() {
        val coordenadas = CoordenadasParser.parsear("-33.5113; -70.7580")

        assertNotNull(coordenadas)
        assertEquals(-33.5113, coordenadas!!.latitud, 0.000001)
    }

    @Test
    fun `acepta el signo menos unicode`() {
        val coordenadas = CoordenadasParser.parsear("\u221233.5113, \u221270.7580")

        assertNotNull(coordenadas)
        assertEquals(-33.5113, coordenadas!!.latitud, 0.000001)
        assertEquals(-70.7580, coordenadas.longitud, 0.000001)
    }

    @Test
    fun `lee un enlace de Google Maps con arroba`() {
        val coordenadas = CoordenadasParser.parsear(
            "https://www.google.com/maps/@-33.5113,-70.7580,17z",
        )

        assertNotNull(coordenadas)
        assertEquals(-33.5113, coordenadas!!.latitud, 0.000001)
        assertEquals(-70.7580, coordenadas.longitud, 0.000001)
    }

    @Test
    fun `lee un enlace con parametro q`() {
        val coordenadas = CoordenadasParser.parsear(
            "https://maps.google.com/?q=-33.5113,-70.7580",
        )

        assertNotNull(coordenadas)
        assertEquals(-33.5113, coordenadas!!.latitud, 0.000001)
    }

    @Test
    fun `lee un enlace con parametro ll`() {
        val coordenadas = CoordenadasParser.parsear(
            "https://maps.google.com/?ll=-33.5113,-70.7580&z=17",
        )

        assertNotNull(coordenadas)
        assertEquals(-70.7580, coordenadas!!.longitud, 0.000001)
    }

    @Test
    fun `lee el formato interno 3d y 4d de Google Maps`() {
        val coordenadas = CoordenadasParser.parsear(
            "https://www.google.com/maps/place/X/data=!4m2!3d-33.5113!4d-70.7580",
        )

        assertNotNull(coordenadas)
        assertEquals(-33.5113, coordenadas!!.latitud, 0.000001)
        assertEquals(-70.7580, coordenadas.longitud, 0.000001)
    }

    @Test
    fun `lee el esquema geo`() {
        val coordenadas = CoordenadasParser.parsear("geo:-33.5113,-70.7580")

        assertNotNull(coordenadas)
        assertEquals(-33.5113, coordenadas!!.latitud, 0.000001)
    }

    @Test
    fun `rechaza una latitud fuera de rango`() {
        assertNull(CoordenadasParser.parsear("-91.0, -70.7580"))
    }

    @Test
    fun `rechaza una longitud fuera de rango`() {
        assertNull(CoordenadasParser.parsear("-33.5113, -181.0"))
    }

    @Test
    fun `rechaza un texto sin coordenadas`() {
        assertNull(CoordenadasParser.parsear("Av. Siempre Viva 123, Maipú"))
    }

    @Test
    fun `rechaza un texto vacio o nulo`() {
        assertNull(CoordenadasParser.parsear(""))
        assertNull(CoordenadasParser.parsear(null))
    }

    @Test
    fun `detecta enlaces cortos que requieren internet`() {
        assertTrue(CoordenadasParser.esEnlaceCorto("https://maps.app.goo.gl/abc123"))
        assertFalse(CoordenadasParser.esEnlaceCorto("https://www.google.com/maps/@-33.5,-70.7,17z"))
    }

    // --- Representación y validación ---

    @Test
    fun `formatea el punto con cinco decimales`() {
        assertEquals("-33.51130, -70.75800", CoordenadasEvento(-33.5113, -70.758).texto)
    }

    @Test
    fun `valida el par de coordenadas`() {
        assertTrue(CoordenadasEvento.esParValido(-33.5113, -70.758))
        assertFalse(CoordenadasEvento.esParValido(null, -70.758))
        assertFalse(CoordenadasEvento.esParValido(-95.0, -70.758))
        assertFalse(CoordenadasEvento.esParValido(-33.5113, 200.0))
    }

    // --- Calidad del punto de referencia ---

    @Test
    fun `no advierte si la precision cabe en la mitad del radio`() {
        assertNull(UbicacionEventoValidator.advertenciaPrecision(100, 20.0))
    }

    @Test
    fun `no advierte si no hay precision informada`() {
        assertNull(UbicacionEventoValidator.advertenciaPrecision(20, null))
    }

    @Test
    fun `advierte cuando la precision supera la mitad del radio`() {
        assertNotNull(UbicacionEventoValidator.advertenciaPrecision(20, 30.0))
    }

    @Test
    fun `advierte cuando la precision es insuficiente`() {
        assertNotNull(UbicacionEventoValidator.advertenciaPrecision(100, 260.0))
    }

    @Test
    fun `considera util una precision dentro del maximo`() {
        assertTrue(UbicacionEventoValidator.esPrecisionUtil(null))
        assertTrue(UbicacionEventoValidator.esPrecisionUtil(240.0))
        assertFalse(UbicacionEventoValidator.esPrecisionUtil(260.0))
    }

    @Test
    fun `recomienda un radio que cubre el doble de la precision`() {
        assertEquals(80, UbicacionEventoValidator.radioRecomendado(40.0))
    }

    @Test
    fun `la recomendacion se limita al radio maximo permitido`() {
        assertEquals(100, UbicacionEventoValidator.radioRecomendado(300.0))
    }
}
