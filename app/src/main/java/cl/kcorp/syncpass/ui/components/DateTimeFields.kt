package cl.kcorp.syncpass.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * Campo de fecha gestionado con el calendario de Material 3.
 *
 * El valor se guarda como `yyyy-MM-dd`; el usuario no escribe la fecha a mano,
 * así no puede equivocarse de formato ni de día.
 */
@Composable
fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var showPicker by remember { mutableStateOf(false) }

    PickerTextField(
        label = label,
        value = value,
        onClick = { showPicker = true },
        modifier = modifier,
        supportingText = supportingText,
    )

    if (showPicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = dateStringToUtcMillis(value),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { onValueChange(millisToUtcDate(it)) }
                        showPicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
}

/**
 * Campo de fecha y hora: primero se elige la fecha en el calendario y luego la hora.
 *
 * El valor se guarda como `yyyy-MM-ddTHH:mm`; el usuario no escribe nada a mano.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<String?>(null) }
    val (initialHour, initialMinute) = parseTimeParts(value)

    PickerTextField(
        label = label,
        value = value.replace('T', ' '),
        onClick = { showDatePicker = true },
        modifier = modifier,
        supportingText = supportingText,
    )

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = dateStringToUtcMillis(value.take(10)),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = dateState.selectedDateMillis
                        if (millis != null) {
                            pendingDate = millisToUtcDate(millis)
                            showTimePicker = true
                        }
                        showDatePicker = false
                    },
                ) { Text("Siguiente") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val date = pendingDate
        if (date != null) {
            val timeState = rememberTimePickerState(
                initialHour = initialHour,
                initialMinute = initialMinute,
                is24Hour = true,
            )
            TimePickerDialog(
                onDismissRequest = {
                    showTimePicker = false
                    pendingDate = null
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onValueChange("${date}T${formatTime(timeState.hour, timeState.minute)}")
                            showTimePicker = false
                            pendingDate = null
                        },
                    ) { Text("Aceptar") }
                },
                title = { Text("Hora") },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showTimePicker = false
                            pendingDate = null
                        },
                    ) { Text("Cancelar") }
                },
            ) {
                TimePicker(state = timeState)
            }
        }
    }
}

/**
 * Reloj del dispositivo: fecha larga y hora actual, actualizado cada segundo.
 * Se usa en la cabecera del panel para que el usuario vea la hora de la app.
 */
@Composable
fun LiveClock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = formatLongDate(now),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatShortTime(now),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Cuadro de texto de solo lectura que se abre al tocarlo. */
@Composable
private fun PickerTextField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Toca para elegir") },
            singleLine = true,
            supportingText = supportingText?.let { text -> { Text(text) } },
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa transparente que abre el selector al tocar cualquier parte del cuadro.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClick() },
        )
    }
}
