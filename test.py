# Let's write a python script to hit MovieBox ranking-list to see its JSON shape.
import urllib.request
import urllib.parse
import hashlib
import base64
import hmac
import time
import json
import uuid

SECRET_KEY_DEFAULT_B64 = "WnB2UGlMUGlJWE9oVUV6Wg=="
brand = "samsung"
model = "SM-G998B"
device_id = uuid.uuid4().hex[:16]

def md5(data):
    return hashlib.md5(data).hexdigest()

def get_headers(url):
    timestamp = str(int(time.time() * 1000))
    reversed_ts = timestamp[::-1]
    hash_val = md5(reversed_ts.encode())
    x_client_token = f"{timestamp},{hash_val}"
    
    parsed = urllib.parse.urlparse(url)
    path = parsed.path
    query = parsed.query
    if query:
        # Sort query params
        params = urllib.parse.parse_qsl(query)
        params.sort()
        query = "&".join(f"{k}={v}" for k, v in params)
        canonical_url = f"{path}?{query}"
    else:
        canonical_url = path

    canonical = f"GET\napplication/json\n\n\n{timestamp}\n\n{canonical_url}"
    
    secret_bytes = base64.b64decode(base64.b64decode(SECRET_KEY_DEFAULT_B64).decode())
    signature = hmac.new(secret_bytes, canonical.encode(), hashlib.md5).digest()
    sig_b64 = base64.b64encode(signature).decode()
    x_tr_signature = f"{timestamp}|2|{sig_b64}"
    
    client_info = json.dumps({"package_name":"com.community.oneroom","version_name":"3.0.13.0325.03","version_code":50020088,"os":"android","os_version":"13","install_ch":"ps","device_id":device_id,"install_store":"ps","gaid":"1b2212c1-dadf-43c3-a0c8-bd6ce48ae22d","brand":brand,"model":model,"system_language":"en","net":"NETWORK_WIFI","region":"US","timezone":"Asia/Calcutta","sp_code":"","X-Play-Mode":"1","X-Idle-Data":"1","X-Family-Mode":"0","X-Content-Mode":"0"})
    
    return {
        "X-Client-Info": client_info,
        "X-Client-Status": "0",
        "X-Client-Token": x_client_token,
        "X-Tr-Signature": x_tr_signature,
        "User-Agent": "com.community.oneroom/50020088 (Linux; U; Android 13; en_US; samsung; Build/TQ3A.230901.001; Cronet/145.0.7582.0)",
        "Accept": "application/json"
    }

req = urllib.request.Request("https://api6.aoneroom.com/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=5", headers=get_headers("https://api6.aoneroom.com/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=5"))
try:
    with urllib.request.urlopen(req) as f:
        print(f.read().decode()[:1500])
except Exception as e:
    print(e)
    try:
        print(e.read().decode())
    except:
        pass
