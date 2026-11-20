package cl.kcorp.syncpass.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UbicacionesChileTest {

    @Test
    fun `el catalogo tiene las dieciseis regiones`() {
        assertEquals(16, UbicacionesChile.regiones.size)
        assertEquals(16, UbicacionesChile.regiones.toSet().size)
    }

    @Test
    fun `cada region tiene comunas no vacias`() {
        UbicacionesChile.regiones.forEach { region ->
            val comunas = UbicacionesChile.comunasDe(region)
            assertTrue("La región $region no tiene comunas", comunas.isNotEmpty())
            assertTrue(
                "La región $region tiene comunas en blanco",
                comunas.all { it.isNotBlank() },
            )
        }
    }

    @Test
    fun `una region desconocida o vacia no tiene comunas`() {
        assertTrue(UbicacionesChile.comunasDe("Región Inventada").isEmpty())
        assertTrue(UbicacionesChile.comunasDe(null).isEmpty())
    }

    @Test
    fun `la region metropolitana incluye Maipu`() {
        assertTrue(
            UbicacionesChile.comunasDe("Metropolitana de Santiago").contains("Maipú"),
        )
    }
}
