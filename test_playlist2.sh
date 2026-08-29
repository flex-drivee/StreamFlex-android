#!/bin/bash
USER_AGENT="Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/149.0.7827.91 Safari/537.36 /OS.Gatu v3.0"
UUID=$(cat /proc/sys/kernel/random/uuid)

echo "1. Getting UUID t_hash_t from verify.php"
HTML=$(curl -s -i -X POST "https://net52.cc/verify.php" \
-H "Origin: https://net22.cc" \
-H "Referer: https://net22.cc/verify2" \
-H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36" \
-d "g-recaptcha-response=$UUID")

THASH=$(echo "$HTML" | grep -o 't_hash_t=[^;]*' | head -1)
echo "Got: $THASH"

UNIXTIME=$(date +%s000)
echo "2. Getting playlist.php"
PLAYLIST=$(curl -s --compressed "https://net52.cc/mobile/playlist.php?id=81465707&t=Spider-Man%3A+No+Way+Home&tm=$UNIXTIME" \
-H "Referer: https://net52.cc/mobile/home?app=1" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: app.netmirror.netmirrornew" \
-H "Cookie: hd=on; $THASH")

M3U8_URL=$(echo "$PLAYLIST" | grep -o '/mobile/hls/[^"]*' | head -1)
echo "M3U8_URL: $M3U8_URL"

echo "3. Fetching m3u8 STRIPPING THE t_hash_t COOKIE (like CNC Verse does!)"
curl -s --compressed "https://net52.cc$M3U8_URL" \
-H "Referer: https://net52.cc/mobile/home?app=1" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: app.netmirror.netmirrornew" \
-H "Cookie: hd=on"
