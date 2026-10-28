package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Card
import com.example.model.CardBackTheme
import com.example.model.Rank

@Composable
fun CardComponent(
    card: Card,
    cardWidth: Dp,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
    cardBackTheme: CardBackTheme = CardBackTheme.ROYAL_GOLD,
    isSelected: Boolean = false,
    isHintSource: Boolean = false,
    isHintTarget: Boolean = false,
    onClick: () -> Unit = {}
) {
    val cornerRadius = 6.dp
    val shape = RoundedCornerShape(cornerRadius)

    val infiniteTransition = rememberInfiniteTransition(label = "hintPulse")
    val hintPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )

    val elevation = when {
        isSelected -> 10.dp
        isHintSource -> 8.dp
        else -> 3.dp
    }

    val borderStroke = when {
        isHintSource -> BorderStroke(2.5.dp, Color(0xFFFBBF24).copy(alpha = hintPulseAlpha))
        isHintTarget -> BorderStroke(2.5.dp, Color(0xFF38BDF8).copy(alpha = hintPulseAlpha))
        isSelected -> BorderStroke(2.dp, Color(0xFF3B82F6))
        card.isFaceUp -> BorderStroke(0.75.dp, Color(0x33A1A1AA))
        else -> BorderStroke(1.dp, Color(0x44FFFFFF))
    }

    Surface(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .shadow(elevation = elevation, shape = shape, spotColor = Color(0x66000000))
            .border(borderStroke, shape)
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = if (card.isFaceUp) Color(0xFFFCFCFD) else cardBackTheme.primaryColor
    ) {
        if (card.isFaceUp) {
            CardFaceContent(card = card, cardWidth = cardWidth, cardHeight = cardHeight)
        } else {
            CardBackContent(theme = cardBackTheme)
        }
    }
}

@Composable
private fun CardFaceContent(
    card: Card,
    cardWidth: Dp,
    cardHeight: Dp
) {
    val textColor = if (card.isRed) Color(0xFFDC2626) else Color(0xFF0F172A)
    val rankTextSize = if (cardWidth < 48.dp) 11.sp else 13.sp
    val suitIconSize = if (cardWidth < 48.dp) 10.dp else 12.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC)
                    )
                )
            )
            .padding(3.dp)
    ) {
        // Top Left Index
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = card.rank.label,
                fontSize = rankTextSize,
                fontWeight = FontWeight.Black,
                color = textColor,
                lineHeight = rankTextSize
            )
            Spacer(modifier = Modifier.height(1.dp))
            SuitIcon(suit = card.suit, size = suitIconSize)
        }

        // Center Graphic
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                card.rank == Rank.ACE -> {
                    // Large Center Ace emblem
                    val centerAceSize = (cardWidth * 0.44f).coerceAtLeast(18.dp)
                    SuitIcon(suit = card.suit, size = centerAceSize)
                }
                card.rank.value >= 11 -> {
                    // Face card badge (J, Q, K)
                    FaceCardCrown(
                        rank = card.rank,
                        suit = card.suit,
                        width = cardWidth * 0.52f,
                        height = cardHeight * 0.42f
                    )
                }
                else -> {
                    // Number card center emblem with rank and suit
                    NumberCardCenter(
                        rank = card.rank,
                        suit = card.suit,
                        width = cardWidth,
                        height = cardHeight
                    )
                }
            }
        }

        // Bottom Right Inverted Index
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .rotate(180f)
        ) {
            Text(
                text = card.rank.label,
                fontSize = rankTextSize,
                fontWeight = FontWeight.Black,
                color = textColor,
                lineHeight = rankTextSize
            )
            Spacer(modifier = Modifier.height(1.dp))
            SuitIcon(suit = card.suit, size = suitIconSize)
        }
    }
}

@Composable
private fun FaceCardCrown(
    rank: Rank,
    suit: com.example.model.Suit,
    width: Dp,
    height: Dp
) {
    val suitColor = if (suit.isRed) Color(0xFFDC2626) else Color(0xFF0F172A)
    val goldColor = Color(0xFFEAB308)

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .border(1.dp, goldColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(Color(0x0C000000), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (rank) {
                    Rank.JACK -> "J"
                    Rank.QUEEN -> "Q"
                    Rank.KING -> "K"
                    else -> rank.label
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = suitColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            SuitIcon(suit = suit, size = 12.dp)
        }
    }
}

@Composable
private fun NumberCardCenter(
    rank: Rank,
    suit: com.example.model.Suit,
    width: Dp,
    height: Dp
) {
    val centerSuitSize = (width * 0.38f).coerceAtLeast(14.dp)
    SuitIcon(suit = suit, size = centerSuitSize)
}

@Composable
private fun CardBackContent(
    theme: CardBackTheme
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp)
    ) {
        val w = size.width
        val h = size.height
        val primary = theme.primaryColor
        val accent = theme.accentColor

        // Draw inner border
        drawRoundRect(
            color = accent.copy(alpha = 0.75f),
            topLeft = Offset(2f, 2f),
            size = Size(w - 4f, h - 4f),
            style = Stroke(width = 1.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )

        when (theme.patternType) {
            "geometric", "diamonds" -> {
                // Diamond mesh pattern
                val step = (w / 4).coerceAtLeast(8f)
                var x = 4f
                while (x < w - 4f) {
                    var y = 4f
                    while (y < h - 4f) {
                        val path = Path().apply {
                            moveTo(x + step / 2, y)
                            lineTo(x + step, y + step / 2)
                            lineTo(x + step / 2, y + step)
                            lineTo(x, y + step / 2)
                            close()
                        }
                        drawPath(path, color = accent.copy(alpha = 0.25f), style = Stroke(width = 1f))
                        y += step
                    }
                    x += step
                }
                // Central jewel
                drawCircle(color = accent, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = primary, radius = w * 0.11f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = accent, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.5f))
            }
            "neon" -> {
                // Cyberpunk circuit lines
                drawLine(accent, Offset(4f, h * 0.3f), Offset(w * 0.5f, h * 0.5f), strokeWidth = 1.5f)
                drawLine(accent, Offset(w - 4f, h * 0.3f), Offset(w * 0.5f, h * 0.5f), strokeWidth = 1.5f)
                drawLine(accent, Offset(4f, h * 0.7f), Offset(w * 0.5f, h * 0.5f), strokeWidth = 1.5f)
                drawLine(accent, Offset(w - 4f, h * 0.7f), Offset(w * 0.5f, h * 0.5f), strokeWidth = 1.5f)
                drawCircle(color = accent, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 2f))
                drawCircle(color = accent, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.5f))
            }
            else -> {
                // Ornate lattice
                drawCircle(color = accent.copy(alpha = 0.35f), radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.2f))
                drawCircle(color = accent.copy(alpha = 0.6f), radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.5f))
                drawCircle(color = accent, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.5f))
            }
        }
    }
}
