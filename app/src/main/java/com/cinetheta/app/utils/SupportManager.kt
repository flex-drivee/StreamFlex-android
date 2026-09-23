package com.cinetheta.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SupportManager {
    const val BINANCE_PAY_ID = "1041683310"
    const val USDT_TRC20_ADDRESS = "TJEbUfurBzdNhFARk6STdzNKAKpuQR5g6j"

    const val ADSTERRA_URL = "https://ensueddenied.com/jg7gcgvu?key=a0919ba886af754e302a4630d7efcf1f"
    const val MONETAG_URL = "https://omg10.com/4/11839109"

    private const val PREFS_NAME = "cinetheta_support_prefs"
    private const val KEY_APP_OPEN_COUNT = "app_open_count"
    private const val KEY_LAST_SHOWN_TIME = "last_support_prompt_time"
    private const val KEY_AD_ROTATION_INDEX = "ad_rotation_index"
    private const val COOLDOWN_MILLIS = 6L * 60L * 60L * 1000L // 6 hours

    // Track when user tapped to watch an ad
    private var adClickTimestamp: Long = 0L
    private var inMemoryAdCounter: Int = 0
    const val MIN_AD_WATCH_DURATION_MS = 20_000L

    fun recordAdClick() {
        adClickTimestamp = System.currentTimeMillis()
    }

    /**
     * Alternates between Adsterra and Monetag, ensuring Adsterra comes 1st, then Monetag.
     */
    fun getRotatedAdUrl(context: Context? = null): String {
        val index = if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getInt(KEY_AD_ROTATION_INDEX, 0)
            prefs.edit().putInt(KEY_AD_ROTATION_INDEX, current + 1).apply()
            current
        } else {
            inMemoryAdCounter++
        }
        return if (index % 2 == 0) ADSTERRA_URL else MONETAG_URL
    }

    /**
     * Opens the ad URL in the user's browser and records click timestamp.
     */
    fun openAd(context: Context) {
        recordAdClick()
        try {
            val url = getRotatedAdUrl(context)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Called when the app is resumed (ON_RESUME) to check if the user returned from an ad.
     * - If user stayed for at least 20s: triggers [onQualifiesForDialog] to show the big popup.
     * - If user returned before 20s: shows a Toast message with heart.
     */
    fun onAppResumeFromAd(context: Context, onQualifiesForDialog: () -> Unit) {
        val clickTime = adClickTimestamp
        if (clickTime <= 0L) return
        adClickTimestamp = 0L // Consume the click so it only fires once

        val elapsedMillis = System.currentTimeMillis() - clickTime
        if (elapsedMillis >= MIN_AD_WATCH_DURATION_MS) {
            onQualifiesForDialog()
        } else {
            Toast.makeText(
                context,
                "You came back earlier than 20s, but Thanks! ❤️",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Checks whether the non-forceful homepage support prompt should be shown.
     * Shows only if the app has been launched at least 2 times and 6 hours have passed since last shown.
     */
    fun shouldShowHomeSupportPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val openCount = prefs.getInt(KEY_APP_OPEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_APP_OPEN_COUNT, openCount).apply()

        val lastShown = prefs.getLong(KEY_LAST_SHOWN_TIME, 0L)
        val now = System.currentTimeMillis()
        return openCount >= 2 && (now - lastShown > COOLDOWN_MILLIS)
    }

    /**
     * Marks the prompt as shown/dismissed, resetting the cooldown.
     */
    fun markSupportPromptShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SHOWN_TIME, System.currentTimeMillis()).apply()
    }
}

/**
 * CineTheta branded Logo Badge with rounded corners, subtle glowing border, and dark backdrop.
 */
@Composable
fun CineThetaLogoBadge(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 64.dp
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val logoResId = remember(context) {
        val id = context.resources.getIdentifier("ic_launcher_foreground_logo", "drawable", context.packageName)
        if (id != 0) id else context.applicationInfo.icon
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141419))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFE50914), Color(0xFFB81D24), Color(0xFF331114))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (logoResId != 0) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "CineTheta Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

/**
 * Observes lifecycle ON_RESUME to detect when the user returns from viewing an ad.
 * Automatically checks whether 20 seconds have passed:
 * - If >= 20s: triggers [onShowThankYouDialog]
 * - If < 20s: shows a Toast message with heart
 */
@Composable
fun AdReturnLifecycleTracker(
    context: Context,
    onShowThankYouDialog: () -> Unit
) {
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                SupportManager.onAppResumeFromAd(context, onShowThankYouDialog)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Beautiful in-app popup shown when user returns to the app after viewing an ad for at least 20 seconds.
 * Features official CineTheta app logo, gratitude message, and confirmation badge.
 */
@Composable
fun ThankYouSupportDialog(
    onDismiss: () -> Unit,
    onOpenAdAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181E),
        shape = RoundedCornerShape(22.dp),
        icon = {
            CineThetaLogoBadge(size = 68.dp)
        },
        title = {
            Text(
                text = "Thank You for Supporting CineTheta!",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Thank you so much for taking the time to browse our sponsor for 20+ seconds! ✨\n\nYour support directly covers API servers, provider scrapers, and app maintenance with zero in-video interruptions!",
                    fontSize = 13.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF26A17B).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF26A17B).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("❤️ ", fontSize = 14.sp)
                        Text(
                            text = "20s+ Browsing Complete — Thank You!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4ADE80)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Awesome, Got It! ❤️", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onOpenAdAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Sponsor Page Again 🔗", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
    )
}

