package cl.kcorp.syncpass.data.local

import cl.kcorp.syncpass.data.model.AsistenciaRef
import cl.kcorp.syncpass.data.model.Encuesta
import cl.kcorp.syncpass.data.model.Evento
import cl.kcorp.syncpass.data.model.Inscripcion
import cl.kcorp.syncpass.data.model.Inscrito
import cl.kcorp.syncpass.data.model.Proyecto
import cl.kcorp.syncpass.data.model.ProyectoRef
import cl.kcorp.syncpass.data.model.Usuario
import cl.kcorp.syncpass.data.model.UsuarioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `proyecto conserva sus campos al mapear a Room y volver`() {
        val proyecto = Proyecto(
            idProyecto = 7,
            idCoordinador = 2,
            idUnidad = 1,
            idArea = 3,
            titulo = "Taller",
            descripcion = "Descripción",
            fechaInicio = "2026-01-01",
            fechaTermino = "2026-02-01",
            estado = "EN_CURSO",
            creadoPor = "Docente Demo",
            creadoEn = "2026-01-01T09:00:00",
        )

        assertEquals(proyecto, proyecto.toEntity().toModel())
    }

    @Test
    fun `evento conserva coordenadas radio y proyecto`() {
        val evento = Evento(
            idEvento = 10,
            idProyecto = 7,
            nombre = "Evento",
            descripcion = "Demo",
            fechaInicio = "2026-01-01T10:00:00",
            fechaTermino = "2026-01-01T12:00:00",
            cuposMaximos = 30,
            recinto = "Sede",
            direccion = "Calle 1",
            latitud = -33.5,
            longitud = -70.7,
            radioValidacionMetros = 120,
            estado = "PROGRAMADO",
            proyecto = ProyectoRef("Taller"),
            creadoPor = "Docente Demo",
            creadoEn = "2026-01-01T08:30:00",
        )

        val entity = evento.toEntity()
        assertEquals("Taller", entity.proyectoTitulo)
        assertEquals(evento, entity.toModel())

        assertNull(evento.copy(proyecto = null).toEntity().proyectoTitulo)
        assertNull(evento.copy(proyecto = null).toEntity().toModel().proyecto)
    }

    @Test
    fun `inscripcion mapea asistencia y encuesta en ambos sentidos`() {
        val inscripcion = Inscripcion(
            idInscripcion = 99,
            idUsuario = 4,
            idEvento = 10,
            estado = "INSCRITO",
            fechaInscripcion = "2026-01-01T09:00:00Z",
            asistencia = AsistenciaRef(
                estadoValidacion = "VALIDADA",
                registradoPor = "Voluntario Demo",
                fechaMarcacion = "2026-01-01T10:05:00",
            ),
            encuesta = Encuesta(5, "MUY_UTIL", "Buen taller"),
        )

        val entity = inscripcion.toEntity()
        assertEquals("VALIDADA", entity.asistenciaEstado)
        assertEquals("Voluntario Demo", entity.asistenciaRegistradoPor)
        assertEquals("2026-01-01T10:05:00", entity.asistenciaFecha)
        assertEquals(5, entity.encuestaCalificacion)
        assertEquals("MUY_UTIL", entity.encuestaUtilidad)
        assertEquals("Buen taller", entity.encuestaComentarios)
        assertEquals(inscripcion, entity.toModel())
    }

    @Test
    fun `inscripcion sin asistencia ni encuesta se mantiene vacia`() {
        val entity = InscripcionEntity(
            idInscripcion = 1,
            idUsuario = 4,
            idEvento = 10,
            estado = "INSCRITO",
            fechaInscripcion = null,
            asistenciaEstado = null,
            encuestaCalificacion = null,
            encuestaUtilidad = null,
            encuestaComentarios = null,
        )

        val model = entity.toModel()
        assertNull(model.asistencia)
        assertNull(model.encuesta)
        assertEquals(entity, model.toEntity())
    }

    @Test
    fun `inscrito conserva sus datos`() {
        val inscrito = Inscrito(
            idInscripcion = 1,
            idUsuario = 4,
            idEvento = 10,
            nombre = "Persona Demo",
            correo = "demo@example.cl",
            estado = "INSCRITO",
            asistio = true,
            asistenciaRegistradoPor = "Voluntario Demo",
            asistenciaFecha = "2026-01-01T10:05:00",
        )

        assertEquals(inscrito, inscrito.toEntity().toModel())
    }

    @Test
    fun `usuario y usuario item se mapean con rol`() {
        val usuario = Usuario(
            idUsuario = 4,
            idRol = 4,
            nombreUsuario = "demo",
            nombreMostrar = "Demo",
            correo = "demo@example.cl",
            estado = "ACTIVO",
        )

        val entity = usuario.toEntity("Beneficiario")
        assertEquals("Beneficiario", entity.rolNombre)
        assertEquals(usuario, entity.toModel())

        val item = entity.toItem()
        assertEquals("Beneficiario", item.rol)
        assertEquals(entity, item.toEntity())
    }

    @Test
    fun `usuario item con nombre vacio usa el correo`() {
        val item = UsuarioItem(
            idUsuario = 5,
            nombreUsuario = "",
            nombreMostrar = "Demo",
            correo = "persona@example.cl",
            idRol = 4,
            rol = "Beneficiario",
            estado = "ACTIVO",
        )

        val entity = item.toEntity()
        assertEquals("persona", entity.nombreUsuario)
        assertTrue(entity.correo.contains("@"))
    }
}
