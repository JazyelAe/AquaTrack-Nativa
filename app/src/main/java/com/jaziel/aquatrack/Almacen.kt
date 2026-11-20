package com.jaziel.aquatrack

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Una toma de agua: hora (HH:mm) y cantidad en mililitros. */
data class Toma(val hora: String, val ml: Int)

/**
 * Almacenamiento local de la app usando SharedPreferences (API nativa de Android).
 * Las tomas se guardan por fecha con la clave "tomas_yyyy-MM-dd" y el texto "08:15|250;09:30|500".
 */
class Almacen(context: Context) {

    private val prefs = context.getSharedPreferences("aquatrack", Context.MODE_PRIVATE)
    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun hoy(): String = formatoFecha.format(Date())

    fun horaActual(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    // ---------- Tomas por día ----------

    fun tomasDe(fecha: String): List<Toma> {
        val texto = prefs.getString("tomas_$fecha", "") ?: ""
        if (texto.isEmpty()) return emptyList()
        return texto.split(";").mapNotNull { item ->
            val partes = item.split("|")
            val ml = partes.getOrNull(1)?.toIntOrNull()
            if (partes.size == 2 && ml != null) Toma(partes[0], ml) else null
        }
    }

    fun guardarTomas(fecha: String, lista: List<Toma>) {
        val texto = lista.joinToString(";") { "${it.hora}|${it.ml}" }
        prefs.edit().putString("tomas_$fecha", texto).apply()
    }

    fun totalDe(fecha: String): Int = tomasDe(fecha).sumOf { it.ml }

    /** Devuelve los últimos [dias] días (empezando por hoy) con su total en ml. */
    fun ultimosDias(dias: Int): List<Pair<String, Int>> {
        val calendario = Calendar.getInstance()
        return (0 until dias).map { i ->
            if (i > 0) calendario.add(Calendar.DAY_OF_YEAR, -1)
            val fecha = formatoFecha.format(calendario.time)
            fecha to totalDe(fecha)
        }
    }

    // ---------- Ajustes ----------

    var meta: Int
        get() = prefs.getInt("meta", 2000)
        set(valor) = prefs.edit().putInt("meta", valor).apply()

    var recordatorioActivo: Boolean
        get() = prefs.getBoolean("recordatorio", false)
        set(valor) = prefs.edit().putBoolean("recordatorio", valor).apply()

    var horasRecordatorio: Int
        get() = prefs.getInt("horas", 2)
        set(valor) = prefs.edit().putInt("horas", valor).apply()

    /** Para felicitar (y vibrar) solo una vez por día. */
    fun yaFelicitadoHoy(): Boolean = prefs.getString("felicitado", "") == hoy()

    fun marcarFelicitado() {
        prefs.edit().putString("felicitado", hoy()).apply()
    }
}
