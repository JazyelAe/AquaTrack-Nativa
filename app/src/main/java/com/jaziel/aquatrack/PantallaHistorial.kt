package com.jaziel.aquatrack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale

/** Pantalla "Historial": total de los últimos 7 días comparado con la meta actual. */
@Composable
fun PantallaHistorial(almacen: Almacen) {
    val meta = almacen.meta
    val dias = remember { almacen.ultimosDias(7) }
    val cumplidos = dias.count { it.second >= meta }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Últimos 7 días", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Meta cumplida $cumplidos de 7 días · meta actual $meta ml")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dias) { (fecha, total) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(fechaBonita(fecha, esHoy = fecha == almacen.hoy()), fontWeight = FontWeight.Bold)
                            Text(if (total >= meta) "$total ml ✔" else "$total ml")
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (total.toFloat() / meta).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/** Convierte "2026-11-20" en "Hoy · jue 20 nov" o "mié 19 nov". */
fun fechaBonita(fecha: String, esHoy: Boolean): String {
    val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val salida = SimpleDateFormat("EEE d MMM", Locale("es", "MX"))
    val texto = entrada.parse(fecha)?.let { salida.format(it) } ?: fecha
    return if (esHoy) "Hoy · $texto" else texto
}
