package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.ui.platform.LocalContext
import com.example.ads.AdBannerView
import com.example.ads.AdMobManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CardPileLocation
import com.example.model.DrawMode
import com.example.model.LanguageDictionary
import com.example.model.LocalizedStrings
import com.example.ui.components.FoundationSlot
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StatsDialog
import com.example.ui.components.StockWasteRow
import com.example.ui.components.TableauColumnView
import com.example.ui.components.WinCelebrationOverlay
import com.example.viewmodel.SolitaireViewModel
import kotlinx.coroutines.delay

@Composable
fun SolitaireScreen(
    viewModel: SolitaireViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = remember(state.settings.language) { LanguageDictionary.getStrings(state.settings.language) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as? Activity
    val adTriggerCounter by viewModel.adTriggerCounter.collectAsState()

    // Automatic AdMob Interstitial triggers on victory or game restart
    LaunchedEffect(adTriggerCounter) {
        if (adTriggerCounter > 0 && activity != null) {
            AdMobManager.showInterstitial(activity)
        }
    }

    // Periodic automatic interstitial ad check (every 3 minutes of gameplay)
    LaunchedEffect(activity) {
        while (true) {
            delay(180_000L)
            if (activity != null && state.isGameActive && !state.isWon && !state.isAutoCompleting) {
                AdMobManager.showInterstitial(activity)
            }
        }
    }

    // Trigger haptic if enabled
    val triggerHaptic: () -> Unit = {
        if (state.settings.hapticFeedbackEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Dismiss toast message automatically
    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            delay(3500)
            viewModel.dismissToast()
        }
    }

    val feltGradient = Brush.radialGradient(
        colors = listOf(
            state.settings.tableTheme.primaryFeltColor,
            state.settings.tableTheme.secondaryFeltColor
        ),
        radius = 1200f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(feltGradient)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD Bar (Score, Moves, Timer, Settings & Stats)
            TopHudBar(
                moves = state.moves,
                score = state.score,
                timeSeconds = state.timeSeconds,
                drawMode = state.settings.drawMode,
                strings = strings,
                onNewGameClick = { viewModel.setShowNewGameDialog(true) },
                onStatsClick = { viewModel.setShowStatsDialog(true) },
                onSettingsClick = { viewModel.setShowSettingsDialog(true) }
            )

            // Playing Field with dynamic Card Sizing
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight

                // Calculate optimal card dimensions for 7 columns
                val colGap = 5.dp
                val horizontalMargin = 2.dp
                val rawCardWidth = ((availableWidth - (horizontalMargin * 2) - (colGap * 6)) / 7f)
                val cardWidth = rawCardWidth.coerceIn(36.dp, 80.dp)
                val cardHeight = cardWidth * 1.38f

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Row: Stock & Waste on left, 4 Foundations on right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stock & Waste
                        StockWasteRow(
                            stockCards = state.stock,
                            wasteCards = state.waste,
                            width = cardWidth,
                            height = cardHeight,
                            tableTheme = state.settings.tableTheme,
                            cardBackTheme = state.settings.cardBackTheme,
                            hintInfo = state.activeHint,
                            onStockClick = {
                                triggerHaptic()
                                viewModel.onStockClick()
                            },
                            onWasteCardClick = { card ->
                                triggerHaptic()
                                viewModel.onWasteCardClick(card)
                            }
                        )

                        // 4 Foundation Piles
                        Row(horizontalArrangement = Arrangement.spacedBy(colGap)) {
                            for (fIndex in 0 until 4) {
                                FoundationSlot(
                                    suitIndex = fIndex,
                                    cards = state.foundations[fIndex],
                                    width = cardWidth,
                                    height = cardHeight,
                                    tableTheme = state.settings.tableTheme,
                                    cardBackTheme = state.settings.cardBackTheme,
                                    hintInfo = state.activeHint,
                                    onCardClick = { card ->
                                        triggerHaptic()
                                        viewModel.onFoundationCardClick(card)
                                    }
                                )
                            }
                        }
                    }

                    // 7 Tableau Columns
                    val tableauMaxHeight = availableHeight - cardHeight - 20.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (colIndex in 0 until 7) {
                            TableauColumnView(
                                columnIndex = colIndex,
                                cards = state.tableau[colIndex],
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                maxHeight = tableauMaxHeight,
                                tableTheme = state.settings.tableTheme,
                                cardBackTheme = state.settings.cardBackTheme,
                                hintInfo = state.activeHint,
                                onCardClick = { cardIndex, card ->
                                    triggerHaptic()
                                    viewModel.onTableauCardClick(colIndex, cardIndex, card)
                                },
                                onEmptyColumnClick = {
                                    // If a hint points to this empty column, trigger hint!
                                    if (state.activeHint?.to is CardPileLocation.Tableau &&
                                        (state.activeHint?.to as CardPileLocation.Tableau).colIndex == colIndex
                                    ) {
                                        viewModel.requestHint()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Floating Auto-Move Ready Banner
            AnimatedVisibility(
                visible = (state.isAutoCompleteAvailable || state.isAutoCompleting) && !state.isWon,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Surface(
                    color = Color(0xF0064E3B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF34D399)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("auto_move_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = strings.autoMoveBannerTitle,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (state.isAutoCompleting) strings.autoMoveInProgress else strings.autoMoveBannerDesc,
                                    color = Color(0xFFD1FAE5),
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                triggerHaptic()
                                viewModel.toggleAutoComplete()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isAutoCompleting) Color(0xFFEF4444) else Color(0xFF10B981)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("auto_move_banner_action")
                        ) {
                            Text(
                                text = if (state.isAutoCompleting) "Stop" else strings.autoMoveButton,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar
            BottomActionBar(
                undoCount = state.undoCount,
                redoCount = state.redoCount,
                hasActiveHint = state.activeHint != null,
                isAutoCompleteAvailable = state.isAutoCompleteAvailable,
                isAutoCompleting = state.isAutoCompleting,
                strings = strings,
                onUndoClick = {
                    triggerHaptic()
                    viewModel.undo()
                },
                onRedoClick = {
                    triggerHaptic()
                    viewModel.redo()
                },
                onHintClick = {
                    triggerHaptic()
                    viewModel.requestHint()
                },
                onAutoCompleteClick = {
                    triggerHaptic()
                    viewModel.toggleAutoComplete()
                }
            )

            // Google AdMob Bottom Banner
            AdBannerView(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Hint Toast Notification
        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
                .navigationBarsPadding()
        ) {
            Surface(
                color = Color(0xF00F172A),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clickable { viewModel.requestHint() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = state.toastMessage.orEmpty(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Win Celebration
        WinCelebrationOverlay(
            isWon = state.isWon,
            timeSeconds = state.timeSeconds,
            moves = state.moves,
            score = state.score,
            language = state.settings.language,
            onPlayAgainClick = { viewModel.startNewGame() }
        )

        // Settings Dialog
        if (state.showSettingsDialog) {
            SettingsDialog(
                settings = state.settings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onPreviewCelebration = { viewModel.triggerVictoryCelebration() },
                onDismiss = { viewModel.setShowSettingsDialog(false) }
            )
        }

        // Stats Dialog
        if (state.showStatsDialog) {
            StatsDialog(
                stats = state.statistics,
                language = state.settings.language,
                onResetStats = { viewModel.resetStatistics() },
                onDismiss = { viewModel.setShowStatsDialog(false) }
            )
        }

        // New Game Confirmation Dialog
        if (state.showNewGameDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowNewGameDialog(false) },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFF1E293B),
                title = {
                    Text(
                        text = strings.newGameTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = strings.newGameConfirmMessage,
                        color = Color(0xFFCBD5E1)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.startNewGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text(strings.newDealButton, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { viewModel.restartCurrentGame() }
                    ) {
                        Text(strings.restartCurrentButton, color = Color(0xFF38BDF8))
                    }
                }
            )
        }
    }
}

@Composable
private fun TopHudBar(
    moves: Int,
    score: Int,
    timeSeconds: Long,
    drawMode: DrawMode,
    strings: LocalizedStrings,
    onNewGameClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = Color(0x33000000),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stats pills
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HudBadge(label = strings.timeLabel, value = formatTime(timeSeconds))
                HudBadge(label = strings.movesLabel, value = "$moves")
                HudBadge(label = strings.scoreLabel, value = "$score")
            }

            // Quick Actions (Stats, Settings, New Game)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStatsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = strings.statsTitle,
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onNewGameClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("new_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = strings.newDealButton,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HudBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun BottomActionBar(
    undoCount: Int,
    redoCount: Int,
    hasActiveHint: Boolean,
    isAutoCompleteAvailable: Boolean,
    isAutoCompleting: Boolean,
    strings: LocalizedStrings,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onHintClick: () -> Unit,
    onAutoCompleteClick: () -> Unit
) {
    Surface(
        color = Color(0x40000000),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo & Redo button group
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Undo button
                Button(
                    onClick = onUndoClick,
                    enabled = undoCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33FFFFFF),
                        disabledContainerColor = Color(0x11FFFFFF)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = strings.undoButton,
                        tint = if (undoCount > 0) Color.White else Color(0x55FFFFFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.undoButton,
                        color = if (undoCount > 0) Color.White else Color(0x55FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Redo button
                Button(
                    onClick = onRedoClick,
                    enabled = redoCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33FFFFFF),
                        disabledContainerColor = Color(0x11FFFFFF)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = strings.redoButton,
                        tint = if (redoCount > 0) Color.White else Color(0x55FFFFFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.redoButton,
                        color = if (redoCount > 0) Color.White else Color(0x55FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Right group: Auto-Move & Hint
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto-Move button (animated in when ready or running)
                AnimatedVisibility(visible = isAutoCompleteAvailable || isAutoCompleting) {
                    Button(
                        onClick = onAutoCompleteClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAutoCompleting) Color(0xFFEF4444) else Color(0xFF10B981)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("auto_move_button")
                            .testTag("auto_complete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = strings.autoMoveButton,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAutoCompleting) "Stop" else strings.autoMoveButton,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Hint button
                Button(
                    onClick = onHintClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasActiveHint) Color(0xFFF59E0B) else Color(0x33FFFFFF)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("hint_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = if (hasActiveHint) strings.applyHintButton else strings.hintButton,
                        tint = if (hasActiveHint) Color(0xFF0F172A) else Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasActiveHint) strings.applyHintButton else strings.hintButton,
                        color = if (hasActiveHint) Color(0xFF0F172A) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
