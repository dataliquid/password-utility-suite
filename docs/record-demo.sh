#!/bin/bash
set -e

cd "$(dirname "$0")/.."

export DISPLAY=:1

# Delay zwischen Hauptschritten (2 Sekunden)
STEP_DELAY=2

echo "=== GIF-Demo Recording für Password Utility Suite ==="

# Anwendung starten
echo "Starte Anwendung..."
mvn exec:java -Prun -q &
APP_PID=$!
sleep 7

# Fenster finden
echo "Suche Fenster..."
WIN_ID=$(xdotool search --name "Password Utility Suite" | head -1)
if [ -z "$WIN_ID" ]; then
    echo "Fehler: Fenster nicht gefunden!"
    kill $APP_PID 2>/dev/null || true
    exit 1
fi
echo "✓ Fenster gefunden: $WIN_ID"

xdotool windowactivate --sync "$WIN_ID"
sleep 1

# Fenster-Geometrie inkl. Titelleiste ermitteln (xwininfo gibt absolute Position)
WININFO=$(xwininfo -id "$WIN_ID")
X=$(echo "$WININFO" | grep "Absolute upper-left X" | awk '{print $NF}')
Y=$(echo "$WININFO" | grep "Absolute upper-left Y" | awk '{print $NF}')
WIDTH=$(echo "$WININFO" | grep "Width:" | awk '{print $NF}')
HEIGHT=$(echo "$WININFO" | grep "Height:" | awk '{print $NF}')

# Titelleiste einbeziehen (ca. 28px nach oben)
Y=$((Y - 28))
HEIGHT=$((HEIGHT + 28))

echo "Fenster: ${WIDTH}x${HEIGHT} an Position ${X},${Y} (inkl. Titelleiste)"

# Recording starten
echo "Starte Recording..."
ffmpeg -y -video_size ${WIDTH}x${HEIGHT} -framerate 10 \
  -f x11grab -i :1+${X},${Y} \
  -t 70 docs/demo-raw.mp4 &
REC_PID=$!
sleep 2

echo "Führe Workflow aus..."

# ============================================
# SCHRITT 1: Environment anlegen (Ctrl+E)
# ============================================
echo "  1. Environment Dialog öffnen..."
xdotool windowactivate --sync "$WIN_ID"
xdotool key ctrl+e
sleep $STEP_DELAY

# Environment Name eingeben
echo "  1a. Environment Name eingeben..."
xdotool type --delay 100 "Production"
sleep $STEP_DELAY

# Tab zum Password-Feld
echo "  1b. Tab zum Password..."
xdotool key Tab
sleep 0.5

# Master Password eingeben
echo "  1c. Password eingeben..."
xdotool type --delay 100 "MySecretPassword123"
sleep $STEP_DELAY

# Save Button (Alt+S)
echo "  1d. Save..."
xdotool key alt+s
sleep $STEP_DELAY

# OK Button (Alt+O)
echo "  1e. OK..."
xdotool key alt+o
sleep $STEP_DELAY

echo "  ✓ Environment erstellt"

# ============================================
# SCHRITT 2: Neue Datei erstellen (Ctrl+N)
# ============================================
echo "  2. Neue Datei erstellen..."
xdotool windowactivate --sync "$WIN_ID"
xdotool key ctrl+n
sleep $STEP_DELAY

# ============================================
# SCHRITT 3: YAML-Inhalt eingeben
# ============================================
echo "  3. YAML eingeben..."

# Fokus auf Editor setzen (klick ins Fenster)
xdotool windowactivate --sync "$WIN_ID"
xdotool mousemove --window "$WIN_ID" 300 400
xdotool click 1
sleep 0.5

# YAML Inhalt
xdotool type --delay 50 "database:"
xdotool key Return
xdotool type --delay 50 "  host: localhost"
xdotool key Return
xdotool type --delay 50 "  password: secret123"
sleep $STEP_DELAY

echo "  ✓ YAML eingegeben"

# ============================================
# SCHRITT 4: Im Tree zum password navigieren
# ============================================
echo "  4. Tree fokussieren und navigieren..."

# Tree fokussieren (klick auf Tree-Bereich rechts)
xdotool windowactivate --sync "$WIN_ID"
xdotool mousemove --window "$WIN_ID" 900 400
xdotool click 1
sleep 1

# Tree navigieren
xdotool key Down
sleep 0.3
xdotool key Right
sleep 0.3
xdotool key Down
sleep 0.3
xdotool key Down
sleep $STEP_DELAY

echo "  ✓ password ausgewählt"

# ============================================
# SCHRITT 5: Verschlüsseln (Alt+C)
# ============================================
echo "  5. Verschlüsseln..."
xdotool key alt+c
sleep $STEP_DELAY

echo "  ✓ Wert verschlüsselt"

# ============================================
# SCHRITT 6: Entschlüsseln (Alt+C)
# ============================================
echo "  6. Entschlüsseln..."
xdotool key alt+c
sleep $STEP_DELAY

echo "  ✓ Wert entschlüsselt"

# ============================================
# SCHRITT 7: Nochmal verschlüsseln
# ============================================
echo "  7. Nochmal verschlüsseln..."
xdotool key alt+c
sleep $STEP_DELAY

echo "  ✓ Endzustand"

# === AUFRÄUMEN ===
echo ""
echo "Beende Recording..."
sleep 1
kill $REC_PID 2>/dev/null || true
sleep 1
kill $APP_PID 2>/dev/null || true
wait $REC_PID 2>/dev/null || true

# GIF erstellen
echo "Konvertiere zu GIF..."
ffmpeg -y -i docs/demo-raw.mp4 \
  -vf "fps=10,scale=800:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse" \
  docs/images/demo-workflow.gif

rm -f docs/demo-raw.mp4

echo ""
echo "=== Fertig! ==="
echo "GIF erstellt: docs/images/demo-workflow.gif"
