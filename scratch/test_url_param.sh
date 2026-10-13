#!/bin/bash
set -e
RESP=$(curl -s "https://api.hlowb.com/v0.1/system/getSecurityKey/1?channel=IndiaA&clientType=1&lang=en-US" -H "User-Agent: Mozilla/5.0")
B64_KEY=$(echo "$RESP" | grep -o '"data":"[^"]*' | cut -d'"' -f4)
KEY_BYTES=$(echo "$B64_KEY" | base64 -d)
SUP="T!BgJB"
RAW_KEY="${KEY_BYTES}${SUP}"
FINAL_KEY=$(echo -n "$RAW_KEY" | head -c 16)
LEN=$(echo -n "$FINAL_KEY" | wc -c)
while [ $LEN -lt 16 ]; do FINAL_KEY="${FINAL_KEY}$(printf '\0')"; LEN=$((LEN+1)); done
HEX_KEY=$(echo -n "$FINAL_KEY" | hexdump -ve '/1 "%02x"')
decrypt() {
  local ENC_DATA="$1"
  ENC_DATA=$(echo "$ENC_DATA" | sed 's/^"//;s/"$//')
  if [[ "$ENC_DATA" == {* ]]; then echo "$ENC_DATA"; return; fi
  echo -n "$ENC_DATA" | base64 -d | openssl enc -d -aes-128-cbc -K "$HEX_KEY" -iv "$HEX_KEY" 2>/dev/null || true
}

SEASON_ID="5699249091140608"
EP_ID="10021384410042735" # Reacher S1 E1

test_payload() {
  local url="$1"
  local json_body="$2"
  local resp=$(curl -s -X POST "$url" -H "Content-Type: application/json" -H "User-Agent: okhttp/4.10.0" -d "$json_body")
  local dec=$(decrypt "$resp")
  echo "$dec" | grep -o '"videoUrl":"[^"]*' | cut -d'"' -f4
}

J1="{\"mode\":\"1\",\"appMarket\":\"GuanWang\",\"clientType\":\"1\",\"woolUser\":\"false\",\"apkSignKey\":\"ED0955EB04E67A1D9F3305B95454FED485261475\",\"androidVersion\":\"13\",\"movieId\":\"$SEASON_ID\",\"episodeId\":\"$EP_ID\",\"isNewUser\":\"true\",\"resolution\":\"\",\"packageName\":\"com.external.castle\"}"

U1="https://api.hlowb.com/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US"
U2="https://api.hlowb.com/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US&languageId=9"
U3="https://api.hlowb.com/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US&track=9"

echo "BASE URL: $(test_payload "$U1" "$J1")"
echo "LANG ID URL: $(test_payload "$U2" "$J1")"
echo "TRACK URL: $(test_payload "$U3" "$J1")"

