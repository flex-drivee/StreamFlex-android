#!/bin/bash
USER_AGENT="Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/149.0.7827.91 Safari/537.36 /OS.Gatu v3.0"

echo "1. Fetching mobile/home?app=1 to get data-addhash..."
HOME_HTML=$(curl -s "https://net52.cc/mobile/home?app=1" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: app.netmirror.netmirrornew")

ADDHASH=$(echo "$HOME_HTML" | grep -o 'data-addhash="[^"]*"' | cut -d'"' -f2)
echo "Addhash (t_hash_t): $ADDHASH"

UNIXTIME=$(date +%s000)
echo "2. Getting playlist.php using addhash as t_hash_t"
PLAYLIST=$(curl -s --compressed "https://net52.cc/mobile/playlist.php?id=81465707&t=Spider-Man%3A+No+Way+Home&tm=$UNIXTIME" \
-H "Referer: https://net52.cc/mobile/home?app=1" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: app.netmirror.netmirrornew" \
-H "Cookie: hd=on; t_hash_t=$ADDHASH")

M3U8_URL=$(echo "$PLAYLIST" | grep -o '/mobile/hls/[^"]*' | head -1)
echo "M3U8_URL: $M3U8_URL"

echo "3. Fetching m3u8 STRIPPING THE t_hash_t COOKIE (like CNC Verse does!)"
curl -s --compressed "https://net52.cc$M3U8_URL" \
-H "Referer: https://net52.cc/mobile/home?app=1" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: app.netmirror.netmirrornew" \
-H "Cookie: hd=on" | head -n 15

