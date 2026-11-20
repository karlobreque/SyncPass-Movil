package cl.kcorp.syncpass.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeUtilsTest {

    @Test
    fun `fecha iso se convierte a millis utc y vuelve igual`() {
        val millis = dateStringToUtcMillis("2026-11-20")
        assertNotNull(millis)
        assertEquals("2026-11-20", millisToUtcDate(millis!!))
    }

    @Test
    fun `fecha invalida devuelve null`() {
        assertNull(dateStringToUtcMillis("20-11-2026"))
        assertNull(dateStringToUtcMillis(""))
        assertNull(dateStringToUtcMillis("2026-13-40"))
    }

    @Test
    fun `extrae hora y minuto con T y con espacio`() {
        assertEquals(10 to 30, parseTimeParts("2026-11-20T10:30"))
        assertEquals(9 to 5, parseTimeParts("2026-11-20 09:05"))
    }

    @Test
    fun `hora invalida usa el valor por defecto`() {
        assertEquals(9 to 0, parseTimeParts(""))
        assertEquals(9 to 0, parseTimeParts("2026-11-20T25:99"))
    }

    @Test
    fun `formatea la hora con dos digitos`() {
        assertEquals("09:05", formatTime(9, 5))
        assertEquals("23:59", formatTime(23, 59))
    }

    @Test
    fun `fecha de hoy tiene formato iso`() {
        assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}").matches(todayIsoDate()))
    }

    @Test
    fun `reloj formatea fecha larga y hora`() {
        val millis = dateStringToUtcMillis("2026-11-20")!!
        assertTrue(formatLongDate(millis).contains("2026"))
        assertTrue(Regex("\\d{2}:\\d{2}:\\d{2}").matches(formatShortTime(millis)))
    }
}
