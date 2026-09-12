import urllib.request
import re
import ssl
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def fetch_links(url):
    print(f"\nFetching {url}")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')
        
        # Extract <ul id="menu... > to </ul>
        nav_match = re.search(r'<ul[^>]*id="menu-[^>]*>(.*?)</ul>', html, re.IGNORECASE | re.DOTALL)
        if nav_match:
            nav_html = nav_match.group(1)
            links = re.findall(r'<a[^>]*href="(.*?)"[^>]*>(.*?)</a>', nav_html, re.IGNORECASE)
            print("--- Menu Links ---")
            for href, text in links:
                text_clean = re.sub(r'<[^>]+>', '', text).strip()
                print(f"{text_clean} -> {href}")
        else:
            print("No menu found")
            links = re.findall(r'<a[^>]*href="(.*?)"[^>]*>(.*?)</a>', html, re.IGNORECASE)
            for href, text in links[:20]:
                text_clean = re.sub(r'<[^>]+>', '', text).strip()
                if text_clean:
                    print(f"{text_clean} -> {href}")
                
        
        print("--- Home Articles ---")
        articles = re.findall(r'<article[^>]*>', html, re.IGNORECASE)
        print(f"Found {len(articles)} articles")
        
    except Exception as e:
        print(f"Error: {e}")

fetch_links('https://animedekho.app/')
fetch_links('https://toon-stream.site/')
