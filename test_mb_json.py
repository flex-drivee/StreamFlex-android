import urllib.request
import re
import ssl
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch_links():
    req = urllib.request.Request("https://api6.aoneroom.com/wefeed-mobile-bff/tab/home", headers={'User-Agent': 'Mozilla/5.0'})
    try:
        html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')
        print(html[:500])
    except Exception as e:
        print(f"Error: {e}")

fetch_links()
