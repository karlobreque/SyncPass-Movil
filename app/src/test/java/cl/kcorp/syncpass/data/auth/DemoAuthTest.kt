package cl.kcorp.syncpass.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAuthTest {

    @Test
    fun `todos los roles tienen una cuenta demo`() {
        val roles = DemoAuth.accounts.map { it.role }.toSet()
        assertEquals(
            setOf(Role.ADMINISTRADOR, Role.DOCENTE, Role.VOLUNTARIO, Role.BENEFICIARIO),
            roles,
        )
    }

    @Test
    fun `cada cuenta demo genera una sesion coherente`() {
        DemoAuth.accounts.forEach { account ->
            val session = DemoAuth.session(account)
            assertEquals(account.role, session.role)
            assertEquals(account.email, session.email)
            assertEquals(account.name, session.name)
            assertEquals(account.email, session.tokens.username)
            assertTrue(session.tokens.accessToken.isNotBlank())
        }
    }

    @Test
    fun `cada rol tiene al menos dos perfiles demo`() {
        val roles = listOf(Role.ADMINISTRADOR, Role.DOCENTE, Role.VOLUNTARIO, Role.BENEFICIARIO)
        roles.forEach { role ->
            assertTrue(
                "El rol ${role.label} debería tener al menos dos perfiles demo",
                DemoAuth.accountsFor(role).size >= 2,
            )
        }
        assertTrue(DemoAuth.accountsFor(Role.VOLUNTARIO).size >= 2)
    }

    @Test
    fun `rol desconocido no genera sesion demo`() {
        assertNull(DemoAuth.session(Role.DESCONOCIDO))
    }
}
