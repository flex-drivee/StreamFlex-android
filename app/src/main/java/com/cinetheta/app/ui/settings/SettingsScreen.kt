package com.cinetheta.app.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToExtensionTest: () -> Unit = {},
    onPluginChanged: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val appTheme by viewModel.appTheme.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    val enableSubtitles by viewModel.enableSubtitles.collectAsState()
    val subtitleTextColor by viewModel.subtitleTextColor.collectAsState()
    val subtitleBgColor by viewModel.subtitleBgColor.collectAsState()
    val cellularData by viewModel.cellularData.collectAsState()
    val developerMode by viewModel.developerMode.collectAsState()
    val decoderMode by viewModel.decoderMode.collectAsState()
    val dohProvider by viewModel.dohProvider.collectAsState()
    val playerVideoQuality by viewModel.playerVideoQuality.collectAsState()
    val preferredPlayer by viewModel.preferredPlayer.collectAsState()

    val scrollState = rememberLazyListState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPlayerDialog by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showDohDialog by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showMovieBoxSettings by remember { mutableStateOf(false) }
    var showPlayerQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleTextColorDialog by remember { mutableStateOf(false) }
    var showSubtitleBgColorDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showThankYouDialog by remember { mutableStateOf(false) }
    var showIssueReportDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }
    
    val providerRepository = com.cinetheta.app.di.ProviderModule.repository
    var selectedProviderName by remember { 
        mutableStateOf(providerRepository.provider(providerRepository.selectedProviderId ?: "")?.name ?: "None") 
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            
            // --- General ---
            item {
                SettingsGroup("General") {
                    SettingsTile(
                        icon = Icons.Outlined.Palette,
                        title = "App Theme",
                        subtitle = getThemeDisplayName(appTheme),
                        onTap = { showThemeDialog = true }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Language,
                        title = "Language",
                        subtitle = "English (US)",
                        onTap = { android.widget.Toast.makeText(context, "More languages coming soon!", android.widget.Toast.LENGTH_SHORT).show() }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.SystemUpdate,
                        title = "Updates",
                        subtitle = "Check for app updates",
                        isLast = true,
                        onTap = {
                            android.widget.Toast.makeText(context, "Checking for updates…", android.widget.Toast.LENGTH_SHORT).show()
                            coroutineScope.launch(Dispatchers.IO) {
                                com.cinetheta.app.utils.AppUpdater.checkUpdate(context, isManualCheck = true)
                            }
                        }
                    )
                }
            }

            // --- Player ---
            item {
                SettingsGroup("Player") {
                    SettingsTile(
                        icon = Icons.Outlined.Autorenew,
                        title = "Auto-Play Next Episode",
                        subtitle = if (autoPlayNext) "Enabled" else "Disabled",
                        trailing = { Switch(
                            checked = autoPlayNext,
                            onCheckedChange = { viewModel.setAutoPlayNext(it) },
                            colors = appSwitchColors()
                        ) },
                        onTap = { viewModel.setAutoPlayNext(!autoPlayNext) }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.HighQuality,
                        title = "Default Video Quality",
                        subtitle = "Current: $playerVideoQuality",
                        onTap = { showPlayerQualityDialog = true }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.PlayCircle,
                        title = "Preferred Player",
                        subtitle = preferredPlayer.title,
                        onTap = { showPlayerDialog = true }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Memory,
                        title = "Decoder Mode",
                        subtitle = com.cinetheta.player.core.DecoderMode.fromKey(decoderMode).title,
                        isLast = true,
                        onTap = { showDecoderDialog = true }
                    )
                }
            }

            // --- Subtitles ---
            item {
                SettingsGroup("Subtitles") {
                    SettingsTile(
                        icon = Icons.Outlined.Subtitles,
                        title = "Enable Subtitles by Default",
                        subtitle = if (enableSubtitles) "Always show subtitles when available" else "Disabled",
                        trailing = { Switch(
                            checked = enableSubtitles,
                            onCheckedChange = { viewModel.setEnableSubtitles(it) },
                            colors = appSwitchColors()
                        ) },
                        onTap = { viewModel.setEnableSubtitles(!enableSubtitles) }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.FormatColorText,
                        title = "Subtitle Text Color",
                        subtitle = getSubtitleColorName(subtitleTextColor),
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parseColorHex(subtitleTextColor, Color.White))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        },
                        onTap = { showSubtitleTextColorDialog = true }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.FormatColorFill,
                        title = "Subtitle Background Color",
                        subtitle = getSubtitleBgColorName(subtitleBgColor),
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parseColorHex(subtitleBgColor, Color(0x80000000)))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        },
                        onTap = { showSubtitleBgColorDialog = true }
                    )
                    SettingsDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Live Subtitle Preview",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "The quick brown fox jumps over the lazy dog.",
                                color = parseColorHex(subtitleTextColor, Color.White),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(parseColorHex(subtitleBgColor, Color(0x80000000)))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }


            // --- Downloads ---
            item {
                SettingsGroup("Downloads") {
                    SettingsTile(
                        icon = Icons.Outlined.Folder,
                        title = "SaveTheta Download Manager",
                        subtitle = "Manage active downloads, pause, continue & offline videos",
                        isLast = true,
                        onTap = onNavigateToDownloads
                    )
                }
            }
            
            // --- Accounts ---
            item {
                SettingsGroup("Accounts") {
                    SettingsTile(
                        icon = Icons.Outlined.AccountCircle,
                        title = "Manage Accounts",
                        subtitle = "Configure Subtitles and Tracking Services",
                        isLast = true,
                        onTap = { android.widget.Toast.makeText(context, "Coming soon!", android.widget.Toast.LENGTH_SHORT).show() }
                    )
                }
            }

            // --- Network ---
            item {
                SettingsGroup("Network") {
                    SettingsTile(
                        icon = Icons.Outlined.SignalCellularAlt,
                        title = "Stream on Cellular Data",
                        subtitle = if (cellularData) "Enabled" else "Disabled",
                        trailing = { Switch(
                            checked = cellularData,
                            onCheckedChange = { viewModel.setCellularData(it) },
                            colors = appSwitchColors()
                        ) },
                        onTap = { viewModel.setCellularData(!cellularData) }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Dns,
                        title = "DNS over HTTPS",
                        subtitle = try {
                            com.cinetheta.core.network.DohProvider.valueOf(dohProvider).title
                        } catch (_: Exception) {
                            com.cinetheta.core.network.DohProvider.GOOGLE.title
                        },
                        isLast = true,
                        onTap = { showDohDialog = true }
                    )
                }
            }

            // --- Extensions (Providers) ---
            item {
                SettingsGroup("Extensions") {
                    SettingsTile(
                        icon = Icons.Outlined.Extension,
                        title = "Manage Extensions",
                        subtitle = "Current: $selectedProviderName",
                        onTap = { showProviderDialog = true }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.CheckCircleOutline,
                        title = "Test Extensions",
                        subtitle = "Run automated tests on installed providers",
                        isLast = true,
                        onTap = onNavigateToExtensionTest
                    )
                }
            }

            // --- App Data ---
            item {
                SettingsGroup("App Data") {
                    SettingsTile(
                        icon = Icons.Outlined.CleaningServices,
                        title = "Clear Cache",
                        subtitle = "Free up temporary data",
                        onTap = {
                            val ok = viewModel.clearCache()
                            val msg = if (ok) "Cache cleared successfully!" else "Failed to clear cache"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.DeleteForever,
                        title = "Factory Reset",
                        subtitle = "Wipe all app data and settings",
                        isLast = true,
                        onTap = { showFactoryResetDialog = true }
                    )
                }
            }

            // --- Developer Options ---
            item {
                SettingsGroup("Developer") {
                    SettingsTile(
                        icon = Icons.Outlined.DeveloperMode,
                        title = "Developer Options",
                        subtitle = "Show advanced debugging options",
                        trailing = { Switch(
                            checked = developerMode,
                            onCheckedChange = { viewModel.setDeveloperMode(it) },
                            colors = appSwitchColors()
                        ) },
                        onTap = { viewModel.setDeveloperMode(!developerMode) },
                        isLast = !developerMode
                    )
                    if (developerMode) {
                        SettingsDivider()
                        SettingsTile(
                            icon = Icons.Outlined.BugReport,
                            title = "View Logs",
                            subtitle = "Check application logs for errors",
                            onTap = { android.widget.Toast.makeText(context, "Coming soon!", android.widget.Toast.LENGTH_SHORT).show() }
                        )
                        SettingsDivider()
                        SettingsTile(
                            icon = Icons.Outlined.Speed,
                            title = "Network Inspector",
                            subtitle = "Monitor API requests and responses",
                            isLast = true,
                            onTap = { android.widget.Toast.makeText(context, "Coming soon!", android.widget.Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }

            // --- Community ---
            item {
                SettingsGroup("Community") {
                    SettingsTile(
                        icon = Icons.Outlined.Chat,
                        title = "Telegram Chat",
                        subtitle = "Join the CineTheta community chat",
                        onTap = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/CinethetaChat"))
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Campaign,
                        title = "Telegram Channel",
                        subtitle = "Official CineTheta updates",
                        onTap = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/CinethetaApp"))
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Forum,
                        title = "Discord Server",
                        subtitle = "Join our official Discord",
                        isLast = true,
                        onTap = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://discord.gg/KxzD8nA8vb"))
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                    )
                }
            }

            // --- About ---
            item {
                SettingsGroup("About") {
                    SettingsTile(
                        icon = Icons.Outlined.PersonOutline,
                        title = "Developer",
                        subtitle = "Developed by Mani (flex-drivee).",
                        trailing = { Spacer(modifier = Modifier.width(0.dp)) },
                        onTap = { /* No external link */ }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Language,
                        title = "Official Website",
                        subtitle = "cinetheta.github.io",
                        onTap = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://cinetheta.github.io"))
                            context.startActivity(intent)
                        }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.BugReport,
                        title = "Report an Issue / Feedback",
                        subtitle = "Report bugs or broken streams to GitHub admin tracker",
                        onTap = { showIssueReportDialog = true }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Filled.Favorite,
                        title = "Support CineTheta",
                        subtitle = "Donate via Binance Pay or USDT",
                        onTap = { showSupportDialog = true }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Info,
                        title = "Version",
                        subtitle = "CineTheta v${com.cinetheta.app.BuildConfig.VERSION_NAME}",
                        trailing = { Spacer(modifier = Modifier.width(0.dp)) }, // No chevron
                        onTap = { /* No action needed for version tile */ }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Group,
                        title = "Credits & Special Thanks",
                        subtitle = "Special thanks to Phiser, NivinCNC, and Cloudstream.",
                        trailing = { Spacer(modifier = Modifier.width(0.dp)) }, // No chevron
                        isLast = true,
                        onTap = { /* No action needed */ }
                    )
                }
            }
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Select App Theme", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOptionRow(
                            title = "System Default",
                            isSelected = appTheme == "SYSTEM",
                            onClick = { viewModel.setAppTheme("SYSTEM"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "Default Dark Mode",
                            isSelected = appTheme == "SKY_DARK",
                            onClick = { viewModel.setAppTheme("SKY_DARK"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "Default Light Mode",
                            isSelected = appTheme == "SKY_LIGHT",
                            onClick = { viewModel.setAppTheme("SKY_LIGHT"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "Netflix Mode (Dark)",
                            isSelected = appTheme == "NETFLIX",
                            onClick = { viewModel.setAppTheme("NETFLIX"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "Prime Mode (Dark)",
                            isSelected = appTheme == "PRIME",
                            onClick = { viewModel.setAppTheme("PRIME"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "Cinematic Mode (Dark OLED)",
                            isSelected = appTheme == "CINETHETA",
                            onClick = { viewModel.setAppTheme("CINETHETA"); showThemeDialog = false }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showDecoderDialog) {
            AlertDialog(
                onDismissRequest = { showDecoderDialog = false },
                title = { Text("Select Decoder Mode", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.cinetheta.player.core.DecoderMode.entries.forEach { mode ->
                            DecoderOptionRow(
                                mode = mode,
                                isSelected = decoderMode == mode.key,
                                onClick = {
                                    viewModel.setDecoderMode(mode.key)
                                    showDecoderDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDecoderDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showDohDialog) {
            AlertDialog(
                onDismissRequest = { showDohDialog = false },
                title = { Text("Select DNS over HTTPS", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.cinetheta.core.network.DohProvider.entries.forEach { provider ->
                            ThemeOptionRow(
                                title = provider.title,
                                isSelected = dohProvider == provider.name,
                                onClick = {
                                    viewModel.setDohProvider(provider.name)
                                    showDohDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDohDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }


        if (showPlayerQualityDialog) {
            val qualities = listOf("Auto", "1080p", "720p", "480p")
            AlertDialog(
                onDismissRequest = { showPlayerQualityDialog = false },
                title = { Text("Default Video Quality", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        qualities.forEach { q ->
                            ThemeOptionRow(
                                title = when (q) {
                                    "Auto" -> "Auto • Adapts to Network"
                                    "1080p" -> "High (1080p) • Best Quality"
                                    "720p" -> "Standard (720p) • Balanced"
                                    else -> "Data Saver (480p) • Less Data"
                                },
                                isSelected = playerVideoQuality == q,
                                onClick = {
                                    viewModel.setPlayerVideoQuality(q)
                                    showPlayerQualityDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlayerQualityDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showPlayerDialog) {
            val players = VideoPlayer.values().toList()
            AlertDialog(
                onDismissRequest = { showPlayerDialog = false },
                title = { Text("Preferred Player", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        players.forEach { p ->
                            ThemeOptionRow(
                                title = p.title,
                                isSelected = preferredPlayer == p,
                                onClick = {
                                    viewModel.setPreferredPlayer(p)
                                    showPlayerDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlayerDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }



        if (showSubtitleTextColorDialog) {
            val textColorPresets = listOf(
                "White" to "#FFFFFF",
                "Yellow" to "#FFEE58",
                "Bright Yellow" to "#FFFF00",
                "Green" to "#66BB6A",
                "Cyan" to "#26C6DA",
                "Light Blue" to "#42A5F5",
                "Red" to "#EF5350",
                "Orange" to "#FFA726",
                "Amber" to "#FFCA28",
                "Pink" to "#EC407A",
                "Lime" to "#D4E157"
            )
            SubtitleColorPickerDialog(
                title = "Subtitle Text Color",
                initialHex = subtitleTextColor,
                presets = textColorPresets,
                isBackgroundPicker = false,
                currentTextColorHex = subtitleTextColor,
                currentBgColorHex = subtitleBgColor,
                onDismiss = { showSubtitleTextColorDialog = false },
                onSelectColor = { newColor ->
                    viewModel.setSubtitleTextColor(newColor)
                    showSubtitleTextColorDialog = false
                }
            )
        }

        if (showSubtitleBgColorDialog) {
            val bgColorPresets = listOf(
                "Translucent Black (50%)" to "#80000000",
                "Transparent (None)" to "#00000000",
                "Solid Black" to "#FF000000",
                "Dark Charcoal (75%)" to "#C0000000",
                "Light Charcoal (25%)" to "#40000000",
                "Deep Navy (50%)" to "#800D1B2A",
                "Dark Gray" to "#80212121",
                "Solid Dark Gray" to "#FF212121"
            )
            SubtitleColorPickerDialog(
                title = "Subtitle Background Color",
                initialHex = subtitleBgColor,
                presets = bgColorPresets,
                isBackgroundPicker = true,
                currentTextColorHex = subtitleTextColor,
                currentBgColorHex = subtitleBgColor,
                onDismiss = { showSubtitleBgColorDialog = false },
                onSelectColor = { newColor ->
                    viewModel.setSubtitleBgColor(newColor)
                    showSubtitleBgColorDialog = false
                }
            )
        }

        if (showProviderDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showProviderDialog = false },
                title = { androidx.compose.material3.Text("Select Provider") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val oldId = providerRepository.selectedProviderId
                                    providerRepository.selectedProviderId = null
                                    selectedProviderName = "None"
                                    showProviderDialog = false
                                    
                                    context.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
                                        .edit().putString("selected_provider", null).apply()
                                        
                                    if (oldId != null) {
                                        onPluginChanged()
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Text("None", modifier = Modifier.weight(1f))
                        }
                        val order = listOf(
                            "moviebox", "netmirror", "castletv", "uhdmovies", "animedekho",
                            "toonstream", "piratexplay", "hdhub4u", "fourkhdhub", "youtube"
                        )
                        providerRepository.enabledProviders().sortedBy { provider ->
                            val idx = order.indexOf(provider.id)
                            if (idx != -1) idx else 999
                        }.forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val oldId = providerRepository.selectedProviderId
                                        providerRepository.selectedProviderId = provider.id
                                        selectedProviderName = provider.name
                                        showProviderDialog = false
                                        
                                        context.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
                                            .edit().putString("selected_provider", provider.id).apply()
                                            
                                        if (oldId != provider.id) {
                                            onPluginChanged()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    androidx.compose.material3.Text(provider.name)
                                    val badgeText = when(provider.id) {
                                        "uhdmovies" -> "MultiAudio Movies & Shows in 4K"
                                        "moviebox", "toonstream", "animedekho", "all_otts" -> "Recommended"
                                        "hdhub4u", "fourkhdhub" -> "Backup"
                                        "youtube" -> "100% Working"
                                        else -> null
                                    }
                                    if (badgeText != null) {
                                        androidx.compose.material3.Text(
                                            badgeText,
                                            fontSize = 11.sp,
                                            color = if (badgeText == "Recommended") Color(0xFF00C853) else if (badgeText.contains("4K", ignoreCase = true)) Color(0xFFFFB300) else if (badgeText == "100% Working") Color(0xFF29B6F6) else Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (provider.id == "moviebox") {
                                    androidx.compose.material3.IconButton(onClick = {
                                        showProviderDialog = false
                                        showMovieBoxSettings = true
                                    }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showProviderDialog = false }) {
                        androidx.compose.material3.Text("Close")
                    }
                }
            )
        }

        if (showMovieBoxSettings) {
            com.cinetheta.app.ui.home.MovieBoxSettingsDialog(
                onDismiss = { showMovieBoxSettings = false },
                context = context
            )
        }

        if (showSupportDialog) {
            com.cinetheta.app.utils.FullSupportCineThetaDialog(
                onDismiss = { showSupportDialog = false },
                context = context,
                onWatchAdSuccess = {
                    showSupportDialog = false
                }
            )
        }

        // Listens for return from ad: shows big ThankYouSupportDialog if >= 20s, or Toast if < 20s
        com.cinetheta.app.utils.AdReturnLifecycleTracker(
            context = context,
            onShowThankYouDialog = { showThankYouDialog = true }
        )

        if (showThankYouDialog) {
            com.cinetheta.app.utils.ThankYouSupportDialog(
                onDismiss = { showThankYouDialog = false },
                onOpenAdAgain = {
                    showThankYouDialog = false
                    com.cinetheta.app.utils.SupportManager.openAd(context)
                }
            )
        }

        if (showIssueReportDialog) {
            IssueReportDialog(
                selectedProviderName = selectedProviderName,
                dohProviderName = try {
                    com.cinetheta.core.network.DohProvider.valueOf(dohProvider).title
                } catch (_: Exception) { "Google" },
                onDismissRequest = { showIssueReportDialog = false }
            )
        }

        if (showFactoryResetDialog) {
            AlertDialog(
                onDismissRequest = { showFactoryResetDialog = false },
                icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Factory Reset", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This will permanently wipe all your settings, preferences, and cached data. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showFactoryResetDialog = false
                            val ok = viewModel.factoryReset()
                            val msg = if (ok) "Factory reset complete." else "Reset failed. Please try again."
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFactoryResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

fun getThemeDisplayName(themeId: String): String {
    return when(themeId) {
        "SYSTEM" -> "System Default"
        "SKY_DARK" -> "Default Dark Mode"
        "SKY_LIGHT" -> "Default Light Mode"
        "NETFLIX" -> "Netflix Mode (Dark)"
        "PRIME" -> "Prime Mode (Dark)"
        "CINETHETA" -> "Cinematic Mode (Dark OLED)"
        else -> "System Default"
    }
}

@Composable
fun ThemeOptionRow(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
    }
}

@Composable
fun DecoderOptionRow(
    mode: com.cinetheta.player.core.DecoderMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = mode.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            content()
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
    )
}

@Composable
fun SettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    isLast: Boolean = false,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container with 10% opacity primary color
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }
        
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun appSwitchColors(): SwitchColors {
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = MaterialTheme.colorScheme.outline,
        uncheckedBorderColor = Color.Transparent
    )
}

@Composable
fun SubtitleColorPickerDialog(
    title: String,
    initialHex: String,
    presets: List<Pair<String, String>>,
    isBackgroundPicker: Boolean,
    currentTextColorHex: String,
    currentBgColorHex: String,
    onDismiss: () -> Unit,
    onSelectColor: (String) -> Unit
) {
    var selectedHex by remember { mutableStateOf(initialHex) }
    var customHexInput by remember { mutableStateOf(initialHex) }

    val effectiveTextColor = if (isBackgroundPicker) {
        parseColorHex(currentTextColorHex, Color.White)
    } else {
        parseColorHex(selectedHex, Color.White)
    }

    val effectiveBgColor = if (isBackgroundPicker) {
        parseColorHex(selectedHex, Color(0x80000000))
    } else {
        parseColorHex(currentBgColorHex, Color(0x80000000))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive Preview Card
                Column {
                    Text(
                        text = "Preview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sample Subtitle Text",
                            color = effectiveTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(effectiveBgColor)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Preset options list
                Text(
                    text = "Presets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.forEach { (label, hex) ->
                        val isSelected = selectedHex.equals(hex, ignoreCase = true)
                        val chipColor = parseColorHex(hex, Color.White)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    selectedHex = hex
                                    customHexInput = hex
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(chipColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Hex Input Field
                OutlinedTextField(
                    value = customHexInput,
                    onValueChange = { input ->
                        val formatted = if (input.startsWith("#")) input else "#$input"
                        customHexInput = formatted
                        if (formatted.matches(Regex("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"))) {
                            selectedHex = formatted
                        }
                    },
                    label = { Text("Custom Hex Color (#RRGGBB / #AARRGGBB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectColor(selectedHex) }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun parseColorHex(hex: String, defaultColor: Color): Color {
    return try {
        val cleanHex = hex.trim().removePrefix("#")
        when (cleanHex.length) {
            6 -> {
                val colorInt = cleanHex.toLong(16)
                Color(0xFF000000 or colorInt)
            }
            8 -> {
                val colorInt = cleanHex.toLong(16)
                Color(colorInt)
            }
            3 -> {
                val r = cleanHex.substring(0, 1).repeat(2)
                val g = cleanHex.substring(1, 2).repeat(2)
                val b = cleanHex.substring(2, 3).repeat(2)
                val colorInt = "$r$g$b".toLong(16)
                Color(0xFF000000 or colorInt)
            }
            else -> defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}

fun getSubtitleColorName(hex: String): String {
    return when (hex.uppercase()) {
        "#FFFFFF", "#FFFFFFFF" -> "White (#FFFFFF)"
        "#FFEE58", "#FFFFEE58" -> "Yellow (#FFEE58)"
        "#FFFF00", "#FFFFFF00" -> "Bright Yellow (#FFFF00)"
        "#66BB6A", "#FF66BB6A" -> "Green (#66BB6A)"
        "#26C6DA", "#FF26C6DA" -> "Cyan (#26C6DA)"
        "#42A5F5", "#FF42A5F5" -> "Light Blue (#42A5F5)"
        "#EF5350", "#FFEF5350" -> "Red (#EF5350)"
        "#FFA726", "#FFFFA726" -> "Orange (#FFA726)"
        "#FFCA28", "#FFFFCA28" -> "Amber (#FFCA28)"
        "#EC407A", "#FFEC407A" -> "Pink (#EC407A)"
        "#D4E157", "#FFD4E157" -> "Lime (#D4E157)"
        else -> hex.uppercase()
    }
}

fun getSubtitleBgColorName(hex: String): String {
    return when (hex.uppercase()) {
        "#80000000" -> "Translucent Black (50%)"
        "#00000000" -> "Transparent (None)"
        "#FF000000" -> "Solid Black"
        "#C0000000" -> "Dark Charcoal (75%)"
        "#40000000" -> "Light Charcoal (25%)"
        "#800D1B2A" -> "Deep Navy (50%)"
        "#80212121" -> "Dark Gray"
        "#FF212121" -> "Solid Dark Gray"
        else -> hex.uppercase()
    }
}


