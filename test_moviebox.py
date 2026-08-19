import urllib.request
import urllib.parse
import json
import hashlib
import hmac
import base64
import time
import uuid
import random

DEVICE_ID = uuid.uuid4().hex[:16]
SECRET_KEY_DEFAULT_B64 = "VUUxTGVHRkhjbGRRV2pGVlowZFhlRk5VUVcxaVZGSkRUVlpKZFdKc1NsQmFTR1Jz"
x_user_token = None

def md5(b):
    return hashlib.md5(b).hexdigest()

def generate_x_client_token(ts):
    reversed_ts = str(ts)[::-1]
    h = md5(reversed_ts.encode('utf-8'))
    return f"{ts},{h}"

def generate_x_tr_signature(method, accept, content_type, url, body, ts):
    parsed = urllib.parse.urlparse(url)
    path = parsed.path
    query = parsed.query
    
    if query:
        params = urllib.parse.parse_qsl(query, keep_blank_values=True)
        sorted_params = sorted(params, key=lambda x: x[0])
        query_str = "&".join([f"{k}={v}" for k, v in sorted_params])
        canonical_url = f"{path}?{query_str}"
    else:
        canonical_url = path

    body_bytes = body.encode('utf-8') if body else b""
    if body_bytes:
        trimmed = body_bytes[:102400]
        body_hash = md5(trimmed)
    else:
        body_hash = ""
    
    body_length = str(len(body_bytes)) if body_bytes else ""
    
    canonical = f"{method.upper()}\n{accept or ''}\n{content_type or ''}\n{body_length}\n{ts}\n{body_hash}\n{canonical_url}"
    
    secret = base64.b64decode(base64.b64decode(SECRET_KEY_DEFAULT_B64))
    
    mac = hmac.new(secret, canonical.encode('utf-8'), hashlib.md5)
    sig_b64 = base64.b64encode(mac.digest()).decode('utf-8')
    
    return f"{ts}|2|{sig_b64}"

def do_request(url, method="GET", body=None):
    global x_user_token
    ts = int(time.time() * 1000)
    accept = "application/json"
    content_type = "application/json" if method == "POST" else ""
    
    client_info = json.dumps({
        "package_name": "com.community.oneroom",
        "version_name": "3.0.13.0325.03",
        "version_code": 50020088,
        "os": "android",
        "os_version": "16",
        "device_id": DEVICE_ID,
        "install_store": "ps",
        "gaid": "d7578036d13336cc",
        "brand": "samsung",
        "model": "SM-G998B",
        "system_language": "en",
        "net": "NETWORK_WIFI",
        "region": "US",
        "timezone": "America/New_York",
        "sp_code": ""
    }, separators=(',', ':'))
    
    headers = {
        "X-Client-Info": client_info,
        "X-Client-Status": "0",
        "X-Client-Token": generate_x_client_token(ts),
        "X-Tr-Signature": generate_x_tr_signature(method, accept, content_type, url, body, ts),
        "User-Agent": "com.community.oneroom/50020088 (Linux; U; Android 16; en_US; sdk_gphone64_x86_64; Build/BP22.250325.006; Cronet/133.0.6876.3)",
        "Accept": accept
    }
    if method == "POST":
        headers["Content-Type"] = content_type
    if x_user_token:
        headers["Authorization"] = f"Bearer {x_user_token}"

    req = urllib.request.Request(url, data=body.encode('utf-8') if body else None, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            x_user = response.getheader("x-user") or response.getheader("X-User")
            if x_user:
                try:
                    x_user_token = json.loads(x_user).get("token")
                except: pass
            return response.read().decode('utf-8')
    except urllib.error.HTTPError as e:
        print(f"HTTP Error: {e.code} {e.read().decode('utf-8')}")
        return None

base_url = "https://apig.inmoviebox.com"

print("Fetching token...")
do_request(f"{base_url}/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=1")

print("Searching...")
search_body = json.dumps({"keyword": "Spider-Man No Way Home", "page": 1, "perPage": 20})
res = do_request(f"{base_url}/wefeed-mobile-bff/subject-api/search/v2", "POST", search_body)
if not res: exit()

data = json.loads(res).get("data", {})
results = data.get("results", [])
found_ids = []
for r in results:
    for s in r.get("subjects", []):
        print(f"Found: {s.get('title')} (ID: {s.get('subjectId')})")
        found_ids.append(s.get('subjectId'))

if not found_ids: exit()

for subject_id in found_ids:
    print(f"\nGetting play info for {subject_id}...")
    play_res = do_request(f"{base_url}/wefeed-mobile-bff/subject-api/play-info?subjectId={subject_id}", "GET")
    if not play_res: continue
    
    play_data = json.loads(play_res).get("data", {})
    streams = play_data.get("streams", [])
    for s in streams:
        print(f"Stream: {s.get('resolutions')} - Lang: {s.get('language')}")
