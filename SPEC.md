# SPEC · AquaTrack — Subproducto No. 1 "Aplicación Nativa"

**Materia:** Aplicaciones Web Progresivas · Unidad I · Actividad Apertura
**Alumno:** Jahir Jaziel Serrano Arana · 10° D
**Repositorio:** `JazyelAe/kotlin` (se propone renombrarlo a `AquaTrack-Nativa`)
**Estado:** borrador para aprobación (Checkpoint A)

## 1. Objetivo
Construir una app **nativa de Android** escrita en Kotlin con el SDK oficial (Jetpack Compose + Material 3) que ayude a registrar el consumo diario de agua. Debe usar funciones propias del sistema operativo (notificaciones locales, tareas en segundo plano con WorkManager, vibración y almacenamiento local), que es lo que la distingue de una app web o multiplataforma.
La app debe poder descargarse como APK desde un GitHub Release generado automáticamente con GitHub Actions.

## 2. Auditoría del repo actual
| Elemento | Estado | Decisión |
|---|---|---|
| `app/src/.../MainActivity.kt` (242 líneas) | Compose: botones 250/500 ml, anillo de progreso, lista de tomas, reiniciar. SharedPreferences guarda **solo el día actual**. Meta fija de 2000 ml. | Se reutiliza la interfaz y se divide en archivos pequeños. |
| `MainActivity.kt` en la raíz | Copia idéntica del anterior. | Se elimina (duplicado). |
| `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts` | Correctos (AGP 8.5.2, Kotlin 2.0.20, minSdk 26, compileSdk 34, JDK 17). | Se conservan; se agregan dependencias. |
| Gradle wrapper (`gradlew`, `gradle/wrapper/`) | **No existe**: el proyecto no compila desde terminal ni en CI. | Se agrega (Gradle 8.9). |
| Icono | No tiene (usa el genérico). | Icono adaptativo propio (gota de agua). |
| Notificaciones, historial, meta configurable, Actions, Release | No existen. | Se implementan. |
| `README.txt` | Instrucciones para crear el proyecto a mano. | Se reemplaza por `README.md`. |

No se puede compilar en esta PC (no hay Android SDK ni JDK 17); la compilación se hace en **GitHub Actions**.

## 3. Requisitos funcionales
- **RF-1 Registro de consumo.** Botones rápidos de **250 ml** y **500 ml** y un botón **"Otra cantidad"** que abre un diálogo para escribir los ml (validación: entero entre 50 y 2000). Cada toma guarda hora y cantidad. Se puede deshacer la última toma.
- **RF-2 Meta diaria configurable.** En la pantalla **Ajustes** se elige la meta (de 1000 a 5000 ml, en pasos de 250 ml; por defecto 2000). La pantalla **Hoy** muestra un anillo de progreso con ml consumidos / meta y el porcentaje.
- **RF-3 Historial por día.** Pantalla **Historial** con los últimos 7 días: fecha, total en ml, barra de progreso y ✔ si se cumplió la meta. Los datos se guardan localmente con **SharedPreferences** (un registro por fecha) y se conservan al cerrar la app o reiniciar el teléfono.
- **RF-4 Funciones nativas del dispositivo.**
  - a) **Recordatorio con notificación local** cada *N* horas (1, 2 o 3; por defecto 2), programado con **WorkManager** y activable o desactivable en Ajustes. La notificación dice cuántos ml faltan para la meta y abre la app al tocarla. No se envía si la meta ya se cumplió.
  - b) En **Android 13+** se pide el permiso `POST_NOTIFICATIONS` al activar el recordatorio; si se niega, la app sigue funcionando y lo indica.
  - c) **Vibración** y un mensaje de felicitación al alcanzar la meta (una vez por día).
  - d) Botón "Probar notificación" en Ajustes, para demostrarla en clase sin esperar horas.
- **RF-5 Interfaz.** Jetpack Compose + Material 3, barra de navegación inferior con 3 pestañas (Hoy · Historial · Ajustes), colores azul agua, soporte de modo oscuro del sistema e **icono adaptativo propio**.

## 4. Requisitos no funcionales
- **RNF-1** Android 8.0+ (minSdk 26), targetSdk 34, paquete `com.jaziel.aquatrack`.
- **RNF-2** Funciona sin internet (no pide el permiso `INTERNET`).
- **RNF-3** Código simple y comentado en español, que pueda explicarse en clase: sin bases de datos ni librerías de navegación; solo Compose, WorkManager y SharedPreferences.
- **RNF-4** Compilación reproducible con `./gradlew assembleDebug` y en GitHub Actions.
- **RNF-5** Sin secretos en el repo. El APK es *debug* (firmado con la llave de depuración), así que puede instalarse directamente ("orígenes desconocidos").

