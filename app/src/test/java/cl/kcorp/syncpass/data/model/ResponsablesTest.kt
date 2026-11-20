package cl.kcorp.syncpass.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsablesTest {

    @Test
    fun `detecta responsables agregados y eliminados`() {
        val actuales = listOf(
            ClaveResponsable(RolResponsable.DOCENTE, 2),
            ClaveResponsable(RolResponsable.VOLUNTARIO, 3),
        )
        val nuevos = listOf(
            ClaveResponsable(RolResponsable.DOCENTE, 2),
            ClaveResponsable(RolResponsable.VOLUNTARIO, 8),
        )

        val cambios = calcularCambiosResponsables(actuales, nuevos)

        assertEquals(
            setOf(ClaveResponsable(RolResponsable.VOLUNTARIO, 8)),
            cambios.agregados,
        )
        assertEquals(
            setOf(ClaveResponsable(RolResponsable.VOLUNTARIO, 3)),
            cambios.eliminados,
        )
    }

    @Test
    fun `sin cambios no genera historial`() {
        val actuales = listOf(ClaveResponsable(RolResponsable.DOCENTE, 2))
        val cambios = calcularCambiosResponsables(actuales, actuales)

        assertTrue(cambios.agregados.isEmpty())
        assertTrue(cambios.eliminados.isEmpty())
    }

    @Test
    fun `un cambio de rol registra baja y alta`() {
        val actuales = listOf(ClaveResponsable(RolResponsable.DOCENTE, 2))
        val nuevos = listOf(ClaveResponsable(RolResponsable.VOLUNTARIO, 2))

        val cambios = calcularCambiosResponsables(actuales, nuevos)

        assertEquals(setOf(ClaveResponsable(RolResponsable.VOLUNTARIO, 2)), cambios.agregados)
        assertEquals(setOf(ClaveResponsable(RolResponsable.DOCENTE, 2)), cambios.eliminados)
    }
}
