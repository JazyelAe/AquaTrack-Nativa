package com.jaziel.aquatrack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Pantalla "Hoy": anillo de progreso, botones para registrar y lista de tomas del día. */
@Composable
fun PantallaHoy(almacen: Almacen) {
    val context = LocalContext.current
    val meta = almacen.meta
    var tomas by remember { mutableStateOf(almacen.tomasDe(almacen.hoy())) }
    var pedirCantidad by remember { mutableStateOf(false) }
    var felicitar by remember { mutableStateOf(false) }

    val total = tomas.sumOf { it.ml }
    val progreso = (total.toFloat() / meta).coerceIn(0f, 1f)

    fun guardar(nuevas: List<Toma>) {
        tomas = nuevas
        almacen.guardarTomas(almacen.hoy(), nuevas)
    }

    fun agregar(ml: Int) {
        guardar(tomas + Toma(almacen.horaActual(), ml))
        // Al llegar a la meta por primera vez en el día: vibración + felicitación.
        if (total + ml >= meta && !almacen.yaFelicitadoHoy()) {
            almacen.marcarFelicitado()
            Recordatorio.vibrar(context)
            felicitar = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnilloProgreso(progreso = progreso, total = total, meta = meta)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { agregar(250) }) { Text("+250 ml") }
            Button(onClick = { agregar(500) }) { Text("+500 ml") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { pedirCantidad = true }) { Text("Otra cantidad") }
            TextButton(onClick = { guardar(tomas.dropLast(1)) }, enabled = tomas.isNotEmpty()) {
                Text("Deshacer")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Tomas de hoy (${tomas.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider()
        if (tomas.isEmpty()) {
            Text("Aún no registras agua hoy. ¡Empieza con un vaso!", modifier = Modifier.padding(16.dp))
        }
        LazyColumn(Modifier.weight(1f)) {
            items(tomas.reversed()) { toma ->
                ListItem(
                    headlineContent = { Text("${toma.ml} ml") },
                    supportingContent = { Text("a las ${toma.hora}") }
                )
            }
        }
    }

    if (pedirCantidad) {
        DialogoCantidad(
            alAceptar = { ml -> agregar(ml); pedirCantidad = false },
            alCancelar = { pedirCantidad = false }
        )
    }
    if (felicitar) {
        AlertDialog(
            onDismissRequest = { felicitar = false },
            title = { Text("¡Meta cumplida! 💧") },
            text = { Text("Llegaste a tus $meta ml de hoy. ¡Muy bien!") },
            confirmButton = { TextButton(onClick = { felicitar = false }) { Text("Genial") } }
        )
    }
}

/** Anillo circular dibujado con Canvas. */
@Composable
fun AnilloProgreso(progreso: Float, total: Int, meta: Int) {
    val colorFondo = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val colorAvance = MaterialTheme.colorScheme.primary
    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val trazo = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            drawArc(colorFondo, 0f, 360f, useCenter = false, style = trazo)
            drawArc(colorAvance, -90f, 360f * progreso, useCenter = false, style = trazo)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$total ml", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("de $meta ml · ${(progreso * 100).toInt()} %")
        }
    }
}

/** Diálogo para escribir una cantidad personalizada (50 a 2000 ml). */
@Composable
fun DialogoCantidad(alAceptar: (Int) -> Unit, alCancelar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    val ml = texto.toIntOrNull()
    val valido = ml != null && ml in 50..2000

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("¿Cuánta agua tomaste?") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { nuevo -> texto = nuevo.filter { it.isDigit() }.take(4) },
                label = { Text("Mililitros") },
                singleLine = true,
                isError = texto.isNotEmpty() && !valido,
                supportingText = { Text("Entre 50 y 2000 ml") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (ml != null) alAceptar(ml) }, enabled = valido) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}
