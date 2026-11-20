package cl.kcorp.syncpass.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FechaValidatorTest {

    @Test
    fun `el mismo dia es un rango valido`() {
        assertTrue(FechaValidator.esRangoValido("2026-11-20", "2026-11-20"))
    }

    @Test
    fun `termino posterior al inicio es valido`() {
        assertTrue(FechaValidator.esRangoValido("2026-11-01", "2026-12-15"))
    }

    @Test
    fun `termino anterior al inicio es invalido con mensaje`() {
        assertFalse(FechaValidator.esRangoValido("2026-12-15", "2026-11-01"))
        assertEquals(
            "La fecha de término no puede ser anterior a la de inicio.",
            FechaValidator.mensajeError("2026-12-15", "2026-11-01"),
        )
    }

    @Test
    fun `evento con la misma hora es valido y con termino anterior no`() {
        assertTrue(FechaValidator.esRangoValido("2026-11-20T10:00", "2026-11-20T10:00"))
        assertFalse(FechaValidator.esRangoValido("2026-11-20T12:00", "2026-11-20T10:00"))
    }

    @Test
    fun `formatos invalidos se rechazan`() {
        assertFalse(FechaValidator.esValido("20-11-2026"))
        assertFalse(FechaValidator.esValido(""))
        assertNull(FechaValidator.mensajeError("2026-11-20", "2026-11-20"))
        assertNotNull(FechaValidator.mensajeError("2026-11-20", ""))
    }

    @Test
    fun `fecha con segundos tambien se reconoce`() {
        assertTrue(FechaValidator.esRangoValido("2026-11-20T10:00:00", "2026-11-20T12:00:00"))
    }

    @Test
    fun `rango con fecha invalida no es valido`() {
        assertFalse(FechaValidator.esRangoValido("", "2026-11-20"))
        assertFalse(FechaValidator.esRangoValido("2026-11-20", "mañana"))
    }

    @Test
    fun `evento dentro del periodo del proyecto es valido`() {
        assertTrue(
            FechaValidator.rangoDentroDePeriodo(
                periodoInicio = "2026-11-01",
                periodoTermino = "2026-12-15",
                fechaInicio = "2026-11-20T10:00",
                fechaTermino = "2026-11-20T12:00",
            ),
        )
        assertNull(
            FechaValidator.mensajeErrorPeriodo(
                "2026-11-01",
                "2026-12-15",
                "2026-11-20T10:00",
                "2026-11-20T12:00",
            ),
        )
    }

    @Test
    fun `evento antes del inicio del proyecto es invalido`() {
        assertFalse(
            FechaValidator.rangoDentroDePeriodo(
                periodoInicio = "2026-11-01",
                periodoTermino = "2026-12-15",
                fechaInicio = "2026-10-30T10:00",
                fechaTermino = "2026-10-30T12:00",
            ),
        )
        assertEquals(
            "El evento no puede comenzar antes del inicio del proyecto (01/11/2026).",
            FechaValidator.mensajeErrorPeriodo(
                "2026-11-01",
                "2026-12-15",
                "2026-10-30T10:00",
                "2026-10-30T12:00",
            ),
        )
    }

    @Test
    fun `evento despues del termino del proyecto es invalido`() {
        assertFalse(
            FechaValidator.rangoDentroDePeriodo(
                periodoInicio = "2026-11-01",
                periodoTermino = "2026-12-15",
                fechaInicio = "2026-12-16T10:00",
                fechaTermino = "2026-12-16T12:00",
            ),
        )
        assertEquals(
            "El evento no puede terminar después del término del proyecto (15/12/2026).",
            FechaValidator.mensajeErrorPeriodo(
                "2026-11-01",
                "2026-12-15",
                "2026-12-16T10:00",
                "2026-12-16T12:00",
            ),
        )
    }

    @Test
    fun `el ultimo dia del proyecto admite eventos en cualquier hora`() {
        assertTrue(
            FechaValidator.rangoDentroDePeriodo(
                periodoInicio = "2026-11-01",
                periodoTermino = "2026-12-15",
                fechaInicio = "2026-12-15T18:00",
                fechaTermino = "2026-12-15T20:00",
            ),
        )
    }

    @Test
    fun `el primer dia del proyecto admite eventos desde la medianoche`() {
        assertTrue(
            FechaValidator.rangoDentroDePeriodo(
                periodoInicio = "2026-11-01",
                periodoTermino = "2026-12-15",
                fechaInicio = "2026-11-01T00:00",
                fechaTermino = "2026-11-01T08:00",
            ),
        )
    }
}
