def similarity(s1, s2):
    s1, s2 = s1.lower(), s2.lower()
    if s1 == s2: return 1.0
    from difflib import SequenceMatcher
    return SequenceMatcher(None, s1, s2).ratio()

print(similarity("Weak Hero", "Weak Hero Class 1"))
print(similarity("Weak Hero Season 1", "Weak Hero Class 1"))
