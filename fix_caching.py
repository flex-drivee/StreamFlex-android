import re

with open("app/src/main/java/com/streamflex/extractors/netmirror/NetMirrorExtractor.kt", "r") as f:
    content = f.read()

old_block = """            var tHashT = ""
            val verifyUrl = "$baseUrl/verify.php"
            
            // Try to resolve using WebView. 
            // Note: WebViewResolver now uses the real Android User-Agent so Turnstile doesn't loop!
            val solved = com.streamflex.core.network.interceptor.WebViewResolver.resolveUsingWebView(
                com.streamflex.app.StreamFlexApplication.instance,
                verifyUrl,
                requiredCookie = "t_hash_t"
            )
            
            if (solved) {
                val cookies = android.webkit.CookieManager.getInstance().getCookie(verifyUrl) ?: ""
                val match = Regex("t_hash_t=([^;]+)").find(cookies)
                if (match != null) {
                    tHashT = match.groupValues[1]
                    com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "WebView Bypass success, got t_hash_t: $tHashT")
                }
            }
            
            if (tHashT.isBlank()) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "Failed to acquire valid t_hash_t cookie via WebViewResolver!")
                return@withContext emptyResult()
            }"""

new_block = """            var tHashT = ""
            val verifyUrl = "$baseUrl/verify.php"
            
            val existingCookies = android.webkit.CookieManager.getInstance().getCookie(verifyUrl) ?: ""
            val existingMatch = Regex("t_hash_t=([^;]+)").find(existingCookies)
            if (existingMatch != null && existingMatch.groupValues[1].isNotBlank()) {
                tHashT = existingMatch.groupValues[1]
                com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "Found existing t_hash_t in CookieManager: $tHashT")
            }
            
            if (tHashT.isBlank()) {
                com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "No t_hash_t found, launching WebViewResolver...")
                val solved = com.streamflex.core.network.interceptor.WebViewResolver.resolveUsingWebView(
                    com.streamflex.app.StreamFlexApplication.instance,
                    verifyUrl,
                    requiredCookie = "t_hash_t"
                )
                
                if (solved) {
                    val cookies = android.webkit.CookieManager.getInstance().getCookie(verifyUrl) ?: ""
                    val match = Regex("t_hash_t=([^;]+)").find(cookies)
                    if (match != null) {
                        tHashT = match.groupValues[1]
                        com.streamflex.core.utils.StreamLogger.debug("NetMirrorExtractor", "WebView Bypass success, got t_hash_t: $tHashT")
                    }
                }
            }
            
            if (tHashT.isBlank()) {
                com.streamflex.core.utils.StreamLogger.error("NetMirrorExtractor", "Failed to acquire valid t_hash_t cookie via WebViewResolver!")
                return@withContext emptyResult()
            }"""

if old_block in content:
    with open("app/src/main/java/com/streamflex/extractors/netmirror/NetMirrorExtractor.kt", "w") as f:
        f.write(content.replace(old_block, new_block))
    print("Patched!")
else:
    print("Old block not found!")