/**
 * Non-forceful homepage dialog (similar to Phisher in Cloudstream) allowing users to either:
 * 1. Watch a quick ad for free contribution.
 * 2. Open full crypto donation options.
 * 3. Dismiss freely without any restrictions.
 */
@Composable
fun HomeSupportDialog(
    onWatchAd: () -> Unit,
    onOpenDonations: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181E),
        shape = RoundedCornerShape(22.dp),
        icon = {
            CineThetaLogoBadge(size = 64.dp)
        },
        title = {
            Text(
                text = "Enjoying CineTheta?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Help keep CineTheta free, ad-free during playback, and actively maintained! If you can't donate money, browsing a sponsor ad for at least 20 seconds is completely free and directly supports server maintenance.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // ── Option 1: Watch Ad (Free) ──────────────────────────────────
                Button(
                    onClick = onWatchAd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎬 Watch Ad (Browse 20s+ to Support)", fontWeight = FontWeight.Bold, color = Color.White)
                }

                // ── Option 2: Donate (Crypto / Binance) ─────────────────────────
                OutlinedButton(
                    onClick = onOpenDonations,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3BA2F).copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF3BA2F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("☕ Donate via Binance / USDT", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Maybe Later", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

/**
 * Full Support Dialog showing all support methods (Free Ad, Binance Pay, USDT TRC-20).
 */
@Composable
fun FullSupportCineThetaDialog(
    onDismiss: () -> Unit,
    context: Context,
    onWatchAdSuccess: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181E),
        shape = RoundedCornerShape(22.dp),
        icon = {
            CineThetaLogoBadge(size = 64.dp)
        },
        title = {
            Text(
                text = "Support CineTheta",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CineTheta is free and open-source. Your contribution helps maintain scraping servers, API costs, and continuous updates!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                // ── Card 1: Free Ad Contribution ──────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF23232A))
                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable {
                            SupportManager.openAd(context)
                            onWatchAdSuccess?.invoke()
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎬", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Watch an Ad to Support",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFFF5252)
                            )
                        }

                        IconButton(
                            onClick = {
                                SupportManager.openAd(context)
                                onWatchAdSuccess?.invoke()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Watch Ad",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% Free • Please browse the page for at least 20s • Opens in browser",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // ── Card 2: Binance Pay ID ─────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF23232A))
                        .border(1.dp, Color(0xFFF3BA2F).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { SupportManager.copyToClipboard(context, "Binance Pay ID", SupportManager.BINANCE_PAY_ID) }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFF3BA2F), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("B", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Binance Pay ID",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFFF3BA2F)
                            )
                        }

                        IconButton(
                            onClick = { SupportManager.copyToClipboard(context, "Binance Pay ID", SupportManager.BINANCE_PAY_ID) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Binance Pay ID",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SupportManager.BINANCE_PAY_ID,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Instant 0% fee transfer • Tap to copy",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                // ── Card 3: USDT (TRC-20) ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF23232A))
                        .border(1.dp, Color(0xFF26A17B).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { SupportManager.copyToClipboard(context, "USDT TRC-20 Address", SupportManager.USDT_TRC20_ADDRESS) }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF26A17B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("₮", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "USDT (Tron / TRC-20)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF26A17B)
                            )
                        }

                        IconButton(
                            onClick = { SupportManager.copyToClipboard(context, "USDT TRC-20 Address", SupportManager.USDT_TRC20_ADDRESS) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy USDT Address",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SupportManager.USDT_TRC20_ADDRESS,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = Color.White,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = "Trust Wallet, MetaMask, or any exchange • Tap to copy",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                // ── Card 4: Community ──────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF23232A))
                        .border(1.dp, Color(0xFF0088CC).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Join Our Community",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF0088CC)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/CinethetaChat"))
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Chat", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/CinethetaApp"))
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Channel", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/KxzD8nA8vb"))
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Discord", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/**
 * Ad countdown dialog shown when NetMirror t_hash_t security token is expired or missing.
 * The NetMirror headless bypass takes ~37 seconds.
 * Gives the user a 5-second countdown to open a sponsor ad for 20 seconds to support server costs,
 * while the bypass continues concurrently in the background.
 */
@Composable
fun NetMirrorBypassAdDialog(
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
        onWatchAd()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181E),
        shape = RoundedCornerShape(20.dp),
        icon = {
            CineThetaLogoBadge(size = 60.dp)
        },
        title = {
            Text(
                text = "Fetching OTT Stream Tokens",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Background status banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF23232A))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFF5252)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OTT Security Bypass running (~37s)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF8A80)
                    )
                }

                Text(
                    text = "NetMirror / OTT stream security tokens (t_hash_t) are being generated in the background. This process takes ~37 seconds to complete.\n\nWhile we prepare your streams, please browse our sponsor page for at least 20 seconds to help cover app maintenance & server costs!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // Countdown badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE50914).copy(alpha = 0.12f))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (countdown > 0) "Opening sponsor ad in ${countdown}s..." else "Opening sponsor ad...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF5252)
                    )
                }

                // Primary Action Button
                Button(
                    onClick = onWatchAd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (countdown > 0) "🎬 Watch Ad in ${countdown}s (or Tap Now)" else "🎬 Watch Ad Now",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Wait Here (Skip Ad)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    )
}
