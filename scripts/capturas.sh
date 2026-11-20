#!/usr/bin/env bash
# Toma capturas REALES de AquaTrack en un emulador Android usando adb y UI Automator.
# Lo ejecuta .github/workflows/capturas.yml; también funciona con un emulador o teléfono local conectado por adb.
# Uso: bash scripts/capturas.sh AquaTrack.apk
set -u
APK="$1"
PKG="com.jaziel.aquatrack"
mkdir -p capturas
LOG=capturas/pruebas.txt
: > "$LOG"
anota() { echo "$*" | tee -a "$LOG"; }

# Busca un elemento en pantalla por su texto (o por un atributo) y devuelve el centro "x y".
buscar() { # $1 = atributo (text|checkable), $2 = valor
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  python3 - "$1" "$2" <<'PY'
import re, sys, xml.etree.ElementTree as ET
attr, valor = sys.argv[1], sys.argv[2]
try:
    nodos = list(ET.parse("ui.xml").iter("node"))
except Exception:
    nodos = []
for n in nodos:
    if n.get(attr) == valor or (attr == "text" and n.get("content-desc") == valor):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
}

tocar() { # $1 = atributo, $2.. = valores aceptados (el primero que aparezca)
  local attr="$1"; shift
  for intento in 1 2 3 4 5 6; do
    for valor in "$@"; do
      local xy; xy=$(buscar "$attr" "$valor")
      if [ -n "$xy" ]; then adb shell input tap $xy; sleep 2; anota "  tocado: $valor"; return 0; fi
    done
    sleep 2
  done
  anota "  ERROR: no encontré $*"; return 1
}

existe() { [ -n "$(buscar text "$1")" ]; }
cap() { sleep 1; adb exec-out screencap -p > "capturas/$1"; anota "captura: $1"; }

anota "== Dispositivo: $(adb shell getprop ro.product.model) · Android $(adb shell getprop ro.build.version.release) (API $(adb shell getprop ro.build.version.sdk))"
adb install -r "$APK" && anota "CA-3 instalación: OK"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# 1. Inicio
adb shell am start -W -n "$PKG/.MainActivity" >/dev/null; sleep 4
existe "Hoy" && anota "CA-3 la app abre: OK"
cap 01_inicio.png

# 2. Registro de tomas: 250 + 500 + 250 = 1000 ml
tocar text "+250 ml"; tocar text "+500 ml"; tocar text "+250 ml"
existe "1000 ml" && anota "CA-4 suma al anillo (1000 ml): OK"
cap 02_registro.png

# 3. Meta alcanzada (2000 ml): +500 +500
tocar text "+500 ml"; tocar text "+500 ml"; sleep 1
existe "¡Meta cumplida! 💧" && anota "CA-6 felicitación al cumplir la meta: OK"
cap 03_meta_alcanzada.png
anota "-- Vibraciones recientes registradas por el sistema:"
adb shell dumpsys vibrator_manager 2>/dev/null | grep -i -A2 "$PKG" | head -8 | tee -a "$LOG"
tocar text "Genial"

# Deshacer resta la última toma
tocar text "Deshacer"
existe "1500 ml" && anota "CA-4 deshacer resta (1500 ml): OK"
tocar text "+500 ml"

# 4. Persistencia: cerrar la app por completo y volver a abrirla
adb shell am force-stop "$PKG"; sleep 1
adb shell am start -W -n "$PKG/.MainActivity" >/dev/null; sleep 4
existe "2000 ml" && anota "CA-7 los datos persisten al cerrar y abrir (2000 ml): OK"

# 5. Historial
tocar text "Historial"
cap 04_historial.png

# 6. Ajustes: activar recordatorio -> diálogo de permiso (Android 13+)
tocar text "Ajustes"
tocar checkable "true"
sleep 2
cap 06_permiso.png
tocar text "Permitir" "Allow" && anota "CA-8 diálogo de permiso POST_NOTIFICATIONS: OK"
sleep 1
existe "Recordatorio activado cada 2 h." && anota "RF-4 recordatorio programado con WorkManager: OK"
anota "-- Trabajo programado en JobScheduler (WorkManager):"
adb shell dumpsys jobscheduler 2>/dev/null | grep -i "$PKG" | head -3 | tee -a "$LOG"
cap 05_ajustes.png

# 7. Notificación de prueba
tocar text "Probar notificación"
sleep 2
adb shell cmd statusbar expand-notifications; sleep 2
adb shell dumpsys notification --noredact 2>/dev/null | grep -q "Hora de tomar agua" && anota "CA-9 notificación publicada: OK"
cap 07_notificacion.png
adb shell cmd statusbar collapse; sleep 1

# 8. Icono en el cajón de aplicaciones
adb shell input keyevent KEYCODE_HOME; sleep 2
TAM=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1); W=${TAM%x*}; H=${TAM#*x}
adb shell input swipe $((W/2)) $((H*85/100)) $((W/2)) $((H*20/100)) 300; sleep 3
cap 08_icono.png

anota "== Fin"
