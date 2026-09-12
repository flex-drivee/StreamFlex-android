import urllib.request
import urllib.parse
import json

for q in ["Weak Hero", "Weak Hero Class 1", "Weak+Hero"]:
    url = f"https://net52.cc/mobile/search.php?s={urllib.parse.quote(q)}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req) as response:
            data = response.read().decode('utf-8')
            print(f"nf ({q}) -> {data[:100]}...")
    except Exception as e:
        print(f"Error for {q}: {e}")
