package com.cinetheta.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SupportManager {
    const val BINANCE_PAY_ID = "1041683310"
    const val USDT_TRC20_ADDRESS = "TJEbUfurBzdNhFARk6STdzNKAKpuQR5g6j"

    const val MONETAG_URL = "https://omg10.com/4/11839109"
    const val ADSTERRA_URL = "https://www.profitableratecpmnetwork.com/jg7gcgvu?key=a0919ba886af754e302a4630d7efcf1f"

    private const val PREFS_NAME = "cinetheta_support_prefs"
    private const val KEY_APP_OPEN_COUNT = "app_open_count"
    private const val KEY_LAST_SHOWN_TIME = "last_support_prompt_time"
    private const val COOLDOWN_MILLIS = 72L * 60L * 60L * 1000L // 72 hours (3 days)

    /**
     * Alternates 50/50 between Monetag and Adsterra to balance earnings across both ad networks.
     */
    fun getRotatedAdUrl(): String {
        return if (System.currentTimeMillis() % 2L == 0L) MONETAG_URL else ADSTERRA_URL
    }

    /**
     * Opens the ad URL in the user's browser.
     */
    fun openAd(context: Context) {
        try {
            val url = getRotatedAdUrl()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening sponsor ad… Thank you for your support!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
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
     * Shows only if the app has been launched at least 2 times and 72 hours have passed since last shown.
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
     * Marks the prompt as shown/dismissed, resetting the 72-hour cooldown.
     */
    fun markSupportPromptShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SHOWN_TIME, System.currentTimeMillis()).apply()
    }
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
        containerColor = Color(0xFF1E1E22),
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFFE50914).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE50914),
                    modifier = Modifier.size(28.dp)
                )
            }
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
                    text = "Help keep CineTheta free, ad-free during playback, and actively maintained! If you can't donate money, watching a 10-second sponsor ad is free and directly supports server maintenance.",
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
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎬 Watch Ad to Support (Free)", fontWeight = FontWeight.Bold, color = Color.White)
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
    context: Context
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E22),
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFFE50914).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE50914),
                    modifier = Modifier.size(28.dp)
                )
            }
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
                        .background(Color(0xFF28282D))
                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { SupportManager.openAd(context) }
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
                            onClick = { SupportManager.openAd(context) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = "Watch Ad",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% Free • Takes only 10s • Opens in browser",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // ── Card 2: Binance Pay ID ─────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF28282D))
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
                        .background(Color(0xFF28282D))
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
