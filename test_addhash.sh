#!/bin/bash
USER_AGENT="Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0"

echo "1. Fetching mobile/home?app=1 to get data-addhash..."
HOME_HTML=$(curl -s "https://net52.cc/mobile/home?app=1" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: app.netmirror.netmirrornew")

ADDHASH=$(echo "$HOME_HTML" | grep -o 'data-addhash="[^"]*"' | cut -d'"' -f2)
echo "Addhash (t_hash_t): $ADDHASH"

echo "2. Using addhash as t_hash_t to fetch newtv/player.php..."
API_BASE="https://mobidetect.pro"
PLAYER_JSON=$(curl -s "$API_BASE/newtv/player.php?id=81465707" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: NetmirrorNewTV v1.0" \
-H "Ott: nf" \
-H "Usertoken: " \
-H "Cookie: t_hash_t=$ADDHASH")

echo "Player JSON: $PLAYER_JSON"
VIDEO_LINK=$(echo "$PLAYER_JSON" | grep -o '"video_link":"[^"]*"' | cut -d'"' -f4 | sed 's/\\//g')
echo "Video Link: $VIDEO_LINK"

echo "3. Fetching m3u8 (stripping t_hash_t)..."
curl -s "$VIDEO_LINK" \
-H "Referer: $API_BASE/" \
-H "User-Agent: $USER_AGENT" \
-H "Cookie: hd=on" | head -n 15

