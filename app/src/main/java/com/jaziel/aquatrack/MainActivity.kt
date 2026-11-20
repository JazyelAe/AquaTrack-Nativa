package com.jaziel.aquatrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Recordatorio.crearCanal(this)
        setContent {
            TemaAquaTrack { AquaTrackApp() }
        }
    }
}

/** Colores azul agua; usa el modo oscuro si el sistema lo tiene activado. */
@Composable
fun TemaAquaTrack(contenido: @Composable () -> Unit) {
    val colores = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF7CC4FF), secondary = Color(0xFF5FD4C0))
    } else {
        lightColorScheme(
            primary = Color(0xFF0B7DDA),
            secondary = Color(0xFF0F9D8A),
            background = Color(0xFFF3F9FE),
            surface = Color(0xFFF3F9FE)
        )
    }
    MaterialTheme(colorScheme = colores, content = contenido)
}

/** Estructura principal: barra superior, contenido y navegación inferior con 3 pestañas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquaTrackApp() {
    val context = LocalContext.current
    val almacen = remember { Almacen(context) }
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AquaTrack", fontWeight = FontWeight.Bold)
                        Text("Contador de hidratación", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pestana == 0, onClick = { pestana = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Hoy") }
                )
                NavigationBarItem(
                    selected = pestana == 1, onClick = { pestana = 1 },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("Historial") }
                )
                NavigationBarItem(
                    selected = pestana == 2, onClick = { pestana = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { relleno ->
        Box(Modifier.padding(relleno)) {
            // Cada pantalla vuelve a leer el almacén al abrirse, así siempre muestra datos actuales.
            when (pestana) {
                0 -> PantallaHoy(almacen)
                1 -> PantallaHistorial(almacen)
                else -> PantallaAjustes(almacen)
            }
        }
    }
}
