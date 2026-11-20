package cl.kcorp.syncpass.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.kcorp.syncpass.data.auth.UserSession
import cl.kcorp.syncpass.data.location.LocationValidator
import cl.kcorp.syncpass.data.model.CambioResponsable
import cl.kcorp.syncpass.data.model.CoordenadasEvento
import cl.kcorp.syncpass.data.model.Evento
import cl.kcorp.syncpass.data.model.EventoInput
import cl.kcorp.syncpass.data.model.DocenteRef
import cl.kcorp.syncpass.data.model.FechaValidator
import cl.kcorp.syncpass.data.model.MetricasEvento
import cl.kcorp.syncpass.data.model.Proyecto
import cl.kcorp.syncpass.data.model.RolResponsable
import cl.kcorp.syncpass.data.model.Usuario
import cl.kcorp.syncpass.data.remote.SupabaseConfig
import cl.kcorp.syncpass.ui.components.EmptyMessage
import cl.kcorp.syncpass.ui.components.ErrorBanner
import cl.kcorp.syncpass.ui.components.InfoBanner
import cl.kcorp.syncpass.ui.components.LoadingMessage
import cl.kcorp.syncpass.ui.components.MetricBox
import cl.kcorp.syncpass.ui.components.MetricCard
import cl.kcorp.syncpass.ui.components.PillTabRow
import cl.kcorp.syncpass.ui.components.PanelHeader
import cl.kcorp.syncpass.ui.components.SectionTitle
import cl.kcorp.syncpass.ui.components.QrCodeImage
import cl.kcorp.syncpass.ui.components.StatusChip
import cl.kcorp.syncpass.ui.components.UbicacionEventoDialog
import cl.kcorp.syncpass.ui.components.DateField
import cl.kcorp.syncpass.ui.components.DateTimeField
import cl.kcorp.syncpass.ui.components.DocentesMultiSelect
import cl.kcorp.syncpass.ui.components.ProyectoDropdown
import cl.kcorp.syncpass.ui.components.RadioValidacionDropdown
import cl.kcorp.syncpass.ui.components.RegionComunaFields
import cl.kcorp.syncpass.ui.components.formatDate
import cl.kcorp.syncpass.ui.components.formatDateTime
import cl.kcorp.syncpass.ui.components.formatDateTimeInput
import cl.kcorp.syncpass.ui.components.todayIsoDate

private val docenteTabs = listOf("Resumen", "Proyectos", "Eventos", "Informes")

@Composable
fun DocentePanel(session: UserSession) {
    val viewModel: DocenteViewModel = viewModel(key = "docente-${session.email}")
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(session.email) {
        viewModel.load(session)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
            PillTabRow(
                items = docenteTabs,
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        }

        when (tab) {
            0 -> ResumenTab(
                name = session.name,
                proyectos = state.proyectos,
                eventos = state.eventos,
                metricas = state.metricas,
                selectedProyecto = state.selectedProyectoId,
                loading = state.loading,
                onSelectProyecto = viewModel::selectProyecto,
                onOpenProyectos = { tab = 1 },
                onOpenEventos = { tab = 2 },
                onOpenInformes = { tab = 3 },
            )
            1 -> ProyectosTab(
                usuario = state.usuario,
                proyectos = state.proyectos,
                docentes = state.docentes,
                voluntarios = state.voluntarios,
                selectedProyecto = state.selectedProyectoId,
                eventosPorProyecto = state.eventosPorProyecto,
                loading = state.loading,
                error = state.error,
                onOpenEventos = { idProyecto ->
                    viewModel.selectProyecto(idProyecto)
                    tab = 2
                },
                onVerHistorial = viewModel::cargarHistorial,
                onCreate = viewModel::createProyecto,
                onUpdate = viewModel::updateProyecto,
                onDelete = viewModel::deleteProyecto,
            )
            2 -> EventosTab(
                proyectos = state.proyectos,
                selectedProyectoId = state.selectedProyectoId,
                onSelectProyecto = viewModel::selectProyecto,
                eventos = state.eventos,
                cargandoEventos = state.eventosLoading,
                metricas = state.metricas,
                error = state.error,
                docentes = state.docentes,
                voluntarios = state.voluntarios,
                onVerHistorial = viewModel::cargarHistorial,
                onCreate = viewModel::createEvento,
                onUpdate = viewModel::updateEvento,
                onDelete = viewModel::deleteEvento,
                onConfigurarUbicacion = viewModel::configurarUbicacion,
            )
            else -> InformesTab(
                proyectos = state.proyectos,
                selectedProyecto = state.selectedProyectoId,
                eventos = state.eventos,
                metricas = state.metricas,
                onSelect = viewModel::selectProyecto,
            )
        }

        state.historialTitulo?.let { titulo ->
            HistorialResponsablesDialog(
                titulo = titulo,
                historial = state.historialResponsables,
                onDismiss = viewModel::cerrarHistorial,
            )
        }
    }
}

