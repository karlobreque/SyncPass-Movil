package cl.kcorp.syncpass.ui.panels

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.kcorp.syncpass.data.auth.UserSession
import cl.kcorp.syncpass.data.location.EstadoValidacionUbicacion
import cl.kcorp.syncpass.data.location.LocationProvider
import cl.kcorp.syncpass.data.location.LocationSnapshot
import cl.kcorp.syncpass.data.location.ValidacionUbicacion
import cl.kcorp.syncpass.data.model.Evento
import cl.kcorp.syncpass.data.model.Inscrito
import cl.kcorp.syncpass.data.model.MetricasEvento
import cl.kcorp.syncpass.data.remote.SupabaseConfig
import cl.kcorp.syncpass.ui.components.EmptyMessage
import cl.kcorp.syncpass.ui.components.InfoBanner
import cl.kcorp.syncpass.ui.components.LoadingMessage
import cl.kcorp.syncpass.ui.components.MetricBox
import cl.kcorp.syncpass.ui.components.PanelHeader
import cl.kcorp.syncpass.ui.components.StatusChip
import cl.kcorp.syncpass.ui.components.formatDateTime
import cl.kcorp.syncpass.ui.scanner.QrScannerScreen
import kotlinx.coroutines.launch

@Composable
fun VoluntarioPanel(session: UserSession) {
    val viewModel: VoluntarioViewModel = viewModel(key = "voluntario-${session.email}")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { granted -> granted }) {
            scope.launch {
                viewModel.actualizarUbicacion(LocationProvider.currentLocation(context))
            }
        } else {
            // Sin permiso no hay lectura: se refleja el bloqueo en la tarjeta GPS.
            viewModel.actualizarUbicacion(null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadEventos()
    }

    LaunchedEffect(state.view, state.selectedEvento?.idEvento) {
        val evento = state.selectedEvento
        val needsLocation = evento?.latitud != null && evento.longitud != null
        if (state.view == VoluntarioView.SCANNER && needsLocation) {
            if (!LocationProvider.hasPermission(context)) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            } else {
                viewModel.actualizarUbicacion(LocationProvider.currentLocation(context))
            }
        }
    }

    when (state.view) {
        VoluntarioView.EVENTOS -> EventosVoluntario(
            eventos = state.eventos,
            metricas = state.metricas,
            loading = state.loading,
            onInscritos = viewModel::openInscritos,
            onScanner = viewModel::openScanner,
        )

        VoluntarioView.INSCRITOS -> InscritosVoluntario(
            evento = state.selectedEvento,
            inscritos = state.inscritos,
            onBack = viewModel::backToEventos,
            onRefresh = viewModel::refreshInscritos,
        )

        VoluntarioView.SCANNER -> Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = state.selectedEvento?.nombre ?: "Escanear QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Escáner de asistencia",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = viewModel::backToEventos) { Text("← Volver") }
            }

            GpsStatusCard(
                evento = state.selectedEvento,
                ubicacion = state.ubicacion,
                validacion = state.validacion,
                onRefresh = {
                    scope.launch {
                        viewModel.actualizarUbicacion(LocationProvider.currentLocation(context))
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )

            QrScannerScreen(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(20.dp)),
                onCode = { code ->
                    val evento = state.selectedEvento
                    val needsLocation = evento?.latitud != null && evento.longitud != null
                    scope.launch {
                        val cached = state.ubicacion
                        val location = when {
                            !needsLocation -> null
                            cached != null && cached.esReciente(90_000L) -> cached
                            else -> LocationProvider.currentLocation(context)
                        }
                        viewModel.processCode(
                            code = code,
                            ubicacion = location,
                            registradoPor = session.name.ifBlank { session.email },
                        )
                    }
                },
                onClose = viewModel::backToEventos,
            )

            if (state.scanResult != null || state.scanStatus != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = state.scanStatus ?: "Código escaneado",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        state.scanResult?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventosVoluntario(
    eventos: List<Evento>,
    metricas: Map<Int, MetricasEvento>,
    loading: Boolean,
    onInscritos: (Evento) -> Unit,
    onScanner: (Evento) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { PanelHeader("Panel Voluntario", "Eventos, scanner de asistencia y lista de inscritos.") }
        if (!SupabaseConfig.isConfigured) {
            item {
                InfoBanner(
                    "Modo local activo: se usan datos de demostración guardados en el dispositivo. " +
                        "La sincronización remota se habilitará al configurar Supabase.",
                )
            }
        }
        if (loading) item { LoadingMessage() }
        if (!loading && eventos.isEmpty()) item { EmptyMessage("No hay eventos.") }

        items(eventos, key = { it.idEvento }) { evento ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = evento.nombre,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(text = evento.estado, positive = evento.estado != "CANCELADO")
                    }
                    Text(
                        text = "${formatDateTime(evento.fechaInicio)} · ${evento.recinto ?: "Sin sala"} · Cupos: ${evento.cuposMaximos}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    val ubicacionEvento = listOfNotNull(evento.comuna, evento.region).joinToString(", ")
                    if (ubicacionEvento.isNotBlank()) {
                        Text(
                            text = "Ubicación: $ubicacionEvento",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        text = "Creado por ${evento.creadoPor ?: "—"} · ${formatDateTime(evento.creadoEn)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    val m = metricas[evento.idEvento]
                    if (m != null) {
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MetricBox(
                                label = "Asistentes",
                                value = m.totalAsistencias.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            MetricBox(
                                label = "Faltantes",
                                value = (m.totalInscritos - m.totalAsistencias).coerceAtLeast(0).toString(),
                                modifier = Modifier.weight(1f),
                            )
                            MetricBox(
                                label = "Asistencia",
                                value = "${m.porcentajeAsistencia}%",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { onInscritos(evento) }) {
                            Text("Lista de inscritos")
                        }
                        Button(onClick = { onScanner(evento) }) {
                            Text("Escanear QR")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GpsStatusCard(
    evento: Evento?,
    ubicacion: LocationSnapshot?,
    validacion: ValidacionUbicacion?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val needsLocation = evento?.latitud != null && evento.longitud != null
    val estado = validacion?.estado
    val titulo: String
    val detalle: String
    when {
        !needsLocation -> {
            titulo = "Evento sin coordenadas"
            detalle = "La asistencia se registrará como SIN VALIDACIÓN GPS."
        }

        estado == EstadoValidacionUbicacion.VALIDADA -> {
            titulo = "Ubicación validada"
            detalle = validacion?.mensaje.orEmpty()
        }

        estado == EstadoValidacionUbicacion.FUERA_DE_RADIO -> {
            titulo = "Fuera del radio permitido"
            detalle = validacion?.mensaje.orEmpty()
        }

        estado == EstadoValidacionUbicacion.PRECISION_INSUFICIENTE -> {
            titulo = "Precisión GPS insuficiente"
            detalle = validacion?.mensaje.orEmpty()
        }

        estado == EstadoValidacionUbicacion.SIN_UBICACION -> {
            titulo = "Ubicación no disponible"
            detalle = validacion?.mensaje.orEmpty()
        }

        ubicacion != null -> {
            titulo = "Ubicación obtenida"
            detalle = buildString {
                append("Lat ${"%.5f".format(ubicacion.latitude)}, Lng ${"%.5f".format(ubicacion.longitude)}")
                ubicacion.accuracyMeters?.let { append(" · Precisión ±${it.toInt()} m") }
            }
        }

        else -> {
            titulo = "Obteniendo ubicación…"
            detalle = "Activa el GPS y concede el permiso de ubicación para validar la asistencia."
        }
    }
    val color = when (estado) {
        EstadoValidacionUbicacion.VALIDADA -> MaterialTheme.colorScheme.primary
        EstadoValidacionUbicacion.FUERA_DE_RADIO,
        EstadoValidacionUbicacion.SIN_UBICACION,
        EstadoValidacionUbicacion.PRECISION_INSUFICIENTE,
        -> MaterialTheme.colorScheme.error

        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (needsLocation) {
                TextButton(onClick = onRefresh) { Text("Actualizar GPS") }
            }
        }
    }
}

@Composable
private fun InscritosVoluntario(
    evento: Evento?,
    inscritos: List<Inscrito>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inscritos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = evento?.nombre.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onBack) { Text("← Volver") }
            }
        }

        item {
            OutlinedButton(onClick = onRefresh) { Text("Actualizar") }
        }

        if (inscritos.isEmpty()) {
            item { EmptyMessage("Sin inscritos.") }
        }

        items(inscritos, key = { it.idInscripcion }) { inscrito ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = inscrito.nombre,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${inscrito.correo} · ${inscrito.estado}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (inscrito.asistio) {
                            Text(
                                text = "Registrada por ${inscrito.asistenciaRegistradoPor ?: "—"} · ${formatDateTime(inscrito.asistenciaFecha)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    StatusChip(
                        text = if (inscrito.asistio) "Presente" else "Pendiente",
                        positive = inscrito.asistio,
                    )
                }
            }
        }
    }
}
