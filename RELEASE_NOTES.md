# CineTheta v1.0.1 Release Notes

🎉 **CineTheta v1.0.1** is here! This release brings major network and DNS connectivity fixes for blocked streaming providers, direct native search for HDHub4u, optimized server priority, and improved ad support rewards!

---

### 🚀 What's New & Highlights

#### 🌐 ISP & Cloudflare Bypass (Toonstream & AnimeDekho Restored)
- **Smart Anycast IP Substitution**:
  - Automatically replaces ISP-blacklisted Cloudflare IP ranges (`188.114.96.x` / `188.114.97.x`) with clean Anycast IPs (`104.21.33.54`, `172.67.189.12`, etc.) via Secure DNS-over-HTTPS (DoH).
  - Prioritizes IPv4 over IPv6 to prevent DNS resolution deadlocks and silent timeouts on mobile carrier networks.
  - Fully restores access to **Toonstream** and **AnimeDekho** in affected regions.

#### 🔍 HDHub4u Direct Native Search
- Replaced the failing external pingora proxy with native WordPress HTML query extraction (`$baseUrl/?s=...`).
- Direct extraction from card thumbnails (`li.thumb`) with automatic Typesense fallback for maximum search coverage.
- Lightning-fast search results without external third-party proxy dependencies.

#### ⚡ Resilient Mirror Fallback & Fail-Fast Network Probes
- **AnimeDekho Mirror Fallback**: Added automatic mirror failover (`hindisubanime.co`) with an 8-second safety deadline.
- **Toonstream Resolved Domain Priority**: Resolved domain queried first with strict 8-second timeout.
- **Fail-Fast Probes**: Probe timeouts reduced to 3.5s with `X-No-Retry` headers to prevent app freezes on unresponsive provider domains.
- Cloudflare challenge solver now skips lightweight HEAD probes and JSON API endpoints.

#### 🎬 Server Stream Priority & Quota Protection
- Reordered player server sources so high-quota-limited servers (such as Google Drive and Buzzer links) are placed at the end of the list.
- Primary and fast streaming servers are prioritized first to ensure instant playback without quota errors.

#### 🧭 Provider Selection Improvements
- Replaced the legacy "All-in-one" option with explicit provider selection.
- Selecting "None" prompts the user with a helpful reminder to choose a provider before playback.

#### ⏱️ Smart Support Ad Timing (20-Second Rule)
- Upgraded the ad-support flow to ensure authentic support visits:
  - **Early Return (< 20 seconds)**: Displays a warm toast message: *"You came back earlier than 20s, but Thanks! ❤️"*.
  - **Full Support (≥ 20 seconds)**: Displays the full branded Thank-You dialog with the app icon.

#### 🌐 Website & Version Archive
- Landing page updated with direct download buttons for **v1.0.1**.
- Added a dedicated **Older Versions** section on the website so users can easily download previous stable releases (including `v1.0.0`) at any time.

---

### 📦 Release Asset & Package Information
- **App Name**: CineTheta
- **Package ID**: `com.cinetheta.app`
- **Release Tag**: `v1.0.1`
- **Release Title**: `CineTheta v1.0.1 — Fast Provider Resolvers & Smart Ad Support`
- **Version Name**: `1.0.1`
- **Version Code**: `6`
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **APK Filename**: `CineTheta-v1.0.1.apk`
- **APK Size**: ~17.4 MB (18,293,439 bytes)

---

### 💖 Support CineTheta Development
If you love using CineTheta and want to help keep the scrapers updated and servers alive:
- **Binance Pay ID**: `1041683310` *(Instant, 0% fees)*
- **USDT (Tron / TRC-20)**: `TJEbUfurBzdNhFARk6STdzNKAKpuQR5g6j`
