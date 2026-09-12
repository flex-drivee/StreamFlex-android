import urllib.request
import json

q = "reacher"
for ott in ["hs", "dp", "nf", "pv"]:
    url = f"https://net52.cc/mobile/search.php?s={q}" if ott == "nf" else f"https://net52.cc/mobile/{ott}/search.php?s={q}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req) as response:
            data = response.read().decode('utf-8')
            print(f"{ott} -> {data}")
    except Exception as e:
        pass
