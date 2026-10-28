package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppLanguage
import com.example.model.CardBackTheme
import com.example.model.DealType
import com.example.model.DrawMode
import com.example.model.GameSettings
import com.example.model.GameStatistics
import com.example.model.LanguageDictionary
import com.example.model.TableTheme

@Composable
fun SettingsDialog(
    settings: GameSettings,
    onSettingsChanged: (GameSettings) -> Unit,
    onPreviewCelebration: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val strings = LanguageDictionary.getStrings(settings.language)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E293B),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.settingsTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Idioma (Multi-idiomas)
                SectionHeader(strings.languageSection)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Two rows of 3 language chips
                    val chunked = AppLanguage.values().toList().chunked(3)
                    chunked.forEach { rowLangs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowLangs.forEach { lang ->
                                val isSelected = settings.language == lang
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF0284C7) else Color(0x15FFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else Color(0x28FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSettingsChanged(settings.copy(language = lang)) }
                                        .testTag("lang_chip_${lang.code}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = lang.flagEmoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = lang.nativeName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Modo de robo (Draw 1 vs Draw 3)
                SectionHeader(strings.drawModeSection)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DrawMode.values().forEach { mode ->
                        val isSelected = settings.drawMode == mode
                        SelectableCard(
                            title = if (mode == DrawMode.DRAW_ONE) strings.draw1Title else strings.draw3Title,
                            subtitle = if (mode == DrawMode.DRAW_ONE) strings.draw1Subtitle else strings.draw3Subtitle,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = { onSettingsChanged(settings.copy(drawMode = mode)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tipo de reparto
                SectionHeader(strings.dealTypeSection)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DealType.values().forEach { type ->
                        val isSelected = settings.dealType == type
                        SelectableCard(
                            title = if (type == DealType.WINNABLE) strings.winnableTitle else strings.randomTitle,
                            subtitle = if (type == DealType.WINNABLE) strings.winnableSubtitle else strings.randomSubtitle,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = { onSettingsChanged(settings.copy(dealType = type)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tapete de juego (Felt Themes)
                SectionHeader(strings.tableThemeSection)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TableTheme.values().forEach { theme ->
                        val isSelected = settings.tableTheme == theme
                        ThemeRowItem(
                            name = theme.displayName,
                            color = theme.primaryFeltColor,
                            isSelected = isSelected,
                            onClick = { onSettingsChanged(settings.copy(tableTheme = theme)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Reverso de Cartas (Card Backs)
                SectionHeader(strings.cardBackSection)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardBackTheme.values().forEach { theme ->
                        val isSelected = settings.cardBackTheme == theme
                        ThemeRowItem(
                            name = theme.displayName,
                            color = theme.primaryColor,
                            accentColor = theme.accentColor,
                            isSelected = isSelected,
                            onClick = { onSettingsChanged(settings.copy(cardBackTheme = theme)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Toggles
                SectionHeader(strings.preferencesSection)

                ToggleRow(
                    title = strings.darkModeToggle,
                    subtitle = strings.darkModeSubtitle,
                    checked = settings.isDarkMode,
                    onCheckedChange = { onSettingsChanged(settings.copy(isDarkMode = it)) }
                )

                ToggleRow(
                    title = strings.hapticToggle,
                    subtitle = strings.hapticSubtitle,
                    checked = settings.hapticFeedbackEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(hapticFeedbackEnabled = it)) }
                )

                ToggleRow(
                    title = strings.autoFinishToggle,
                    subtitle = strings.autoFinishSubtitle,
                    checked = settings.autoPlayToFoundation,
                    onCheckedChange = { onSettingsChanged(settings.copy(autoPlayToFoundation = it)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onPreviewCelebration,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("preview_celebration_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.previewCelebrationButton,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(strings.saveAndCloseButton, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatsDialog(
    stats: GameStatistics,
    language: AppLanguage,
    onResetStats: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LanguageDictionary.getStrings(language)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("stats_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E293B),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.statsTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatRow(label = strings.statsPlayed, value = "${stats.gamesPlayed}")
                    StatRow(label = strings.statsWon, value = "${stats.gamesWon}")
                    StatRow(label = strings.statsWinRate, value = "%.1f%%".format(stats.winRate))
                    StatRow(label = strings.statsBestTime, value = if (stats.bestTimeSeconds > 0) formatTime(stats.bestTimeSeconds) else "--:--")
                    StatRow(label = strings.statsBestScore, value = "${stats.bestScore}")
                    StatRow(label = strings.statsFewestMoves, value = if (stats.fewestMoves > 0) "${stats.fewestMoves}" else "--")
                    StatRow(label = strings.statsCurrentStreak, value = "${stats.currentStreak}")
                    StatRow(label = strings.statsBestStreak, value = "${stats.bestStreak}")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetStats,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.statsResetButton, color = Color(0xFFEF4444))
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.statsCloseButton, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SelectableCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF3B82F6) else Color(0x33FFFFFF)
    val bgColor = if (isSelected) Color(0x223B82F6) else Color(0x11FFFFFF)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ThemeRowItem(
    name: String,
    color: Color,
    accentColor: Color? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0x223B82F6) else Color(0x0EFFFFFF))
            .border(
                1.dp,
                if (isSelected) Color(0xFF3B82F6) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, accentColor ?: Color(0x40FFFFFF), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0)
            )
        }

        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3B82F6),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