@Composable
private fun ResumenTab(
    name: String,
    proyectos: List<Proyecto>,
    eventos: List<Evento>,
    metricas: Map<Int, MetricasEvento>,
    selectedProyecto: Int?,
    loading: Boolean,
    onSelectProyecto: (Int) -> Unit,
    onOpenProyectos: () -> Unit,
    onOpenEventos: () -> Unit,
    onOpenInformes: () -> Unit,
) {
    val totalInscritos = metricas.values.sumOf { it.totalInscritos }
    val totalAsistencias = metricas.values.sumOf { it.totalAsistencias }
    val totalEncuestas = metricas.values.sumOf { it.encuestasRespondidas }
    val promedio = if (totalEncuestas > 0) {
        Math.round(
            metricas.values.sumOf { it.satisfaccionPromedio * it.encuestasRespondidas } /
                totalEncuestas * 100.0,
        ) / 100.0
    } else {
        0.0
    }
    val porcentaje = if (totalInscritos > 0) totalAsistencias * 100 / totalInscritos else 0
    val proyecto = proyectos.firstOrNull { it.idProyecto == selectedProyecto } ?: proyectos.firstOrNull()
    val proximo = eventos.minByOrNull { it.fechaInicio }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PanelHeader(
                title = "Hola, ${name.substringBefore(' ')}",
                subtitle = "Este es el estado de tus proyectos y actividades comunitarias.",
            )
        }
        if (loading) {
            item { LoadingMessage("Cargando tu resumen...") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    label = "Proyectos",
                    value = proyectos.size.toString(),
                    foot = "${eventos.size} eventos",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Inscritos",
                    value = totalInscritos.toString(),
                    foot = "Participantes",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    label = "Asistencia",
                    value = "$porcentaje%",
                    foot = "$totalAsistencias registros",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Satisfacción",
                    value = "$promedio/4",
                    foot = "$totalEncuestas encuestas",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle(
                        title = "Próximo evento",
                        action = {
                            TextButton(onClick = onOpenEventos) { Text("Gestionar") }
                        },
                    )
                    if (proximo == null) {
                        EmptyMessage("No hay eventos programados en este proyecto.")
                    } else {
                        Text(
                            text = proximo.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Text(
                            text = "${formatDateTime(proximo.fechaInicio)} · ${proximo.recinto ?: "Sin sala"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatusChip(proximo.estado, proximo.estado != "CANCELADO")
                            StatusChip("${proximo.cuposMaximos} cupos", positive = true)
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle(
                        title = "Proyectos activos",
                        action = { TextButton(onClick = onOpenProyectos) { Text("Ver todos") } },
                    )
                    if (proyectos.isEmpty()) {
                        EmptyMessage("Aún no tienes proyectos. Crea el primero para comenzar.")
                    } else {
                        proyectos.take(4).forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.titulo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${formatDate(p.fechaInicio)} → ${formatDate(p.fechaTermino)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        onSelectProyecto(p.idProyecto)
                                        onOpenEventos()
                                    },
                                ) { Text("Ver") }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle(
                        title = "Actividad reciente",
                        action = { TextButton(onClick = onOpenInformes) { Text("Informes") } },
                    )
                    if (eventos.isEmpty()) {
                        EmptyMessage("Sin eventos registrados.")
                    } else {
                        eventos.take(4).forEach { e ->
                            val m = metricas[e.idEvento]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = e.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = formatDateTime(e.fechaInicio),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = m?.let { "${it.porcentajeAsistencia}%" } ?: "—",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProyectosTab(
    usuario: Usuario?,
    proyectos: List<Proyecto>,
    docentes: List<DocenteRef>,
    voluntarios: List<DocenteRef>,
    selectedProyecto: Int?,
    eventosPorProyecto: Map<Int, Int>,
    loading: Boolean,
    error: String?,
    onOpenEventos: (Int) -> Unit,
    onVerHistorial: (String, Int, String) -> Unit,
    onCreate: (String, String?, String, String, List<Int>, List<Int>) -> Unit,
    onUpdate: (Proyecto, String, String?, String, String, String, List<Int>, List<Int>) -> Unit,
    onDelete: (Proyecto) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Proyecto?>(null) }
    var deleting by remember { mutableStateOf<Proyecto?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PanelHeader("Panel Docente", "Proyectos, eventos, métricas e informes.")
        }

        if (!SupabaseConfig.isConfigured) {
            item {
                InfoBanner(
                    "Modo local activo: proyectos, eventos e informes de demostración guardados en el dispositivo.",
                )
            }
        }
        if (error != null) {
            item { ErrorBanner(error) }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Proyectos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Button(onClick = { showCreate = !showCreate }, enabled = usuario != null) {
                    Text(if (showCreate) "Cancelar" else "+ Crear")
                }
            }
        }

        if (showCreate && usuario != null) {
            item {
                CrearProyectoForm(
                    docentes = docentes,
                    voluntarios = voluntarios,
                    onCancel = { showCreate = false },
                    onCreate = { titulo, descripcion, inicio, termino, docentesIds, voluntariosIds ->
                        onCreate(titulo, descripcion, inicio, termino, docentesIds, voluntariosIds)
                        showCreate = false
                    },
                )
            }
        }

        if (loading) {
            item { LoadingMessage() }
        }
        if (!loading && proyectos.isEmpty()) {
            item { EmptyMessage("No tienes proyectos aún. Crea el primero.") }
        }

        items(proyectos, key = { it.idProyecto }) { proyecto ->
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
                            text = proyecto.titulo,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(
                            text = proyecto.estado,
                            positive = proyecto.estado != "CANCELADO",
                        )
                    }
                    Text(
                        text = proyecto.descripcion ?: "Sin descripción",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = "${formatDate(proyecto.fechaInicio)} → ${formatDate(proyecto.fechaTermino)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    val totalEventos = eventosPorProyecto[proyecto.idProyecto] ?: 0
                    Text(
                        text = if (totalEventos == 0) {
                            "Aún no tiene eventos creados."
                        } else {
                            "$totalEventos evento${if (totalEventos == 1) "" else "s"} creado${if (totalEventos == 1) "" else "s"}."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (totalEventos == 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    if (proyecto.docentes.isNotEmpty()) {
                        Text(
                            text = "Docentes a cargo: ${proyecto.docentes.joinToString { it.nombre }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (proyecto.voluntarios.isNotEmpty()) {
                        Text(
                            text = "Voluntarios a cargo: ${proyecto.voluntarios.joinToString { it.nombre }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        text = "Creado por ${proyecto.creadoPor ?: "—"} · ${formatDateTime(proyecto.creadoEn)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    if (selectedProyecto == proyecto.idProyecto) {
                        Text(
                            text = "Proyecto seleccionado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = { onOpenEventos(proyecto.idProyecto) }) {
                            Text("Ver eventos")
                        }
                        OutlinedButton(onClick = { editing = proyecto }) {
                            Text("Editar")
                        }
                    }
                    TextButton(
                        onClick = {
                            onVerHistorial("PROYECTO", proyecto.idProyecto, proyecto.titulo)
                        },
                    ) {
                        Text("Historial de responsables")
                    }
                    TextButton(onClick = { deleting = proyecto }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editing?.let { proyecto ->
        ProyectoEditDialog(
            proyecto = proyecto,
            docentes = docentes,
            voluntarios = voluntarios,
            onDismiss = { editing = null },
            onSave = { titulo, descripcion, inicio, termino, estado, docentesIds, voluntariosIds ->
                onUpdate(
                    proyecto,
                    titulo,
                    descripcion,
                    inicio,
                    termino,
                    estado,
                    docentesIds,
                    voluntariosIds,
                )
                editing = null
            },
        )
    }

    deleting?.let { proyecto ->
        ConfirmDialog(
            title = "Eliminar proyecto",
            message = "¿Eliminar \"${proyecto.titulo}\" y todos sus eventos?",
            onDismiss = { deleting = null },
            onConfirm = {
                onDelete(proyecto)
                deleting = null
            },
        )
    }
}

@Composable
private fun CrearProyectoForm(
    docentes: List<DocenteRef>,
    voluntarios: List<DocenteRef>,
    onCancel: () -> Unit,
    onCreate: (String, String?, String, String, List<Int>, List<Int>) -> Unit,
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var inicio by remember { mutableStateOf(todayIsoDate()) }
    var termino by remember { mutableStateOf(todayIsoDate()) }
    var docentesIds by remember(docentes) {
        mutableStateOf(docentes.take(1).map { it.idDocente }.toSet())
    }
    var voluntariosIds by remember { mutableStateOf(emptySet<Int>()) }
    val errorFechas = FechaValidator.mensajeError(inicio, termino)
    val puedeGuardar = titulo.isNotBlank() &&
        errorFechas == null &&
        docentesIds.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Nuevo proyecto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                label = "Fecha inicio",
                value = inicio,
                onValueChange = { inicio = it },
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                label = "Fecha término",
                value = termino,
                onValueChange = { termino = it },
                modifier = Modifier.fillMaxWidth(),
            )
            if (errorFechas != null) {
                Text(
                    text = errorFechas,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            DocentesMultiSelect(
                docentes = docentes,
                selectedIds = docentesIds,
                onToggle = { id ->
                    docentesIds = if (id in docentesIds) docentesIds - id else docentesIds + id
                },
            )
            DocentesMultiSelect(
                docentes = voluntarios,
                selectedIds = voluntariosIds,
                onToggle = { id ->
                    voluntariosIds = if (id in voluntariosIds) voluntariosIds - id else voluntariosIds + id
                },
                label = "Voluntarios a cargo",
            )
            Text(
                text = "Quedarás como creador y el proyecto se asignará a los responsables seleccionados.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onCreate(
                            titulo.trim(),
                            descripcion.trim().ifBlank { null },
                            inicio.trim(),
                            termino.trim(),
                            docentesIds.toList(),
                            voluntariosIds.toList(),
                        )
                    },
                    enabled = puedeGuardar,
                ) {
                    Text("Crear proyecto")
                }
                OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun ProyectoEditDialog(
    proyecto: Proyecto,
    docentes: List<DocenteRef>,
    voluntarios: List<DocenteRef>,
    onDismiss: () -> Unit,
    onSave: (String, String?, String, String, String, List<Int>, List<Int>) -> Unit,
) {
    var titulo by remember(proyecto.idProyecto) { mutableStateOf(proyecto.titulo) }
    var descripcion by remember(proyecto.idProyecto) { mutableStateOf(proyecto.descripcion.orEmpty()) }
    var inicio by remember(proyecto.idProyecto) { mutableStateOf(formatDate(proyecto.fechaInicio)) }
    var termino by remember(proyecto.idProyecto) { mutableStateOf(formatDate(proyecto.fechaTermino)) }
    var estado by remember(proyecto.idProyecto) { mutableStateOf(proyecto.estado) }
    var docentesIds by remember(proyecto.idProyecto, docentes) {
        mutableStateOf(
            proyecto.docentes.map { it.idDocente }.toSet()
                .ifEmpty { setOf(proyecto.idCoordinador) },
        )
    }
    var voluntariosIds by remember(proyecto.idProyecto, voluntarios) {
        mutableStateOf(proyecto.voluntarios.map { it.idDocente }.toSet())
    }
    val errorFechas = FechaValidator.mensajeError(inicio, termino)
    val puedeGuardar = titulo.isNotBlank() &&
        errorFechas == null &&
        docentesIds.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar proyecto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") })
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") })
                DateField(label = "Inicio", value = inicio, onValueChange = { inicio = it })
                DateField(label = "Término", value = termino, onValueChange = { termino = it })
                if (errorFechas != null) {
                    Text(
                        text = errorFechas,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                DocentesMultiSelect(
                    docentes = docentes,
                    selectedIds = docentesIds,
                    onToggle = { id ->
                        docentesIds = if (id in docentesIds) docentesIds - id else docentesIds + id
                    },
                )
                DocentesMultiSelect(
                    docentes = voluntarios,
                    selectedIds = voluntariosIds,
                    onToggle = { id ->
                        voluntariosIds = if (id in voluntariosIds) voluntariosIds - id else voluntariosIds + id
                    },
                    label = "Voluntarios a cargo",
                )
                OutlinedTextField(value = estado, onValueChange = { estado = it }, label = { Text("Estado") })
                Text(
                    text = "Creado por ${proyecto.creadoPor ?: "—"} · ${formatDateTime(proyecto.creadoEn)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        titulo.trim(),
                        descripcion.trim().ifBlank { null },
                        inicio.trim(),
                        termino.trim(),
                        estado.trim(),
                        docentesIds.toList(),
                        voluntariosIds.toList(),
                    )
                },
                enabled = puedeGuardar,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun EventosTab(
    proyectos: List<Proyecto>,
    selectedProyectoId: Int?,
    onSelectProyecto: (Int) -> Unit,
    eventos: List<Evento>,
    cargandoEventos: Boolean,
    metricas: Map<Int, MetricasEvento>,
    error: String?,
    docentes: List<DocenteRef>,
    voluntarios: List<DocenteRef>,
    onVerHistorial: (String, Int, String) -> Unit,
    onCreate: (Int, EventoInput) -> Unit,
    onUpdate: (Evento, EventoInput) -> Unit,
    onDelete: (Evento) -> Unit,
    onConfigurarUbicacion: (Evento, Double?, Double?, Int) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Evento?>(null) }
    var deleting by remember { mutableStateOf<Evento?>(null) }
    var qrEvento by remember { mutableStateOf<Evento?>(null) }
    var eventoUbicacion by remember { mutableStateOf<Evento?>(null) }
    val proyectoSeleccionado = proyectos.firstOrNull { it.idProyecto == selectedProyectoId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PanelHeader(
                "Eventos y actividades",
                "Elige el proyecto y asígnale sus eventos. Cada evento queda vinculado a un proyecto.",
            )
        }

        if (error != null) {
            item { ErrorBanner(error) }
        }

        if (proyectos.isEmpty()) {
            item { EmptyMessage("Primero crea un proyecto en la pestaña Proyectos.") }
        } else {
            item {
                ProyectoDropdown(
                    proyectos = proyectos,
                    selectedId = selectedProyectoId,
                    onSelect = onSelectProyecto,
                    label = "Proyecto",
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Eventos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Button(onClick = { showCreate = !showCreate }) {
                        Text(if (showCreate) "Cancelar" else "+ Crear")
                    }
                }
            }

            if (showCreate) {
                item {
                    EventoForm(
                        proyectos = proyectos,
                        selectedProyectoId = selectedProyectoId,
                        onSelectProyecto = onSelectProyecto,
                        docentes = docentes,
                        voluntarios = voluntarios,
                        tituloBoton = "Crear evento",
                        onCancel = { showCreate = false },
                        onSave = { idProyecto, input ->
                            onCreate(idProyecto, input)
                            showCreate = false
                        },
                    )
                }
            }

            if (eventos.isEmpty() && cargandoEventos) {
                item { LoadingMessage("Cargando eventos del proyecto…") }
            }
            if (eventos.isEmpty() && !cargandoEventos) {
                item {
                    EmptyMessage(
                        "El proyecto \"${proyectoSeleccionado?.titulo ?: "seleccionado"}\" aún no tiene eventos creados.",
                    )
                }
            }

            items(eventos, key = { it.idEvento }) { evento ->
                val m = metricas[evento.idEvento]
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
                            StatusChip(
                                text = evento.estado,
                                positive = evento.estado != "CANCELADO",
                            )
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
                        val proyectoEvento = proyectos.firstOrNull { it.idProyecto == evento.idProyecto }
                        val gpsConfigurado = evento.latitud != null && evento.longitud != null
                        Text(
                            text = if (gpsConfigurado) {
                                "GPS: ${CoordenadasEvento(evento.latitud!!, evento.longitud!!).texto} · radio ${evento.radioValidacionMetros} m"
                            } else {
                                "GPS: sin coordenadas · las asistencias quedan SIN VALIDACIÓN"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            text = "Proyecto: ${proyectoEvento?.titulo ?: evento.proyecto?.titulo ?: "Sin proyecto"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            text = "Creado por ${evento.creadoPor ?: "—"} · ${formatDateTime(evento.creadoEn)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        val docentesEvento = evento.responsables
                            .filter { it.rol == RolResponsable.DOCENTE }
                        val voluntariosEvento = evento.responsables
                            .filter { it.rol == RolResponsable.VOLUNTARIO }
                        if (docentesEvento.isNotEmpty()) {
                            Text(
                                text = "Docentes responsables: ${docentesEvento.joinToString { it.nombre }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (voluntariosEvento.isNotEmpty()) {
                            Text(
                                text = "Voluntarios responsables: ${voluntariosEvento.joinToString { it.nombre }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (docentesEvento.isEmpty() && voluntariosEvento.isEmpty()) {
                            Text(
                                text = "Sin responsables asignados.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (evento.descripcion != null) {
                            Text(
                                text = evento.descripcion,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        if (m != null) {
                            Row(
                                modifier = Modifier.padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MetricBox(
                                    label = "Asistencia",
                                    value = "${m.totalAsistencias}/${m.totalInscritos} (${m.porcentajeAsistencia}%)",
                                    modifier = Modifier.weight(1f),
                                )
                                MetricBox(
                                    label = "Encuestas",
                                    value = "${m.encuestasRespondidas}/${m.totalInscritos} (${m.porcentajeEncuestas}%)",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MetricBox(
                                    label = "Satisfacción",
                                    value = "${m.satisfaccionPromedio}/4",
                                    modifier = Modifier.weight(1f),
                                )
                                MetricBox(
                                    label = "Comentarios",
                                    value = m.comentarios.size.toString(),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (m.comentarios.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                                Text(
                                    text = "Comentarios",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                m.comentarios.forEach { comentario ->
                                    Text(
                                        text = "• $comentario",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(onClick = { qrEvento = evento }) { Text("Ver QR") }
                            Button(onClick = { editing = evento }) { Text("Editar") }
                        }
                        TextButton(onClick = { eventoUbicacion = evento }) {
                            Text(if (gpsConfigurado) "Cambiar ubicación GPS" else "Configurar ubicación GPS")
                        }
                        TextButton(
                            onClick = {
                                onVerHistorial("EVENTO", evento.idEvento, evento.nombre)
                            },
                        ) {
                            Text("Historial de responsables")
                        }
                        TextButton(onClick = { deleting = evento }) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    editing?.let { evento ->
        EventoEditDialog(
            evento = evento,
            proyecto = proyectos.firstOrNull { it.idProyecto == evento.idProyecto },
            docentes = docentes,
            voluntarios = voluntarios,
            onDismiss = { editing = null },
            onSave = { input ->
                onUpdate(evento, input)
                editing = null
            },
        )
    }

    deleting?.let { evento ->
        ConfirmDialog(
            title = "Eliminar evento",
            message = "¿Eliminar \"${evento.nombre}\"?",
            onDismiss = { deleting = null },
            onConfirm = {
                onDelete(evento)
                deleting = null
            },
        )
    }

    eventoUbicacion?.let { evento ->
        UbicacionEventoDialog(
            evento = evento,
            onDismiss = { eventoUbicacion = null },
            onSave = { latitud, longitud, radioMetros ->
                onConfigurarUbicacion(evento, latitud, longitud, radioMetros)
                eventoUbicacion = null
            },
        )
    }

    qrEvento?.let { evento ->
        AlertDialog(
            onDismissRequest = { qrEvento = null },
            title = { Text(evento.nombre) },
            text = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    QrCodeImage(content = "syncpass:evento:${evento.idEvento}", size = 220.dp)
                    Text(
                        text = "syncpass:evento:${evento.idEvento}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { qrEvento = null }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun EventoForm(
    proyectos: List<Proyecto>,
    selectedProyectoId: Int?,
    onSelectProyecto: (Int) -> Unit,
    docentes: List<DocenteRef>,
    voluntarios: List<DocenteRef>,
    tituloBoton: String,
    onCancel: () -> Unit,
    onSave: (Int, EventoInput) -> Unit,
) {
    val proyectoId = selectedProyectoId
        ?.takeIf { id -> proyectos.any { it.idProyecto == id } }
        ?: proyectos.firstOrNull()?.idProyecto
    val proyecto = proyectos.firstOrNull { it.idProyecto == proyectoId }

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var inicio by remember { mutableStateOf("${todayIsoDate()}T09:00") }
    var termino by remember { mutableStateOf("${todayIsoDate()}T11:00") }
    var cupos by remember { mutableStateOf("25") }
    var recinto by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var region by remember { mutableStateOf<String?>(null) }
    var comuna by remember { mutableStateOf<String?>(null) }
    var radioMetros by remember { mutableStateOf(LocationValidator.DEFAULT_RADIO_METROS) }
    var docentesIds by remember(proyectoId, docentes) {
        mutableStateOf(proyecto?.docentes.orEmpty().map { it.idDocente }.toSet())
    }
    var voluntariosIds by remember(proyectoId) { mutableStateOf(emptySet<Int>()) }
    val errorFechas = FechaValidator.mensajeError(inicio, termino)
    val errorPeriodo = if (errorFechas == null) {
        proyecto?.let {
            FechaValidator.mensajeErrorPeriodo(
                periodoInicio = it.fechaInicio,
                periodoTermino = it.fechaTermino,
                fechaInicio = inicio,
                fechaTermino = termino,
            )
        }
    } else {
        null
    }
    val puedeGuardar = nombre.isNotBlank() &&
        errorFechas == null &&
        errorPeriodo == null &&
        proyectoId != null &&
        (docentesIds.isNotEmpty() || voluntariosIds.isNotEmpty())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Datos del evento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            ProyectoDropdown(
                proyectos = proyectos,
                selectedId = proyectoId,
                onSelect = onSelectProyecto,
                label = "Evento para",
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del evento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
            )
            DateTimeField(
                label = "Inicio",
                value = inicio,
                onValueChange = { inicio = it },
                modifier = Modifier.fillMaxWidth(),
            )
            DateTimeField(
                label = "Término",
                value = termino,
                onValueChange = { termino = it },
                modifier = Modifier.fillMaxWidth(),
            )
            if (errorFechas != null) {
                Text(
                    text = errorFechas,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (proyecto != null) {
                Text(
                    text = "Período del proyecto: ${formatDate(proyecto.fechaInicio)} al ${formatDate(proyecto.fechaTermino)}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (errorPeriodo != null) {
                Text(
                    text = errorPeriodo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cupos,
                    onValueChange = { cupos = it.filter { c -> c.isDigit() } },
                    label = { Text("Cupos") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = recinto,
                    onValueChange = { recinto = it },
                    label = { Text("Recinto/Sala") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            RegionComunaFields(
                region = region,
                comuna = comuna,
                onRegionChange = { region = it },
                onComunaChange = { comuna = it },
                modifier = Modifier.fillMaxWidth(),
            )
            RadioValidacionDropdown(
                radioMetros = radioMetros,
                onSelect = { radioMetros = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "La validación GPS es opcional: puedes configurarla después con " +
                    "“Configurar ubicación GPS” en la tarjeta del evento. Sin coordenadas, " +
                    "la asistencia se registra como SIN VALIDACIÓN GPS.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Responsables del evento",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            DocentesMultiSelect(
                docentes = docentes,
                selectedIds = docentesIds,
                onToggle = { id ->
                    docentesIds = if (id in docentesIds) docentesIds - id else docentesIds + id
                },
                label = "Docentes responsables",
            )
            DocentesMultiSelect(
                docentes = voluntarios,
                selectedIds = voluntariosIds,
                onToggle = { id ->
                    voluntariosIds = if (id in voluntariosIds) voluntariosIds - id else voluntariosIds + id
                },
                label = "Voluntarios responsables",
            )
            if (docentesIds.isEmpty() && voluntariosIds.isEmpty()) {
                Text(
                    "Asigna al menos un docente o voluntario responsable.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val idProyecto = proyectoId
                        if (idProyecto != null) {
                            onSave(
                                idProyecto,
                                EventoInput(
                                    nombre = nombre.trim(),
                                    descripcion = descripcion.trim().ifBlank { null },
                                    fechaInicio = inicio.trim(),
                                    fechaTermino = termino.trim(),
                                    cuposMaximos = cupos.toIntOrNull() ?: 25,
                                    recinto = recinto.trim().ifBlank { null },
                                    direccion = direccion.trim().ifBlank { null },
                                    region = region,
                                    comuna = comuna,
                                    radioValidacionMetros = radioMetros,
                                    estado = "PROGRAMADO",
                                    docentesIds = docentesIds.toList(),
                                    voluntariosIds = voluntariosIds.toList(),
                                ),
                            )
                        }
                    },
                    enabled = puedeGuardar,
                ) { Text(tituloBoton) }
                OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun EventoEditDialog(
    evento: Evento,
    proyecto: Proyecto? = null,
    docentes: List<DocenteRef>,
    voluntarios: List<DocenteRef>,
    onDismiss: () -> Unit,
    onSave: (EventoInput) -> Unit,
) {
    var nombre by remember(evento.idEvento) { mutableStateOf(evento.nombre) }
    var descripcion by remember(evento.idEvento) { mutableStateOf(evento.descripcion.orEmpty()) }
    var inicio by remember(evento.idEvento) { mutableStateOf(formatDateTimeInput(evento.fechaInicio)) }
    var termino by remember(evento.idEvento) { mutableStateOf(formatDateTimeInput(evento.fechaTermino)) }
    var cupos by remember(evento.idEvento) { mutableStateOf(evento.cuposMaximos.toString()) }
    var recinto by remember(evento.idEvento) { mutableStateOf(evento.recinto.orEmpty()) }
    var direccion by remember(evento.idEvento) { mutableStateOf(evento.direccion.orEmpty()) }
    var region by remember(evento.idEvento) { mutableStateOf(evento.region) }
    var comuna by remember(evento.idEvento) { mutableStateOf(evento.comuna) }
    var radioMetros by remember(evento.idEvento) {
        mutableStateOf(LocationValidator.normalizarRadio(evento.radioValidacionMetros))
    }
    var estado by remember(evento.idEvento) { mutableStateOf(evento.estado) }
    var docentesIds by remember(evento.idEvento, docentes) {
        mutableStateOf(
            evento.responsables
                .filter { it.rol == RolResponsable.DOCENTE }
                .map { it.idDocente }
                .toSet(),
        )
    }
    var voluntariosIds by remember(evento.idEvento, voluntarios) {
        mutableStateOf(
            evento.responsables
                .filter { it.rol == RolResponsable.VOLUNTARIO }
                .map { it.idDocente }
                .toSet(),
        )
    }
    val errorFechas = FechaValidator.mensajeError(inicio, termino)
    val errorPeriodo = if (errorFechas == null) {
        proyecto?.let {
            FechaValidator.mensajeErrorPeriodo(
                periodoInicio = it.fechaInicio,
                periodoTermino = it.fechaTermino,
                fechaInicio = inicio,
                fechaTermino = termino,
            )
        }
    } else {
        null
    }
    val puedeGuardar = nombre.isNotBlank() &&
        errorFechas == null &&
        errorPeriodo == null &&
        (docentesIds.isNotEmpty() || voluntariosIds.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar evento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Proyecto: ${proyecto?.titulo ?: evento.proyecto?.titulo ?: "Sin proyecto"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (proyecto != null) {
                    Text(
                        text = "Período del proyecto: ${formatDate(proyecto.fechaInicio)} al ${formatDate(proyecto.fechaTermino)}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") })
                DateTimeField(label = "Inicio", value = inicio, onValueChange = { inicio = it })
                DateTimeField(label = "Término", value = termino, onValueChange = { termino = it })
                if (errorFechas != null) {
                    Text(
                        text = errorFechas,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (errorPeriodo != null) {
                    Text(
                        text = errorPeriodo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedTextField(
                    value = cupos,
                    onValueChange = { cupos = it.filter { c -> c.isDigit() } },
                    label = { Text("Cupos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(value = recinto, onValueChange = { recinto = it }, label = { Text("Recinto/Sala") })
                OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text("Dirección") })
                RegionComunaFields(
                    region = region,
                    comuna = comuna,
                    onRegionChange = { region = it },
                    onComunaChange = { comuna = it },
                )
                RadioValidacionDropdown(
                    radioMetros = radioMetros,
                    onSelect = { radioMetros = it },
                )
                Text(
                    text = if (evento.latitud != null && evento.longitud != null) {
                        "Validación GPS activa: el evento ya tiene coordenadas configuradas."
                    } else {
                        "Validación GPS opcional: aún no hay coordenadas configuradas."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Responsables del evento",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                DocentesMultiSelect(
                    docentes = docentes,
                    selectedIds = docentesIds,
                    onToggle = { id ->
                        docentesIds = if (id in docentesIds) docentesIds - id else docentesIds + id
                    },
                    label = "Docentes responsables",
                )
                DocentesMultiSelect(
                    docentes = voluntarios,
                    selectedIds = voluntariosIds,
                    onToggle = { id ->
                        voluntariosIds = if (id in voluntariosIds) voluntariosIds - id else voluntariosIds + id
                    },
                    label = "Voluntarios responsables",
                )
                if (docentesIds.isEmpty() && voluntariosIds.isEmpty()) {
                    Text(
                        "Asigna al menos un docente o voluntario responsable.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedTextField(value = estado, onValueChange = { estado = it }, label = { Text("Estado") })
                Text(
                    text = "Creado por ${evento.creadoPor ?: "—"} · ${formatDateTime(evento.creadoEn)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        EventoInput(
                            nombre = nombre.trim(),
                            descripcion = descripcion.trim().ifBlank { null },
                            fechaInicio = inicio.trim(),
                            fechaTermino = termino.trim(),
                            cuposMaximos = cupos.toIntOrNull() ?: 25,
                            recinto = recinto.trim().ifBlank { null },
                            direccion = direccion.trim().ifBlank { null },
                            region = region,
                            comuna = comuna,
                            latitud = evento.latitud,
                            longitud = evento.longitud,
                            radioValidacionMetros = radioMetros,
                            estado = estado.trim(),
                            docentesIds = docentesIds.toList(),
                            voluntariosIds = voluntariosIds.toList(),
                        ),
                    )
                },
                enabled = puedeGuardar,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun InformesTab(
    proyectos: List<Proyecto>,
    selectedProyecto: Int?,
    eventos: List<Evento>,
    metricas: Map<Int, MetricasEvento>,
    onSelect: (Int) -> Unit,
) {
    var informe by remember { mutableStateOf("") }
    var guardado by remember { mutableStateOf(false) }
    val proyecto = proyectos.firstOrNull { it.idProyecto == selectedProyecto }
    val totalInscritos = metricas.values.sumOf { it.totalInscritos }
    val totalAsistencias = metricas.values.sumOf { it.totalAsistencias }
    val totalEncuestas = metricas.values.sumOf { it.encuestasRespondidas }
    val promedio = if (totalEncuestas > 0) {
        Math.round(metricas.values.sumOf { it.satisfaccionPromedio * it.encuestasRespondidas } / totalEncuestas * 100.0) / 100.0
    } else {
        0.0
    }
    val comentarios = metricas.values.flatMap { it.comentarios }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PanelHeader("Informes de impacto", proyecto?.titulo ?: "Selecciona un proyecto.")
        }

        if (proyectos.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(proyectos, key = { it.idProyecto }) { p ->
                        if (p.idProyecto == selectedProyecto) {
                            Button(onClick = { onSelect(p.idProyecto) }) {
                                Text(p.titulo, maxLines = 1)
                            }
                        } else {
                            OutlinedButton(onClick = { onSelect(p.idProyecto) }) {
                                Text(p.titulo, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Resumen del proyecto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    InfoLine("Eventos", eventos.size.toString())
                    InfoLine("Inscritos", totalInscritos.toString())
                    InfoLine("Asistencias", totalAsistencias.toString())
                    InfoLine("Encuestas respondidas", totalEncuestas.toString())
                    InfoLine("Satisfacción promedio", "$promedio/4")
                    if (comentarios.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Comentarios", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        comentarios.forEach { c ->
                            Text(
                                "• $c",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = informe,
                onValueChange = { informe = it; guardado = false },
                label = { Text("Redacta el informe de impacto") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
            )
        }
        item {
            Button(onClick = { guardado = true }) { Text("Guardar informe") }
            if (guardado) {
                Text(
                    "Informe guardado en esta sesión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HistorialResponsablesDialog(
    titulo: String,
    historial: List<CambioResponsable>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de responsables") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (historial.isEmpty()) {
                    Text(
                        text = "Aún no hay cambios de responsables registrados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    historial.forEach { cambio ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${cambio.accion} · ${cambio.rol}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (cambio.accion == "ELIMINADO") {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                )
                                Text(
                                    text = cambio.nombreUsuario,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = formatDateTime(cambio.fecha),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                cambio.actor?.let {
                                    Text(
                                        text = "Registrado por $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}


