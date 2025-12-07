#!/bin/bash
export DISPLAY=:1

WIN_ID=$(xdotool search --name "Password Utility Suite" | head -1)
echo "WIN_ID: $WIN_ID"

echo ""
echo "=== TEST 1: Ctrl+E (Environment Dialog) ==="
xdotool windowactivate --sync "$WIN_ID"
xdotool key ctrl+e
sleep 2

DIALOG=$(xdotool search --name "Environments" 2>/dev/null | head -1)
if [ -n "$DIALOG" ]; then
    echo "✓ Dialog geöffnet: $DIALOG"
else
    echo "✗ Dialog NICHT geöffnet"
    exit 1
fi

echo ""
echo "=== TEST 2: Environment Name eingeben ==="
xdotool type --delay 100 "Production"
sleep 1
echo "✓ Name eingegeben"

echo ""
echo "=== TEST 3: Tab zum Password ==="
xdotool key Tab
sleep 0.5
echo "✓ Tab gedrückt"

echo ""
echo "=== TEST 4: Password eingeben ==="
xdotool type --delay 100 "MySecretPassword123"
sleep 1
echo "✓ Password eingegeben"

echo ""
echo "=== TEST 5: Save (Alt+S) ==="
xdotool key alt+s
sleep 2
echo "✓ Save gedrückt"

echo ""
echo "=== TEST 6: OK (Alt+O) ==="
xdotool key alt+o
sleep 2

DIALOG=$(xdotool search --name "Environments" 2>/dev/null | head -1)
if [ -z "$DIALOG" ]; then
    echo "✓ Dialog geschlossen"
else
    echo "✗ Dialog noch offen, schließe mit ESC"
    xdotool key Escape
    sleep 1
fi

echo ""
echo "=== TEST 7: Neue Datei (Ctrl+N) ==="
xdotool windowactivate --sync "$WIN_ID"
xdotool key ctrl+n
sleep 2
echo "✓ Ctrl+N gedrückt"

echo ""
echo "=== TEST 8: YAML eingeben ==="
# Fokus auf Editor setzen (klick ins Fenster)
xdotool windowactivate --sync "$WIN_ID"
xdotool mousemove --window "$WIN_ID" 300 400
xdotool click 1
sleep 0.5
xdotool type --delay 50 "database:"
xdotool key Return
xdotool type --delay 50 "  host: localhost"
xdotool key Return
xdotool type --delay 50 "  password: secret123"
sleep 2
echo "✓ YAML eingegeben"

echo ""
echo "=== TEST 9: Fokus auf Tree setzen ==="
# Tree ist rechts im Fenster - klick auf Tree-Bereich (ca. x=900)
xdotool windowactivate --sync "$WIN_ID"
xdotool mousemove --window "$WIN_ID" 900 400
xdotool click 1
sleep 1
echo "✓ Tree fokussiert"

echo ""
echo "=== TEST 10: Tree navigieren ==="
xdotool key Down
sleep 0.3
xdotool key Right
sleep 0.3
xdotool key Down
sleep 0.3
xdotool key Down
sleep 1
echo "✓ Im Tree navigiert (sollte bei password sein)"

echo ""
echo "=== TEST 11: Verschlüsseln (Alt+C) ==="
xdotool key alt+c
sleep 2
echo "✓ Alt+C gedrückt"

echo ""
echo "=== ALLE TESTS ABGESCHLOSSEN ==="
