package com.jaziel.aquatrack

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Funciones nativas del sistema: canal de notificaciones, permiso POST_NOTIFICATIONS,
 * tareas periódicas con WorkManager y vibración.
 */
object Recordatorio {

    private const val CANAL = "recordatorios_agua"
    private const val TRABAJO = "recordatorio_agua"

    /** Android 8+ exige un canal para mostrar notificaciones. */
    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL, "Recordatorios de agua", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Te recuerda tomar agua cada cierto tiempo" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** En Android 13+ (API 33) el usuario debe conceder el permiso de notificaciones. */
    fun tienePermiso(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Programa el recordatorio cada [horas] horas. WorkManager lo mantiene aunque se reinicie el teléfono. */
    fun programar(context: Context, horas: Int) {
        val solicitud = PeriodicWorkRequestBuilder<RecordatorioWorker>(horas.toLong(), TimeUnit.HOURS)
            .setInitialDelay(horas.toLong(), TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.UPDATE, solicitud)
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO)
    }

    fun textoPendiente(almacen: Almacen): String {
        val faltan = almacen.meta - almacen.totalDe(almacen.hoy())
        return if (faltan > 0) "Te faltan $faltan ml para tu meta de hoy."
        else "¡Ya cumpliste tu meta de hoy! Sigue hidratándote."
    }

    /** Muestra la notificación; al tocarla se abre la app. */
    @SuppressLint("MissingPermission") // el permiso se revisa con tienePermiso()
    fun mostrar(context: Context, texto: String) {
        if (!tienePermiso(context)) return
        crearCanal(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val alTocar = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_gota)
            .setContentTitle("Hora de tomar agua 💧")
            .setContentText(texto)
            .setContentIntent(alTocar)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, notificacion)
    }

    /** Vibración corta doble (hardware del teléfono). */
    fun vibrar(context: Context) {
        val patron = VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)
        obtenerVibrador(context).vibrate(patron)
    }

    @Suppress("DEPRECATION")
    private fun obtenerVibrador(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
}

/** Tarea en segundo plano que ejecuta WorkManager: avisa solo si aún no se cumple la meta. */
class RecordatorioWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val almacen = Almacen(applicationContext)
        if (almacen.totalDe(almacen.hoy()) < almacen.meta) {
            Recordatorio.mostrar(applicationContext, Recordatorio.textoPendiente(almacen))
        }
        return Result.success()
    }
}
