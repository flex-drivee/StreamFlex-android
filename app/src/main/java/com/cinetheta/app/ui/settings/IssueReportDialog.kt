package com.cinetheta.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinetheta.app.BuildConfig
import java.net.URLEncoder

enum class IssueCategory(val label: String, val icon: String, val githubLabel: String) {
    BUG("Bug / Glitch", "🐛", "bug"),
    STREAM_ERROR("Playback / Stream Error", "🎬", "playback-error"),
    PROVIDER_DOWN("Provider Down", "🌐", "provider-issue"),
    DOWNLOAD("Download Problem", "📥", "download-issue"),
    FEATURE("Feature Request", "💡", "enhancement"),
    FEEDBACK("General Feedback", "💬", "feedback")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueReportDialog(
    selectedProviderName: String,
    dohProviderName: String,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(IssueCategory.BUG) }
    var issueTitle by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var userContact by remember { mutableStateOf("") }
    var attachDiagnostics by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()
    val categoryScrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16171E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header with Close 'X' button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF53B66).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BugReport,
                                contentDescription = null,
                                tint = Color(0xFFF53B66),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Report an Issue / Feedback",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tracked directly on GitHub by admin",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category Selection
                    Text(
                        text = "Category",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(categoryScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IssueCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text("${category.icon} ${category.label}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF22242F),
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }

                    // Summary / Title
                    OutlinedTextField(
                        value = issueTitle,
                        onValueChange = { issueTitle = it },
                        label = { Text("Summary / Title", fontSize = 13.sp) },
                        placeholder = { Text("e.g., HDHub4u search fails or video buffers", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF333544),
                            focusedContainerColor = Color(0xFF1E202B),
                            unfocusedContainerColor = Color(0xFF1E202B)
                        )
                    )

                    // Description & Steps
                    OutlinedTextField(
                        value = issueDescription,
                        onValueChange = { issueDescription = it },
                        label = { Text("Description & Steps to Reproduce", fontSize = 13.sp) },
                        placeholder = { Text("Describe what happened, movie/anime title, or steps...", fontSize = 13.sp) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF333544),
                            focusedContainerColor = Color(0xFF1E202B),
                            unfocusedContainerColor = Color(0xFF1E202B)
                        )
                    )

                    // Contact info (Optional)
                    OutlinedTextField(
                        value = userContact,
                        onValueChange = { userContact = it },
                        label = { Text("Your Email or GitHub Username (Optional)", fontSize = 13.sp) },
                        placeholder = { Text("So admin can reply back to you", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF333544),
                            focusedContainerColor = Color(0xFF1E202B),
                            unfocusedContainerColor = Color(0xFF1E202B)
                        )
                    )

                    // Diagnostics Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E202B))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = attachDiagnostics,
                            onCheckedChange = { attachDiagnostics = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Attach Device Diagnostics",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} • Android ${Build.VERSION.RELEASE} • ${Build.MANUFACTURER} ${Build.MODEL}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons (Stacked cleanly without ANY collisions)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Primary Action: Submit to GitHub
                    Button(
                        onClick = {
                            if (issueTitle.isBlank()) {
                                Toast.makeText(context, "Please enter an issue title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val formattedBody = buildIssueMarkdown(
                                category = selectedCategory,
                                description = issueDescription,
                                contact = userContact,
                                attachDiagnostics = attachDiagnostics,
                                providerName = selectedProviderName,
                                dohName = dohProviderName
                            )
                            openGitHubNewIssue(
                                context = context,
                                category = selectedCategory,
                                title = issueTitle,
                                body = formattedBody
                            )
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit to GitHub Issues", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }

                    // Secondary Action Row: View Admin Issues & Copy Report
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cinetheta/cinetheta.github.io/issues"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Outlined.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Admin Issues", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        TextButton(
                            onClick = {
                                val formattedBody = buildIssueMarkdown(
                                    category = selectedCategory,
                                    description = issueDescription,
                                    contact = userContact,
                                    attachDiagnostics = attachDiagnostics,
                                    providerName = selectedProviderName,
                                    dohName = dohProviderName
                                )
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("CineTheta Issue Report", formattedBody))
                                Toast.makeText(context, "Issue details copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Report", fontSize = 12.sp, color = Color.LightGray)
                        }
                    }

                    // Cancel / Close
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        Text("Cancel", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun buildIssueMarkdown(
    category: IssueCategory,
    description: String,
    contact: String,
    attachDiagnostics: Boolean,
    providerName: String,
    dohName: String
): String {
    val sb = StringBuilder()
    sb.append("### Issue Category\n")
    sb.append("${category.icon} ${category.label}\n\n")

    sb.append("### Description\n")
    if (description.isNotBlank()) {
        sb.append(description.trim()).append("\n\n")
    } else {
        sb.append("*(No detailed description provided)*\n\n")
    }

    if (contact.isNotBlank()) {
        sb.append("### Contact / Reporter\n")
        sb.append(contact.trim()).append("\n\n")
    }

    if (attachDiagnostics) {
        sb.append("### Diagnostics\n")
        sb.append("- **App Version:** CineTheta v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\n")
        sb.append("- **Device:** ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}\n")
        sb.append("- **Android OS:** Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        sb.append("- **Active Provider:** $providerName\n")
        sb.append("- **Active DoH:** $dohName\n")
    }

    return sb.toString()
}

private fun openGitHubNewIssue(
    context: Context,
    category: IssueCategory,
    title: String,
    body: String
) {
    val fullTitle = "[${category.label.substringBefore(" ")}] $title"
    val encodedTitle = URLEncoder.encode(fullTitle, "UTF-8")
    val encodedBody = URLEncoder.encode(body, "UTF-8")
    val encodedLabel = URLEncoder.encode(category.githubLabel, "UTF-8")

    val url = "https://github.com/cinetheta/cinetheta.github.io/issues/new?title=$encodedTitle&body=$encodedBody&labels=$encodedLabel"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No web browser found to open GitHub", Toast.LENGTH_SHORT).show()
    }
}
