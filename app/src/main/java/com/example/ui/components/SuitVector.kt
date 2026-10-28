package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Suit

fun DrawScope.drawSuit(
    suit: Suit,
    topLeft: Offset,
    size: Size,
    colorOverride: Color? = null
) {
    val suitColor = colorOverride ?: if (suit.isRed) Color(0xFFDC2626) else Color(0xFF1E293B)
    val w = size.width
    val h = size.height

    translate(left = topLeft.x, top = topLeft.y) {
        when (suit) {
            Suit.HEARTS -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.85f)
                    cubicTo(w * 0.1f, h * 0.55f, 0f, h * 0.25f, w * 0.25f, h * 0.15f)
                    cubicTo(w * 0.45f, h * 0.05f, w * 0.5f, h * 0.3f, w * 0.5f, h * 0.35f)
                    cubicTo(w * 0.5f, h * 0.3f, w * 0.55f, h * 0.05f, w * 0.75f, h * 0.15f)
                    cubicTo(w * 1.0f, h * 0.25f, w * 0.9f, h * 0.55f, w * 0.5f, h * 0.85f)
                    close()
                }
                drawPath(path, color = suitColor)
            }
            Suit.DIAMONDS -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.9f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.92f)
                    lineTo(w * 0.1f, h * 0.5f)
                    close()
                }
                drawPath(path, color = suitColor)
            }
            Suit.CLUBS -> {
                val radius = w * 0.22f
                drawCircle(color = suitColor, radius = radius, center = Offset(w * 0.5f, h * 0.32f))
                drawCircle(color = suitColor, radius = radius, center = Offset(w * 0.32f, h * 0.55f))
                drawCircle(color = suitColor, radius = radius, center = Offset(w * 0.68f, h * 0.55f))
                drawCircle(color = suitColor, radius = radius * 0.8f, center = Offset(w * 0.5f, h * 0.48f))
                val stem = Path().apply {
                    moveTo(w * 0.46f, h * 0.52f)
                    lineTo(w * 0.32f, h * 0.9f)
                    lineTo(w * 0.68f, h * 0.9f)
                    lineTo(w * 0.54f, h * 0.52f)
                    close()
                }
                drawPath(stem, color = suitColor)
            }
            Suit.SPADES -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.10f)
                    cubicTo(w * 0.78f, h * 0.32f, w * 0.96f, h * 0.54f, w * 0.80f, h * 0.72f)
                    cubicTo(w * 0.65f, h * 0.82f, w * 0.52f, h * 0.68f, w * 0.5f, h * 0.62f)
                    cubicTo(w * 0.48f, h * 0.68f, w * 0.35f, h * 0.82f, w * 0.20f, h * 0.72f)
                    cubicTo(w * 0.04f, h * 0.54f, w * 0.22f, h * 0.32f, w * 0.5f, h * 0.10f)
                    close()
                }
                drawPath(path, color = suitColor)
                val stem = Path().apply {
                    moveTo(w * 0.46f, h * 0.58f)
                    lineTo(w * 0.34f, h * 0.9f)
                    lineTo(w * 0.66f, h * 0.9f)
                    lineTo(w * 0.54f, h * 0.58f)
                    close()
                }
                drawPath(stem, color = suitColor)
            }
        }
    }
}

@Composable
fun SuitIcon(
    suit: Suit,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    colorOverride: Color? = null
) {
    Canvas(modifier = modifier.size(size)) {
        drawSuit(suit = suit, topLeft = Offset.Zero, size = this.size, colorOverride = colorOverride)
    }
}
