#!/bin/bash
export DISPLAY=:1
cd /home/coding/prototype/encrypt
mvn exec:java -Prun -q &
sleep 7
xdotool search --name "Password Utility Suite" | head -1
