package cl.kcorp.syncpass.ui.panels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.kcorp.syncpass.data.location.LocationSnapshot
import cl.kcorp.syncpass.data.location.LocationValidator
import cl.kcorp.syncpass.data.location.ValidacionUbicacion
import cl.kcorp.syncpass.data.model.Evento
import cl.kcorp.syncpass.data.model.Inscrito
import cl.kcorp.syncpass.data.model.MetricasEvento
import cl.kcorp.syncpass.data.repository.SyncPassRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VoluntarioView { EVENTOS, INSCRITOS, SCANNER }

data class VoluntarioUiState(
    val view: VoluntarioView = VoluntarioView.EVENTOS,
    val eventos: List<Evento> = emptyList(),
    val metricas: Map<Int, MetricasEvento> = emptyMap(),
    val selectedEvento: Evento? = null,
    val inscritos: List<Inscrito> = emptyList(),
    val loading: Boolean = true,
    val scanStatus: String? = null,
    val scanResult: String? = null,
    val ubicacion: LocationSnapshot? = null,
    val validacion: ValidacionUbicacion? = null,
)

class VoluntarioViewModel : ViewModel() {

    private val _state = MutableStateFlow(VoluntarioUiState())
    val state: StateFlow<VoluntarioUiState> = _state.asStateFlow()

    private var processing = false

    fun loadEventos() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val eventos = SyncPassRepository.getEventos()
            val metricas = eventos.associate { evento ->
                evento.idEvento to SyncPassRepository.getMetricasEvento(evento.idEvento)
            }
            _state.update { it.copy(eventos = eventos, metricas = metricas, loading = false) }
        }
    }

    fun openInscritos(evento: Evento) {
        _state.update {
            it.copy(
                view = VoluntarioView.INSCRITOS,
                selectedEvento = evento,
                scanStatus = null,
                scanResult = null,
            )
        }
        loadInscritos(evento.idEvento)
    }

    fun openScanner(evento: Evento) {
        _state.update {
            it.copy(
                view = VoluntarioView.SCANNER,
                selectedEvento = evento,
                scanStatus = null,
                scanResult = null,
                ubicacion = null,
                validacion = null,
            )
        }
        loadInscritos(evento.idEvento)
    }

    fun backToEventos() {
        _state.update {
            it.copy(
                view = VoluntarioView.EVENTOS,
                scanStatus = null,
                scanResult = null,
                ubicacion = null,
                validacion = null,
            )
        }
    }

    fun refreshInscritos() {
        _state.value.selectedEvento?.let { loadInscritos(it.idEvento) }
    }

    /**
     * Actualiza la lectura GPS mostrada en el escáner y recalcula la validación
     * contra las coordenadas del evento seleccionado.
     */
    fun actualizarUbicacion(ubicacion: LocationSnapshot?) {
        val evento = _state.value.selectedEvento
        val validacion = evento?.let {
            LocationValidator.validar(
                eventoLatitud = it.latitud,
                eventoLongitud = it.longitud,
                radioMetros = it.radioValidacionMetros,
                ubicacion = ubicacion,
            )
        }
        _state.update { it.copy(ubicacion = ubicacion, validacion = validacion) }
    }

    fun processCode(code: String, ubicacion: LocationSnapshot?, registradoPor: String? = null) {
        if (processing) return
        val evento = _state.value.selectedEvento
        _state.update { it.copy(scanResult = code, ubicacion = ubicacion) }

        if (evento == null) {
            _state.update { it.copy(scanStatus = "❌ Selecciona un evento primero") }
            return
        }

        val parts = code.split(":")
        if (parts.size != 4 || parts[0] != "syncpass" || parts[1] != "asistencia") {
            _state.update { it.copy(scanStatus = "❌ QR no válido") }
            return
        }
        val idUsuario = parts[2].toIntOrNull()
        val idEvento = parts[3].toIntOrNull()
        if (idUsuario == null || idEvento == null) {
            _state.update { it.copy(scanStatus = "❌ QR no válido") }
            return
        }
        if (idEvento != evento.idEvento) {
            _state.update { it.copy(scanStatus = "❌ El QR no corresponde a este evento") }
            return
        }

        val validacion = LocationValidator.validar(
            eventoLatitud = evento.latitud,
            eventoLongitud = evento.longitud,
            radioMetros = evento.radioValidacionMetros,
            ubicacion = ubicacion,
        )
        _state.update { it.copy(validacion = validacion) }

        if (!validacion.permiteRegistro) {
            _state.update { it.copy(scanStatus = "❌ ${validacion.mensaje}") }
            return
        }

        processing = true
        viewModelScope.launch {
            try {
                _state.update { it.copy(scanStatus = "Procesando...") }
                val lista = _state.value.inscritos.ifEmpty {
                    SyncPassRepository.getInscritosByEvento(idEvento)
                }
                val inscripcion = lista.firstOrNull { it.idUsuario == idUsuario }
                    ?: SyncPassRepository.getInscritosByEvento(idEvento)
                        .firstOrNull { it.idUsuario == idUsuario }

                if (inscripcion == null) {
                    _state.update { it.copy(scanStatus = "❌ No está en la lista de inscritos") }
                    return@launch
                }

                val resultado = SyncPassRepository.registrarAsistencia(
                    idInscripcion = inscripcion.idInscripcion,
                    latitud = ubicacion?.latitude,
                    longitud = ubicacion?.longitude,
                    estadoValidacion = validacion.estadoAsistencia,
                    registradoPor = registradoPor,
                )
                if (resultado.isSuccess) {
                    val sincronizado = resultado.getOrNull() == true
                    val encabezado = if (sincronizado) {
                        "✅ Asistencia confirmada"
                    } else {
                        "✅ Asistencia guardada. Se sincronizará al recuperar conexión."
                    }
                    val mensaje = "$encabezado\n${validacion.mensaje}"
                    val inscritosActualizados = SyncPassRepository.getInscritosByEvento(idEvento)
                    val metricasActualizadas = SyncPassRepository.getMetricasEvento(idEvento)
                    _state.update {
                        it.copy(
                            scanStatus = mensaje,
                            inscritos = inscritosActualizados,
                            metricas = it.metricas + (idEvento to metricasActualizadas),
                        )
                    }
                    delay(2200)
                    _state.update {
                        it.copy(
                            scanStatus = null,
                            scanResult = null,
                            view = VoluntarioView.EVENTOS,
                            ubicacion = null,
                            validacion = null,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            scanStatus = "❌ ${resultado.exceptionOrNull()?.localizedMessage ?: "Error al registrar"}",
                        )
                    }
                }
            } finally {
                processing = false
            }
        }
    }

    private fun loadInscritos(idEvento: Int) {
        viewModelScope.launch {
            val inscritos = SyncPassRepository.getInscritosByEvento(idEvento)
            _state.update { it.copy(inscritos = inscritos) }
        }
    }
}
