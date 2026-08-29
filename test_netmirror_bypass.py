import urllib.request
import time
import re
from http.cookiejar import CookieJar

cj = CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
opener.addheaders = [
    ('User-Agent', 'Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0'),
    ('X-Requested-With', 'XMLHttpRequest')
]

# 1. GET verify.php
print("Getting verify.php...")
try:
    r1 = opener.open("https://net77.cc/mobile/verify.php")
    html = r1.read().decode('utf-8')
    match = re.search(r'data-addhash="([^"]+)"', html)
    if not match:
        print("Could not find data-addhash!")
        exit(1)
    
    addhash = match.group(1)
    print(f"Got addhash: {addhash}")
    
    # 2. GET userver.net52.cc
    unix_time = int(time.time())
    url2 = f"https://userver.net52.cc/?jjoii={addhash}&a=y&t={unix_time}"
    print(f"Triggering backend bypass: {url2}")
    opener.open(url2)
    
    # 3. Wait 10 seconds
    print("Waiting 10 seconds...")
    time.sleep(10)
    
    # 4. POST verify2.php
    print("Posting verify2.php...")
    data = f"verify={addhash}".encode('utf-8')
    opener.open("https://net77.cc/mobile/verify2.php", data=data)
    
    cookies = {cookie.name: cookie.value for cookie in cj}
    print(f"Cookies received: {cookies}")
    
    if "t_hash_t" in cookies:
        print(f"SUCCESS! t_hash_t = {cookies['t_hash_t']}")
    else:
        print("Failed to get t_hash_t.")

except Exception as e:
    print(f"Error: {e}")
