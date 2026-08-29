import requests
import time
import json
from bs4 import BeautifulSoup

def bypass():
    headers = {
        "User-Agent": "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0",
        "X-Requested-With": "app.netmirror.netmirrornew"
    }
    
    print("1. Fetching /mobile/home?app=1 to get data-addhash")
    res = requests.get("https://net50.cc/mobile/home?app=1", headers=headers)
    soup = BeautifulSoup(res.text, "html.parser")
    body = soup.find("body")
    if not body or not body.has_attr("data-addhash"):
        print("Failed to find data-addhash!")
        return
        
    addhash = body["data-addhash"]
    print("Found addhash:", addhash)
    
    print("2. Hitting userver.net52.cc")
    timestamp = int(time.time() * 1000)
    requests.get(f"https://userver.net52.cc/?jjoii={addhash}&a=y&t={timestamp}", headers=headers)
    
    print("3. Sleeping 10 seconds...")
    time.sleep(10)
    
    print("4. POSTing to mobile/verify2.php")
    res2 = requests.post("https://net50.cc/mobile/verify2.php", data={"verify": addhash}, headers=headers, allow_redirects=False)
    print("Headers:", res2.headers)
    
    cookies = res2.cookies.get_dict()
    print("Cookies:", cookies)
    
    if "t_hash_t" not in cookies:
        print("Failed to get t_hash_t")
        return
        
    t_hash_t = cookies["t_hash_t"]
    print("Got t_hash_t:", t_hash_t)
    
    return t_hash_t

bypass()
