package cl.kcorp.syncpass.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.kcorp.syncpass.data.location.LocationValidator
import cl.kcorp.syncpass.data.model.DocenteRef
import cl.kcorp.syncpass.data.model.Proyecto
import cl.kcorp.syncpass.data.model.UbicacionesChile

/** Selector simple de proyecto, para asociar eventos sin salir de la pestaña. */
@Composable
fun ProyectoDropdown(
    proyectos: List<Proyecto>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Proyecto",
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = proyectos.firstOrNull { it.idProyecto == selectedId }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = proyectos.isNotEmpty(),
        ) {
            Text(
                text = selected?.let { "$label: ${it.titulo}" } ?: "Selecciona un proyecto",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            proyectos.forEach { proyecto ->
                DropdownMenuItem(
                    text = {
                        Text(proyecto.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    onClick = {
                        onSelect(proyecto.idProyecto)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Selector de una opción cerrada, para evitar valores escritos a mano. */
@Composable
private fun OpcionDropdown(
    opciones: List<String>,
    seleccionada: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    placeholder: String = "Selecciona una opción",
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && opciones.isNotEmpty(),
        ) {
            Text(
                text = seleccionada?.let { "$label: $it" } ?: placeholder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(opcion)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Región y comuna del recinto, con listas dependientes. */
@Composable
fun RegionComunaFields(
    region: String?,
    comuna: String?,
    onRegionChange: (String?) -> Unit,
    onComunaChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val comunas = UbicacionesChile.comunasDe(region)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OpcionDropdown(
            opciones = UbicacionesChile.regiones,
            seleccionada = region,
            onSelect = { nueva ->
                onRegionChange(nueva)
                if (comuna !in UbicacionesChile.comunasDe(nueva)) {
                    onComunaChange(null)
                }
            },
            label = "Región",
            placeholder = "Selecciona la región",
            modifier = Modifier.fillMaxWidth(),
        )
        OpcionDropdown(
            opciones = comunas,
            seleccionada = comuna,
            onSelect = onComunaChange,
            label = "Comuna",
            placeholder = if (region == null) "Primero elige la región" else "Selecciona la comuna",
            modifier = Modifier.fillMaxWidth(),
            enabled = region != null,
        )
    }
}

/** Radio de validación restringido a los valores permitidos. */
@Composable
fun RadioValidacionDropdown(
    radioMetros: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    OpcionDropdown(
        opciones = LocationValidator.RADIOS_VALIDOS.map { "$it m" },
        seleccionada = LocationValidator.normalizarRadio(radioMetros).let { "$it m" },
        onSelect = { valor -> onSelect(valor.filter { it.isDigit() }.toInt()) },
        label = "Radio de validación",
        modifier = modifier,
    )
}

/** Selección múltiple de docentes a cargo de un proyecto. */
@Composable
fun DocentesMultiSelect(
    docentes: List<DocenteRef>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Docentes a cargo",
) {
    var expanded by remember { mutableStateOf(false) }
    val nombres = docentes
        .filter { it.idDocente in selectedIds }
        .map { it.nombre.ifBlank { it.correo } }
        .filter { it.isNotBlank() }
    val resumen = when {
        nombres.isEmpty() -> "selecciona al menos uno"
        nombres.size == 1 -> nombres.first()
        else -> "${nombres.size} docentes asignados"
    }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = docentes.isNotEmpty(),
        ) {
            Text(
                text = "$label: $resumen",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            docentes.forEach { docente ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = docente.idDocente in selectedIds,
                                onCheckedChange = null,
                            )
                            Text(
                                text = docente.nombre.ifBlank { docente.correo },
                                modifier = Modifier.padding(start = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick = { onToggle(docente.idDocente) },
                )
            }
        }
    }
}
