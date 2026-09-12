import re

def normalize(title):
    return title.lower().replace("&", "and")

def similarity(first, second):
    a = re.sub(r'^(the|a|an)\s+', '', normalize(first))
    b = re.sub(r'^(the|a|an)\s+', '', normalize(second))
    if not a or not b: return 0.0
    if a == b: return 1.0

    aNoSpace = a.replace(" ", "")
    bNoSpace = b.replace(" ", "")

    matchA = re.search(r'\b' + re.escape(a) + r'\b', b)
    matchB = re.search(r'\b' + re.escape(b) + r'\b', a)
    if (matchA and len(b) > 3) or (matchB and len(a) > 3):
        indexA = matchA.start() if matchA else float('inf')
        indexB = matchB.start() if matchB else float('inf')
        if indexA == 0 or indexB == 0:
            return 0.90
        else:
            return 0.80

    if (aNoSpace in bNoSpace and len(aNoSpace) > 4) or (bNoSpace in aNoSpace and len(bNoSpace) > 4):
        return 0.85
    
    return 0.5 # Dummy fallback

def score(expectedTitle, season, episode, resultTitle, isMovie):
    mainExpectedTitle = expectedTitle.split(" -")[0].split("-")[0].strip()
    expectedToUse = mainExpectedTitle if len(mainExpectedTitle) > 3 else expectedTitle
    
    baseResultTitle = re.sub(r'(?i)\b(?:season|s)\s*0*\d+.*', '', resultTitle)
    baseResultTitle = re.sub(r'(?i)\b(?:episode|ep|e)\s*0*\d+.*', '', baseResultTitle)
    baseResultTitle = re.sub(r'(?i)\b\d+x\d+.*', '', baseResultTitle)
    baseResultTitle = re.sub(r'(?i)hindi dub.*', '', baseResultTitle).strip()
    if not baseResultTitle: baseResultTitle = resultTitle
    
    sim1 = similarity(expectedTitle, baseResultTitle)
    sim2 = similarity(expectedToUse, baseResultTitle)
    sim = max(sim1, sim2)

    print("Similarity:", sim)
    if sim < 0.75: return -1
    if isMovie: return -1

    score = int(sim * 50)
    title = normalize(resultTitle)

    matchesSeason = (f"season {season}" in title or f"season{season}" in title or 
                     f"s{season:02d}" in title or f"s{season}" in title or 
                     f"season {season:02d}" in title)
    
    if matchesSeason:
        score += 60
    else:
        mentionedSeasons = [int(m) for m in re.findall(r'\b(?:season|s)\s*0*(\d{1,2})\b', title, re.IGNORECASE)]
        if mentionedSeasons and season not in mentionedSeasons:
            expectedLower = expectedTitle.lower()
            shouldPenalize = True
            for m in mentionedSeasons:
                if str(m) in expectedLower:
                    shouldPenalize = False
                    score += 60
                    break
            if shouldPenalize:
                score -= 40
                
    if f"s{season:02d}e{episode:02d}" in title: score += 100
    if f"s{season}e{episode}" in title: score += 100
    if f"{season}x{episode:02d}" in title: score += 90
    if f"episode {episode}" in title or f"episode {episode:02d}" in title: score += 40
    if f"ep {episode}" in title or f"ep {episode:02d}" in title: score += 35

    return score

print("Weak Hero vs Weak Hero Class 1")
print(score("Weak Hero", 1, 1, "Weak Hero Class 1", False))

