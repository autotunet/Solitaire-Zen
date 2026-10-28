package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.LanguageDictionary
import com.example.model.Rank
import com.example.model.Suit
import kotlin.math.sin
import kotlin.random.Random

enum class CelebrationEffectMode(val title: String) {
    BOTH("Cascada y Confeti"),
    CASCADE("Solo Cascada"),
    CONFETTI("Solo Confeti")
}

private data class TrailPoint(val x: Float, val y: Float)

private class CascadeCardState(
    val suit: Suit,
    val rank: Rank,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var isLaunched: Boolean = false,
    var isFinished: Boolean = false,
    val trail: MutableList<TrailPoint> = mutableListOf()
)

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var vRot: Float,
    var size: Float,
    var wobble: Float,
    val shapeType: Int, // 0: rect, 1: circle, 2: star/ribbon
    val color: Color
)

@Composable
fun WinCelebrationOverlay(
    isWon: Boolean,
    timeSeconds: Long,
    moves: Int,
    score: Int,
    language: AppLanguage = AppLanguage.ES,
    onPlayAgainClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isWon) return

    val strings = remember(language) { LanguageDictionary.getStrings(language) }
    val density = LocalDensity.current
    var celebrationMode by remember { mutableStateOf(CelebrationEffectMode.BOTH) }
    var isDialogVisible by remember { mutableStateOf(true) }
    var animationKey by remember { mutableStateOf(0) }

    // Cards list for the classic cascade: 52 cards (Kings down to Aces, 4 suits)
    val cascadeCards = remember(animationKey) {
        val list = mutableListOf<CascadeCardState>()
        val allRanks = Rank.values().reversed() // King down to Ace
        val allSuits = listOf(Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS)
        for (rank in allRanks) {
            for (suit in allSuits) {
                list.add(
                    CascadeCardState(
                        suit = suit,
                        rank = rank,
                        x = 0f,
                        y = 0f,
                        vx = 0f,
                        vy = 0f
                    )
                )
            }
        }
        list
    }

    // Confetti particles list
    val confettiParticles = remember(animationKey) {
        val particles = mutableListOf<ConfettiParticle>()
        val colors = listOf(
            Color(0xFFEF4444), // Crimson
            Color(0xFFF59E0B), // Amber Gold
            Color(0xFF10B981), // Emerald
            Color(0xFF3B82F6), // Royal Blue
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFFFBBF24), // Bright Yellow
            Color(0xFF38BDF8), // Sky Blue
            Color(0xFFFFFFFF)  // Silver Shimmer
        )
        val rand = Random(99)
        repeat(120) {
            particles.add(
                ConfettiParticle(
                    x = rand.nextFloat() * 1200f,
                    y = -rand.nextFloat() * 800f,
                    vx = (rand.nextFloat() - 0.5f) * 4.5f,
                    vy = 3.5f + rand.nextFloat() * 6.5f,
                    rotation = rand.nextFloat() * 360f,
                    vRot = (rand.nextFloat() - 0.5f) * 10f,
                    size = 8f + rand.nextFloat() * 14f,
                    wobble = rand.nextFloat() * 6.28f,
                    shapeType = rand.nextInt(3),
                    color = colors[rand.nextInt(colors.size)]
                )
            )
        }
        particles
    }

    // Physics Loop for Cascade & Confetti
    LaunchedEffect(isWon, animationKey) {
        var currentLaunchingIndex = 0
        var launchTimer = 0
        val launchInterval = 9 // Launch new card every ~9 frames (~150ms)
        val rand = Random(animationKey + 42)

        while (true) {
            withFrameNanos { _ ->
                // 1. Advance Cascade Cards
                if (celebrationMode != CelebrationEffectMode.CONFETTI) {
                    launchTimer++
                    if (launchTimer >= launchInterval && currentLaunchingIndex < cascadeCards.size) {
                        val card = cascadeCards[currentLaunchingIndex]
                        card.isLaunched = true
                        // Calculate foundation origin (top-right portion of screen)
                        val suitOffsetIdx = currentLaunchingIndex % 4
                        card.x = 450f + (suitOffsetIdx * 110f)
                        card.y = 120f
                        // Direction: bias bouncing leftwards and slightly upwards initially
                        val dir = if (rand.nextBoolean()) -1f else (if (card.x > 700f) -1f else 1f)
                        card.vx = dir * (5.5f + rand.nextFloat() * 7.5f)
                        card.vy = -3f - rand.nextFloat() * 4.5f
                        currentLaunchingIndex++
                        launchTimer = 0
                    }

                    // Physics update for active cascade cards
                    val gravity = 0.72f
                    val floorBounce = 0.82f
                    val cardWidth = 140f
                    val cardHeight = 195f

                    for (i in 0 until currentLaunchingIndex) {
                        val card = cascadeCards[i]
                        if (!card.isLaunched || card.isFinished) continue

                        card.vy += gravity
                        card.x += card.vx
                        card.y += card.vy

                        // Save trail point every ~5 pixels of travel
                        val lastPoint = card.trail.lastOrNull()
                        val distSq = if (lastPoint != null) {
                            val dx = card.x - lastPoint.x
                            val dy = card.y - lastPoint.y
                            dx * dx + dy * dy
                        } else 100f

                        if (distSq >= 25f) {
                            card.trail.add(TrailPoint(card.x, card.y))
                            // Cap trail length per card for supreme performance
                            if (card.trail.size > 90) {
                                card.trail.removeAt(0)
                            }
                        }

                        // Floor bounce
                        val floorY = 2100f
                        if (card.y + cardHeight >= floorY) {
                            card.y = floorY - cardHeight
                            card.vy = -card.vy * floorBounce
                        }

                        // Check if card has exited screen bounds
                        if (card.x < -cardWidth * 2 || card.x > 1800f || (card.y > floorY + 100f)) {
                            card.isFinished = true
                        }
                    }
                }

                // 2. Advance Confetti Particles
                if (celebrationMode != CelebrationEffectMode.CASCADE) {
                    confettiParticles.forEach { p ->
                        p.wobble += 0.08f
                        p.x += p.vx + sin(p.wobble) * 2.2f
                        p.y += p.vy
                        p.rotation += p.vRot
                        if (p.y > 2300f) {
                            p.y = -40f
                            p.x = rand.nextFloat() * 1200f
                        }
                    }
                }
            }
        }
    }

    val textPaintRed = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#DC2626")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    val textPaintBlack = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x77000000))
    ) {
        // Combined Canvas for Cascading Cards and Confetti
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val scale = (canvasW / 1080f).coerceIn(0.7f, 1.3f)
            val cardW = 125f * scale
            val cardH = 175f * scale
            val cornerRadius = CornerRadius(10f * scale, 10f * scale)

            // 1. Render Cascading Cards & their Trails
            if (celebrationMode != CelebrationEffectMode.CONFETTI) {
                cascadeCards.forEach { card ->
                    if (card.isLaunched) {
                        val paint = if (card.suit.isRed) textPaintRed else textPaintBlack
                        paint.textSize = 24f * scale

                        // Draw historical trail positions (the classic solid cascade waterfall!)
                        card.trail.forEach { pt ->
                            val ptX = pt.x * (canvasW / 1080f)
                            val ptY = pt.y * (canvasH / 2100f)

                            // Card background
                            drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                topLeft = Offset(ptX, ptY),
                                size = Size(cardW, cardH),
                                cornerRadius = cornerRadius
                            )
                            // Subtle card border
                            drawRoundRect(
                                color = Color(0x33000000),
                                topLeft = Offset(ptX, ptY),
                                size = Size(cardW, cardH),
                                cornerRadius = cornerRadius,
                                style = Stroke(width = 1.2f)
                            )
                            // Corner indices
                            drawContext.canvas.nativeCanvas.drawText(
                                card.rank.label,
                                ptX + 8f * scale,
                                ptY + 24f * scale,
                                paint
                            )
                            // Small suit
                            drawSuit(
                                suit = card.suit,
                                topLeft = Offset(ptX + 8f * scale, ptY + 28f * scale),
                                size = Size(18f * scale, 18f * scale)
                            )
                            // Center suit emblem
                            drawSuit(
                                suit = card.suit,
                                topLeft = Offset(ptX + (cardW - 44f * scale) / 2f, ptY + (cardH - 44f * scale) / 2f),
                                size = Size(44f * scale, 44f * scale)
                            )
                        }

                        // Draw current active leading card
                        val leadX = card.x * (canvasW / 1080f)
                        val leadY = card.y * (canvasH / 2100f)

                        // Leading card shadow
                        drawRoundRect(
                            color = Color(0x55000000),
                            topLeft = Offset(leadX + 4f, leadY + 6f),
                            size = Size(cardW, cardH),
                            cornerRadius = cornerRadius
                        )
                        // Leading card body
                        drawRoundRect(
                            color = Color(0xFFFFFFFF),
                            topLeft = Offset(leadX, leadY),
                            size = Size(cardW, cardH),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            color = Color(0xFFE2E8F0),
                            topLeft = Offset(leadX, leadY),
                            size = Size(cardW, cardH),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.5f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            card.rank.label,
                            leadX + 8f * scale,
                            leadY + 24f * scale,
                            paint
                        )
                        drawSuit(
                            suit = card.suit,
                            topLeft = Offset(leadX + 8f * scale, leadY + 28f * scale),
                            size = Size(18f * scale, 18f * scale)
                        )
                        drawSuit(
                            suit = card.suit,
                            topLeft = Offset(leadX + (cardW - 44f * scale) / 2f, leadY + (cardH - 44f * scale) / 2f),
                            size = Size(44f * scale, 44f * scale)
                        )
                    }
                }
            }

            // 2. Render Screen Confetti Particles
            if (celebrationMode != CelebrationEffectMode.CASCADE) {
                confettiParticles.forEach { p ->
                    val drawX = (p.x % canvasW)
                    val drawY = p.y % canvasH
                    val pSize = p.size * scale

                    rotate(degrees = p.rotation, pivot = Offset(drawX, drawY)) {
                        when (p.shapeType) {
                            0 -> { // Rectangle ribbon
                                drawRect(
                                    color = p.color,
                                    topLeft = Offset(drawX, drawY),
                                    size = Size(pSize, pSize * 0.45f)
                                )
                            }
                            1 -> { // Circle sparkle
                                drawCircle(
                                    color = p.color,
                                    radius = pSize * 0.4f,
                                    center = Offset(drawX, drawY)
                                )
                            }
                            else -> { // Diamond / Star
                                drawRoundRect(
                                    color = p.color,
                                    topLeft = Offset(drawX, drawY),
                                    size = Size(pSize * 0.7f, pSize * 0.7f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating "Ver a Pantalla Completa" button or dismiss overlay
        if (!isDialogVisible) {
            Surface(
                color = Color(0xCC0F172A),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
                    .clickable { isDialogVisible = true }
                    .testTag("show_dialog_floating_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.tapToViewSummary,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Victory Card Dialog
        AnimatedVisibility(
            visible = isDialogVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(0.92f)
                    .testTag("win_dialog"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xF2131D2E) // Deep luxury glassmorphism
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
                border = BorderStroke(1.5.dp, Color(0x4038BDF8))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Trophy Circle with golden halo
                    Surface(
                        shape = CircleShape,
                        color = Color(0x33F59E0B),
                        border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.victoryTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )

                    Text(
                        text = strings.victorySubtitle,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stats in Victory
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x1EFFFFFF), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        VictoryStatItem(label = strings.timeLabel, value = formatTime(timeSeconds))
                        VictoryStatItem(label = strings.movesLabel, value = "$moves")
                        VictoryStatItem(label = strings.scoreLabel, value = "$score")
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Celebratory Effect Selector Buttons (Both, Cascade, Confetti)
                    Text(
                        text = strings.celebrationEffectLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CelebrationEffectMode.values().forEach { mode ->
                            val isSelected = celebrationMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0x1AFFFFFF),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color(0x33FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { celebrationMode = mode }
                            ) {
                                Text(
                                    text = when (mode) {
                                        CelebrationEffectMode.BOTH -> strings.effectBoth
                                        CelebrationEffectMode.CASCADE -> strings.effectCascade
                                        CelebrationEffectMode.CONFETTI -> strings.effectConfetti
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row: Repetir & Ver Pantalla Completa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { animationKey++ },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.rewindButton, fontSize = 12.sp, color = Color(0xFF38BDF8))
                        }

                        OutlinedButton(
                            onClick = { isDialogVisible = false },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.viewCascadeButton, fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Play Again Primary Button
                    Button(
                        onClick = onPlayAgainClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("play_again_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = strings.playAgainButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VictoryStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF38BDF8)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