## 5. Tecnologías
| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.0.20 |
| Interfaz | Jetpack Compose (BOM 2024.09) + Material 3 |
| Persistencia | SharedPreferences |
| Segundo plano | WorkManager 2.9 (`PeriodicWorkRequest`) |
| Sistema | NotificationManager / NotificationChannel, Vibrator, permisos en tiempo de ejecución |
| Build | Gradle 8.9 (wrapper), Android Gradle Plugin 8.5.2, JDK 17 |
| CI/CD | GitHub Actions (`.github/workflows/android.yml`) + GitHub Releases |

## 6. Estructura propuesta
```
app/src/main/java/com/jaziel/aquatrack/
├── MainActivity.kt        ← tema y navegación inferior
├── Almacen.kt             ← SharedPreferences: tomas por fecha, meta y ajustes
├── PantallaHoy.kt         ← anillo, botones y lista de tomas
├── PantallaHistorial.kt   ← últimos 7 días
├── PantallaAjustes.kt     ← meta, recordatorio y botón de prueba
└── Recordatorio.kt        ← canal de notificación + Worker de WorkManager
app/src/main/res/          ← icono adaptativo (vector), tema
.github/workflows/android.yml
docs/capturas/
README.md · SPEC.md
```

## 7. Criterios de aceptación
| # | Criterio | Cómo se verifica |
|---|---|---|
| CA-1 | El workflow de Actions termina en verde y genera `AquaTrack-v1.0.0.apk`. | Pestaña Actions |
| CA-2 | El Release `v1.0.0` tiene el APK descargable. | Abrir la página del Release |
| CA-3 | La app instala y abre en Android 8+. | Teléfono o emulador (lo haces tú) |
| CA-4 | Los botones 250/500/otra cantidad suman al anillo; "deshacer" resta. | Captura `02_registro.png` |
| CA-5 | Al cambiar la meta en Ajustes, el anillo se recalcula. | Captura `05_ajustes.png` |
| CA-6 | Al alcanzar la meta vibra y se muestra la felicitación. | Captura `03_meta_alcanzada.png` |
| CA-7 | Al cerrar y abrir la app, los datos siguen ahí; el historial muestra los días anteriores. | Captura `04_historial.png` |
| CA-8 | En Android 13+ se pide el permiso de notificaciones. | Captura `06_permiso.png` |
| CA-9 | Llega la notificación de recordatorio (botón de prueba y la periódica). | Captura `07_notificacion.png` |
| CA-10 | README con descripción, tecnologías, instrucciones, enlaces y capturas. | Revisar el repo |

## 8. Plan de despliegue
1. Commits en `main` → el workflow compila `assembleDebug` y sube el APK como *artifact* en cada push.
2. Al crear la etiqueta `v1.0.0`, el mismo workflow crea el **GitHub Release** y adjunta `AquaTrack-v1.0.0.apk`.
3. **URL de la aplicación** para el reporte: `https://github.com/JazyelAe/<repo>/releases/tag/v1.0.0`.

## 9. Capturas
No hay emulador en esta PC, así que **las tomas tú** en tu teléfono o en el emulador de Android Studio y las guardas en `docs/capturas/` del repo. Se piden en el Checkpoint B con instrucciones exactas:
`01_inicio.png` (pantalla Hoy vacía) · `02_registro.png` (con varias tomas) · `03_meta_alcanzada.png` (anillo al 100 % y felicitación) · `04_historial.png` · `05_ajustes.png` · `06_permiso.png` (diálogo del permiso de notificaciones) · `07_notificacion.png` (notificación en la barra) · `08_icono.png` (icono en el cajón de apps).
Yo genero el diagrama de arquitectura (Kotlin → compilado para Android → APIs del sistema) como imagen para el reporte.

## 10. Ejemplos reales propuestos (URL comprobadas el 20/11/2026)
| App | Por qué es nativa | URL |
|---|---|---|
| WhatsApp Messenger | App de Android distribuida en Play Store; usa cámara, micrófono, contactos y notificaciones push del sistema. | https://play.google.com/store/apps/details?id=com.whatsapp |
| Google Maps | Usa GPS, sensores (brújula) y renderizado de mapas nativo; navegación en segundo plano. | https://play.google.com/store/apps/details?id=com.google.android.apps.maps |
| Pixel Camera (Google) | App de cámara del sistema: acceso directo al hardware de la cámara y procesamiento de imagen en el dispositivo. | https://play.google.com/store/apps/details?id=com.google.android.GoogleCamera |

Fuentes técnicas para el reporte (comprobadas): Android Developers — Platform architecture, Jetpack Compose, WorkManager y Notification runtime permission.

## 11. Pruebas
_(Se llena después de implementar: ✅/❌ por cada criterio.)_
