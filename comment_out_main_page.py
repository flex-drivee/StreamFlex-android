import os
import re

providers = [
    '/sdcard/AndroidIDEProjects/StreamFlex-android/app/src/main/java/com/cinetheta/providers/moviebox/MovieBoxProvider.kt',
    '/sdcard/AndroidIDEProjects/StreamFlex-android/app/src/main/java/com/cinetheta/providers/animedekho/AnimeDekhoProvider.kt',
    '/sdcard/AndroidIDEProjects/StreamFlex-android/app/src/main/java/com/cinetheta/providers/toonstream/ToonStreamProvider.kt'
]

for provider in providers:
    with open(provider, 'r') as f:
        content = f.read()
    
    # We will just remove the "override suspend fun getMainPage()" and everything after it, 
    # since it's the last method in the file, and then add a closing brace.
    # Wait, for MovieBox, there's `fetchAndScrape` right before it!
    
    # Let's just find `private suspend fun fetchAndScrape` and remove everything from there to the end of the file except the last brace
    
    if "private suspend fun fetchAndScrape" in content:
        parts = content.split("private suspend fun fetchAndScrape")
        new_content = parts[0].rstrip() + "\n}\n"
        with open(provider, 'w') as f:
            f.write(new_content)
    elif "override suspend fun getMainPage" in content:
        parts = content.split("override suspend fun getMainPage")
        new_content = parts[0].rstrip() + "\n}\n"
        with open(provider, 'w') as f:
            f.write(new_content)

