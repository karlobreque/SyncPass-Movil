package cl.kcorp.syncpass.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.kcorp.syncpass.data.remote.SupabaseConfig
import cl.kcorp.syncpass.data.repository.SyncPassRepository
import cl.kcorp.syncpass.data.sync.SyncScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val pendingSync: Int = 0,
    val syncing: Boolean = false,
    val syncMessage: String? = null,
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                syncNowInternal()
                delay(30_000)
            }
        }
    }

    /** Sincronización manual solicitada desde la barra superior. */
    fun syncNow() {
        if (_state.value.syncing) return
        SyncScheduler.syncNow(getApplication())
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, syncMessage = null) }
            val sincronizados = runCatching { SyncPassRepository.syncPending() }.getOrDefault(0)
            val pendientes = runCatching { SyncPassRepository.pendingCount() }
                .getOrDefault(_state.value.pendingSync)
            val mensaje = when {
                !SupabaseConfig.isConfigured ->
                    "Modo local: los datos quedan en el dispositivo hasta configurar Supabase."

                sincronizados > 0 -> "$sincronizados registro(s) sincronizado(s)."
                pendientes > 0 -> "No se pudo sincronizar. Revisa la conexión."
                else -> "No hay registros pendientes."
            }
            _state.update {
                it.copy(
                    syncing = false,
                    pendingSync = pendientes,
                    syncMessage = mensaje,
                )
            }
        }
    }

    private suspend fun syncNowInternal() {
        runCatching { SyncPassRepository.syncPending() }
        val pending = runCatching { SyncPassRepository.pendingCount() }
            .getOrDefault(_state.value.pendingSync)
        _state.update { it.copy(pendingSync = pending) }
    }
}
