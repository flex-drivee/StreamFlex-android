import urllib.request
import urllib.parse
import json

url = f"https://net52.cc/search.php?s=Weak+Hero"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        data = response.read().decode('utf-8')
        print(f"desktop -> {data}")
except Exception as e:
    print(f"Error: {e}")
