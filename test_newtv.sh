#!/bin/bash
USER_AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
UUID=$(cat /proc/sys/kernel/random/uuid)

echo "1. Getting UUID t_hash_t from verify.php"
HTML=$(curl -s -i -X POST "https://net52.cc/verify.php" \
-H "Origin: https://net22.cc" \
-H "Referer: https://net22.cc/verify2" \
-H "User-Agent: $USER_AGENT" \
-d "g-recaptcha-response=$UUID")

THASH=$(echo "$HTML" | grep -o 't_hash_t=[^;]*' | head -1)
echo "Got: $THASH"

echo "2. Resolving API URL (checknewtv.php)"
API_BASE="https://mobidetect.pro"
TOKEN_HASH=$(curl -s "https://mobidetect.pro/checknewtv.php" -H "User-Agent: $USER_AGENT" -H "Cookie: $THASH" | grep -o '"token_hash":"[^"]*"' | cut -d'"' -f4)
if [ -n "$TOKEN_HASH" ]; then
    API_BASE=$(echo "$TOKEN_HASH" | base64 -d)
fi
echo "API Base: $API_BASE"

echo "3. Fetching player.php"
PLAYER_JSON=$(curl -s "$API_BASE/newtv/player.php?id=81465707" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: NetmirrorNewTV v1.0" \
-H "Ott: nf" \
-H "Usertoken: " \
-H "Cookie: $THASH")

echo "Player JSON: $PLAYER_JSON"
VIDEO_LINK=$(echo "$PLAYER_JSON" | grep -o '"video_link":"[^"]*"' | cut -d'"' -f4 | sed 's/\\//g')
echo "Video Link: $VIDEO_LINK"

echo "4. Fetching the m3u8 (stripping t_hash_t like CNC Verse!)"
curl -s "$VIDEO_LINK" \
-H "Referer: $API_BASE/" \
-H "User-Agent: $USER_AGENT" \
-H "Cookie: hd=on; $THASH" | head -n 15

