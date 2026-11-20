package cl.kcorp.syncpass.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.kcorp.syncpass.data.location.LocationProvider
import cl.kcorp.syncpass.data.location.LocationValidator
import cl.kcorp.syncpass.data.model.CoordenadasEvento
import cl.kcorp.syncpass.data.model.CoordenadasParser
import cl.kcorp.syncpass.data.model.Evento
import cl.kcorp.syncpass.data.model.UbicacionEventoValidator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Configura el punto de referencia GPS de un evento ya creado.
 *
 * El alta del evento no exige coordenadas: este diálogo se abre desde la
 * tarjeta del evento cuando el docente ya está en el recinto o cuando consigue
 * un enlace de Google Maps con la ubicación. Existen dos caminos:
 *
 *  1. Leer la ubicación actual del dispositivo (GPS/red).
 *  2. Pegar un enlace de Google Maps o un par `latitud, longitud`.
 *
 * Guardar sin coordenadas es válido: el evento queda sin validación GPS y las
 * asistencias se registran como `SIN_VALIDACION`.
 */
@Composable
fun UbicacionEventoDialog(
    evento: Evento,
    onDismiss: () -> Unit,
    onSave: (latitud: Double?, longitud: Double?, radioMetros: Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var seleccion by remember(evento.idEvento) {
        mutableStateOf(
            if (evento.latitud != null && evento.longitud != null) {
                CoordenadasEvento(evento.latitud, evento.longitud)
            } else {
                null
            },
        )
    }
    var precision by remember(evento.idEvento) { mutableStateOf<Double?>(null) }
    var radio by remember(evento.idEvento) {
        mutableStateOf(LocationValidator.normalizarRadio(evento.radioValidacionMetros))
    }
    var textoCoordenadas by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var capturando by remember { mutableStateOf(false) }

    val capturar: () -> Unit = {
        scope.launch {
            capturando = true
            error = null
            val snapshot = LocationProvider.currentLocation(context)
            capturando = false
            if (snapshot == null) {
                error = "No se pudo obtener tu ubicación. Activa el GPS e intenta de nuevo."
            } else {
                seleccion = CoordenadasEvento(snapshot.latitude, snapshot.longitude)
                precision = snapshot.accuracyMeters
            }
        }
    }

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            capturar()
        } else {
            error = "Sin permiso de ubicación no se puede fijar el punto del evento."
        }
    }

    val advertencia = UbicacionEventoValidator.advertenciaPrecision(radio, precision)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubicación GPS del evento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = evento.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                InfoBanner(
                    "Configurar el GPS no es obligatorio para guardar el evento. " +
                        "Si no lo configuras, las asistencias quedan como SIN VALIDACIÓN GPS.",
                )

                Text(
                    text = "Punto del evento",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val punto = seleccion
                if (punto != null) {
                    Text(
                        text = punto.texto,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val precisionActual = precision
                    if (precisionActual != null) {
                        Text(
                            text = "Precisión de la lectura: ±${precisionActual.roundToInt()} m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = "Aún no hay coordenadas configuradas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = {
                        if (LocationProvider.hasPermission(context)) {
                            capturar()
                        } else {
                            permisoLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    },
                    enabled = !capturando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (capturando) "Obteniendo ubicación…" else "Usar mi ubicación actual")
                }

                OutlinedTextField(
                    value = textoCoordenadas,
                    onValueChange = {
                        textoCoordenadas = it
                        error = null
                    },
                    label = { Text("Enlace de Google Maps o coordenadas") },
                    placeholder = { Text("-33.51130, -70.75800") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                )
                OutlinedButton(
                    onClick = {
                        val parseado = CoordenadasParser.parsear(textoCoordenadas)
                        if (parseado == null) {
                            error = if (CoordenadasParser.esEnlaceCorto(textoCoordenadas)) {
                                "Los enlaces cortos (maps.app.goo.gl) requieren internet. " +
                                    "Abre el enlace en el navegador, copia la URL completa y pégala aquí."
                            } else {
                                "No se pudieron leer las coordenadas. Usa el formato " +
                                    "-33.51130, -70.75800 o un enlace de Google Maps que las contenga."
                            }
                        } else {
                            seleccion = parseado
                            precision = null
                            error = null
                        }
                    },
                    enabled = textoCoordenadas.isNotBlank() && !capturando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Leer coordenadas del texto o enlace")
                }

                RadioValidacionDropdown(
                    radioMetros = radio,
                    onSelect = { radio = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (advertencia != null) {
                    Text(
                        text = advertencia,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                error?.let { mensaje ->
                    Text(
                        text = mensaje,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (punto != null) {
                    TextButton(
                        onClick = {
                            seleccion = null
                            precision = null
                            textoCoordenadas = ""
                            error = null
                        },
                    ) {
                        Text(
                            text = "Quitar ubicación GPS",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(seleccion?.latitud, seleccion?.longitud, radio) },
                enabled = !capturando && UbicacionEventoValidator.esPrecisionUtil(precision),
            ) {
                Text(if (seleccion == null) "Guardar sin GPS" else "Guardar ubicación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
