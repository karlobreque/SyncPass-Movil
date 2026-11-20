package cl.kcorp.syncpass.data.auth

/**
 * Acceso de demostración sin AWS Cognito ni Supabase.
 *
 * Permite abrir cada panel con las cuentas ficticias sembradas en Room para
 * grabar la demo y probar todos los flujos. No reemplaza al login real: cuando
 * Cognito está disponible se sigue usando el formulario institucional.
 */
object DemoAuth {

    data class DemoAccount(
        val role: Role,
        val email: String,
        val name: String,
        val description: String,
    )

    val accounts: List<DemoAccount> = listOf(
        DemoAccount(
            role = Role.ADMINISTRADOR,
            email = "pinkyfloresflores@gmail.com",
            name = "Administrador Demo",
            description = "Gestión de usuarios y roles",
        ),
        DemoAccount(
            role = Role.ADMINISTRADOR,
            email = "coordinador@demo.cl",
            name = "Coordinador General",
            description = "Supervisión de todos los paneles",
        ),
        DemoAccount(
            role = Role.DOCENTE,
            email = "docente@duocuc.cl",
            name = "Docente Demo",
            description = "Alfabetización Digital para Vecinos",
        ),
        DemoAccount(
            role = Role.DOCENTE,
            email = "docente.huerto@demo.cl",
            name = "Docente de Huerto",
            description = "Taller de Huerto Comunitario",
        ),
        DemoAccount(
            role = Role.DOCENTE,
            email = "docente.noche@demo.cl",
            name = "Docente Vespertino",
            description = "Talleres en horario nocturno",
        ),
        DemoAccount(
            role = Role.VOLUNTARIO,
            email = "voluntario@duocuc.cl",
            name = "Voluntario Demo",
            description = "Escáner QR y validación GPS",
        ),
        DemoAccount(
            role = Role.VOLUNTARIO,
            email = "voluntaria@demo.cl",
            name = "Voluntaria de Acreditación",
            description = "Acreditación de asistentes en terreno",
        ),
        DemoAccount(
            role = Role.VOLUNTARIO,
            email = "voluntario.huerto@demo.cl",
            name = "Voluntario de Huerto",
            description = "Apoyo en jornadas comunitarias",
        ),
        DemoAccount(
            role = Role.BENEFICIARIO,
            email = "je.aquino@duouc.cl",
            name = "Beneficiario Demo",
            description = "Inscripciones, pase QR y encuesta",
        ),
        DemoAccount(
            role = Role.BENEFICIARIO,
            email = "ana.rojas@demo.cl",
            name = "Ana Rojas",
            description = "Participante de talleres de alfabetización",
        ),
        DemoAccount(
            role = Role.BENEFICIARIO,
            email = "bruno.silva@demo.cl",
            name = "Bruno Silva",
            description = "Participante de talleres de huerto",
        ),
        DemoAccount(
            role = Role.BENEFICIARIO,
            email = "carla.mendez@demo.cl",
            name = "Carla Méndez",
            description = "Participante de actividades comunitarias",
        ),
    )

    fun accountsFor(role: Role): List<DemoAccount> = accounts.filter { it.role == role }

    fun session(role: Role): UserSession? =
        accountsFor(role).firstOrNull()?.let(::session)

    fun session(account: DemoAccount): UserSession = UserSession(
        tokens = AuthTokens(
            idToken = "demo-id-token",
            accessToken = "demo-access-token",
            refreshToken = "demo-refresh-token",
            username = account.email,
        ),
        role = account.role,
        email = account.email,
        name = account.name,
    )
}
