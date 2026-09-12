import urllib.request
for ott in ["pv", "hs"]:
    url = f"https://net52.cc/{ott}/search.php?s=Weak+Hero"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req) as response:
            print(f"{ott} -> {response.read().decode('utf-8')}")
    except Exception as e:
        print(f"{ott} error -> {e}")
