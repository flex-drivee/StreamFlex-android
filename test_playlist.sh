#!/bin/bash
USER_AGENT="Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/149.0.7827.91 Safari/537.36 /OS.Gatu v3.0"
UNIXTIME=$(date +%s000)

curl -s --compressed "https://net52.cc/mobile/playlist.php?id=81465707&t=Spider-Man%3A+No+Way+Home&tm=$UNIXTIME" \
-H "Accept: application/json, text/plain, */*" \
-H "Accept-Language: en-US,en;q=0.9" \
-H "Connection: keep-alive" \
-H "Referer: https://net52.cc/mobile/home?app=1" \
-H "Sec-Fetch-Dest: empty" \
-H "Sec-Fetch-Mode: cors" \
-H "Sec-Fetch-Site: same-origin" \
-H "User-Agent: $USER_AGENT" \
-H "X-Requested-With: app.netmirror.netmirrornew" \
-H "Cookie: t_hash_t=b21bb9450aed630e9ab0acf812b55869%3A%3Af5bc0bb4e15bbaa780ef67e427cd1047%3A%3A1787859936%3A%3Akp%3A%3A99"
