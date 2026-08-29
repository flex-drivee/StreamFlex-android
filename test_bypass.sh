#!/bin/bash
USER_AGENT="Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0"

echo "1. Fetching /mobile/home?app=1 to get data-addhash"
HTML=$(curl -s -H "User-Agent: $USER_AGENT" -H "X-Requested-With: app.netmirror.netmirrornew" "https://net52.cc/mobile/home?app=1")
ADDHASH=$(echo "$HTML" | grep -o 'data-addhash="[^"]*"' | cut -d'"' -f2)
echo "Found addhash: $ADDHASH"

if [ -z "$ADDHASH" ]; then
    echo "Failed to get addhash"
    exit 1
fi

TIMESTAMP=$(date +%s000)
echo "2. Hitting userver.net52.cc"
curl -s -H "User-Agent: $USER_AGENT" -H "X-Requested-With: app.netmirror.netmirrornew" "https://userver.net52.cc/?jjoii=${ADDHASH}&a=y&t=${TIMESTAMP}"

echo "3. Sleeping 10 seconds..."
sleep 10

echo "4. POSTing to mobile/verify2.php"
curl -s -i -X POST -d "verify=$ADDHASH" -H "User-Agent: $USER_AGENT" -H "X-Requested-With: app.netmirror.netmirrornew" "https://net52.cc/mobile/verify2.php"
