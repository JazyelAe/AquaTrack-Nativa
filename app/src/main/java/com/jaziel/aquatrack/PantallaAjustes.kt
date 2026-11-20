package com.jaziel.aquatrack

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Pantalla "Ajustes": meta diaria, recordatorio periódico y prueba de notificación. */
@Composable
fun PantallaAjustes(almacen: Almacen) {
    val context = LocalContext.current
    var meta by remember { mutableIntStateOf(almacen.meta) }
    var activo by remember { mutableStateOf(almacen.recordatorioActivo) }
    var horas by remember { mutableIntStateOf(almacen.horasRecordatorio) }
    var aviso by remember { mutableStateOf("") }
    var probarDespuesDelPermiso by remember { mutableStateOf(false) }

    fun encender() {
        activo = true
        almacen.recordatorioActivo = true
        Recordatorio.programar(context, horas)
        aviso = "Recordatorio activado cada $horas h."
    }

    fun probar() = Recordatorio.mostrar(context, Recordatorio.textoPendiente(almacen))

    // Diálogo del sistema para pedir el permiso de notificaciones (Android 13+).
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (!concedido) {
            activo = false
            almacen.recordatorioActivo = false
            aviso = "Sin permiso de notificaciones: la app funciona, pero no podrá recordarte."
        } else if (probarDespuesDelPermiso) {
            probar()
        } else {
            encender()
        }
        probarDespuesDelPermiso = false
    }

    fun necesitaPermiso() = Build.VERSION.SDK_INT >= 33 && !Recordatorio.tienePermiso(context)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Meta diaria", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("$meta ml", style = MaterialTheme.typography.headlineSmall)
        Slider(
            value = meta.toFloat(),
            onValueChange = { meta = (it / 250f).roundToInt() * 250 },
            onValueChangeFinished = { almacen.meta = meta },
            valueRange = 1000f..5000f,
            steps = 15 // de 250 en 250 ml
        )

        HorizontalDivider()

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Recordatorio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Notificación para tomar agua")
            }
            Switch(
                checked = activo,
                onCheckedChange = { encendido ->
                    if (!encendido) {
                        activo = false
                        almacen.recordatorioActivo = false
                        Recordatorio.cancelar(context)
                        aviso = "Recordatorio desactivado."
                    } else if (necesitaPermiso()) {
                        pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        encender()
                    }
                }
            )
        }

        Text("Cada cuántas horas:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3).forEach { opcion ->
                FilterChip(
                    selected = horas == opcion,
                    onClick = {
                        horas = opcion
                        almacen.horasRecordatorio = opcion
                        if (activo) encender()
                    },
                    label = { Text("$opcion h") }
                )
            }
        }

        Button(onClick = {
            if (necesitaPermiso()) {
                probarDespuesDelPermiso = true
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                probar()
                aviso = "Notificación de prueba enviada."
            }
        }) { Text("Probar notificación") }

        if (aviso.isNotEmpty()) {
            Text(aviso, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
