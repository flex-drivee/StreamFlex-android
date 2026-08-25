package com.streamflex.app.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val appTheme by viewModel.appTheme.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    val enableSubtitles by viewModel.enableSubtitles.collectAsState()
    val cellularData by viewModel.cellularData.collectAsState()
    val developerMode by viewModel.developerMode.collectAsState()
    val decoderMode by viewModel.decoderMode.collectAsState()
    val wifiOnlyDownloads by viewModel.wifiOnlyDownloads.collectAsState()
    val downloadQuality by viewModel.downloadQuality.collectAsState()
    val smartDownloads by viewModel.smartDownloads.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()

    val scrollState = rememberLazyListState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showMovieBoxSettings by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    
    val providerRepository = com.streamflex.app.di.ProviderModule.repository
    var selectedProviderName by remember { 
        mutableStateOf(providerRepository.provider(providerRepository.selectedProviderId ?: "")?.name ?: "All in One") 
    }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                        onTap = { /* TODO */ }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.SystemUpdate,
                        title = "Updates",
                        subtitle = "Check for app updates",
                        isLast = true,
                        onTap = { /* TODO */ }
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
                        subtitle = "Auto",
                        onTap = { /* TODO */ }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Subtitles,
                        title = "Enable Subtitles by Default",
                        subtitle = if (enableSubtitles) "Enabled" else "Disabled",
                        trailing = { Switch(
                            checked = enableSubtitles,
                            onCheckedChange = { viewModel.setEnableSubtitles(it) },
                            colors = appSwitchColors()
                        ) },
                        onTap = { viewModel.setEnableSubtitles(!enableSubtitles) }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Memory,
                        title = "Decoder Mode",
                        subtitle = com.streamflex.player.core.DecoderMode.fromKey(decoderMode).title,
                        isLast = true,
                        onTap = { showDecoderDialog = true }
                    )
                }
            }

            // --- Downloads ---
            item {
                SettingsGroup("Downloads") {
                    SettingsTile(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Smart Downloads",
                        subtitle = if (smartDownloads) "Auto-download next episode" else "Disabled",
                        trailing = { Switch(
                            checked = smartDownloads,
                            onCheckedChange = { viewModel.setSmartDownloads(it) },
                            colors = appSwitchColors()
                        ) },
                        onTap = { viewModel.setSmartDownloads(!smartDownloads) }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Wifi,
                        title = "Download on Wi-Fi Only",
                        subtitle = if (wifiOnlyDownloads) "Save mobile data" else "Download on any network",
                        trailing = { Switch(
                            checked = wifiOnlyDownloads,
                            onCheckedChange = { viewModel.setWifiOnlyDownloads(it) },
                            colors = appSwitchColors()
                        ) },
                        onTap = { viewModel.setWifiOnlyDownloads(!wifiOnlyDownloads) }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.HighQuality,
                        title = "Video Quality",
                        subtitle = "Current: $downloadQuality",
                        onTap = { showQualityDialog = true }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.SdStorage,
                        title = "Storage Usage",
                        subtitle = "StreamFlex: ${storageStats.formattedStreamFlexUsed} • Free: ${storageStats.formattedFree}",
                        onTap = { viewModel.refreshStorageStats() }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.DeleteSweep,
                        title = "Delete All Downloads",
                        subtitle = "Remove all offline movies and episodes",
                        isLast = true,
                        onTap = { showDeleteAllDialog = true }
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
                        onTap = { /* TODO */ }
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
                        title = "Custom DNS",
                        subtitle = "Bypass ISP blocking (Cloudflare/Google)",
                        isLast = true,
                        onTap = { /* TODO */ }
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
                        isLast = true,
                        onTap = { showProviderDialog = true }
                    )
                }
            }

            // --- App Data ---
            item {
                SettingsGroup("App Data") {
                    SettingsTile(
                        icon = Icons.Outlined.CleaningServices,
                        title = "Clear Cache",
                        subtitle = "Free up space on your device",
                        onTap = { /* TODO */ }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.DeleteForever,
                        title = "Factory Reset",
                        subtitle = "Wipe all app data and settings",
                        isLast = true,
                        onTap = { /* TODO */ }
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
                            onTap = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsTile(
                            icon = Icons.Outlined.Speed,
                            title = "Network Inspector",
                            subtitle = "Monitor API requests and responses",
                            isLast = true,
                            onTap = { /* TODO */ }
                        )
                    }
                }
            }

            // --- About ---
            item {
                SettingsGroup("About") {
                    SettingsTile(
                        icon = Icons.Outlined.PersonOutline,
                        title = "Developer",
                        subtitle = "Developed by Mani-Balouch.",
                        onTap = { /* TODO */ }
                    )
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Outlined.Info,
                        title = "Version",
                        subtitle = "StreamFlex v1.0.0",
                        trailing = { Spacer(modifier = Modifier.width(0.dp)) }, // No chevron
                        isLast = true,
                        onTap = { /* TODO */ }
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
                            title = "Sky Stream Dark",
                            isSelected = appTheme == "SKY_DARK",
                            onClick = { viewModel.setAppTheme("SKY_DARK"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "Sky Stream Light",
                            isSelected = appTheme == "SKY_LIGHT",
                            onClick = { viewModel.setAppTheme("SKY_LIGHT"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "Netflix Mode",
                            isSelected = appTheme == "NETFLIX",
                            onClick = { viewModel.setAppTheme("NETFLIX"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "Amazon Prime Mode",
                            isSelected = appTheme == "PRIME",
                            onClick = { viewModel.setAppTheme("PRIME"); showThemeDialog = false }
                        )
                        ThemeOptionRow(
                            title = "StreamFlex Premium",
                            isSelected = appTheme == "STREAMFLEX",
                            onClick = { viewModel.setAppTheme("STREAMFLEX"); showThemeDialog = false }
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
                        com.streamflex.player.core.DecoderMode.entries.forEach { mode ->
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

        if (showQualityDialog) {
            val qualities = listOf("1080p", "720p", "480p")
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                title = { Text("Download Video Quality", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        qualities.forEach { q ->
                            ThemeOptionRow(
                                title = when (q) {
                                    "1080p" -> "High (1080p) • Best Quality"
                                    "720p" -> "Standard (720p) • Balanced"
                                    else -> "Data Saver (480p) • Small File"
                                },
                                isSelected = downloadQuality == q,
                                onClick = {
                                    viewModel.setDownloadQuality(q)
                                    showQualityDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Delete All Downloads?", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.surface,
                text = {
                    Text(
                        "This will permanently delete all downloaded movies, episodes, and offline files from your device.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAllDownloads()
                            showDeleteAllDialog = false
                        }
                    ) {
                        Text("Delete All", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                                    providerRepository.selectedProviderId = null
                                    selectedProviderName = "All in One"
                                    showProviderDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Text("All in One", modifier = Modifier.weight(1f))
                        }
                        
                        providerRepository.enabledProviders().forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        providerRepository.selectedProviderId = provider.id
                                        selectedProviderName = provider.name
                                        showProviderDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Text(provider.name, modifier = Modifier.weight(1f))
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
            com.streamflex.app.ui.home.MovieBoxSettingsDialog(
                onDismiss = { showMovieBoxSettings = false },
                context = context
            )
        }
    }
}

fun getThemeDisplayName(themeId: String): String {
    return when(themeId) {
        "SKY_DARK" -> "Sky Stream Dark"
        "SKY_LIGHT" -> "Sky Stream Light"
        "NETFLIX" -> "Netflix Mode"
        "PRIME" -> "Amazon Prime Mode"
        "STREAMFLEX" -> "StreamFlex Premium"
        else -> "Sky Stream Dark"
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
    mode: com.streamflex.player.core.DecoderMode,
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
