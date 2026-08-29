import requests

def check_domain(name, url):
    try:
        r = requests.get(url, timeout=10, headers={'User-Agent': 'Mozilla/5.0'})
        print(f"[{r.status_code}] {name}: {url}")
        if name == "MovieBox":
            print(f"MovieBox API Response Snippet: {r.text[:100]}")
    except Exception as e:
        print(f"[ERROR] {name} ({url}) - {e}")

# MovieBox uses API
check_domain("MovieBox Search API", "https://api3.aoneroom.com/wefeed-mobile-bff/subject-api/search?keyword=spider")

# AnimeDekho
check_domain("AnimeDekho Site", "https://animedekho.app/")

# 4kHdHub
check_domain("4kHdHub Site", "https://4khdhub.com/")

# NetMirror
check_domain("NetMirror Site", "https://netmirror.app/")

