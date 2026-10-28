package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Card
import com.example.model.CardBackTheme
import com.example.model.CardPileLocation
import com.example.model.HintInfo
import com.example.model.Suit
import com.example.model.TableTheme

@Composable
fun EmptySlot(
    width: Dp,
    height: Dp,
    tableTheme: TableTheme,
    modifier: Modifier = Modifier,
    isTargetHint: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    val shape = RoundedCornerShape(6.dp)
    val infiniteTransition = rememberInfiniteTransition(label = "slotHint")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )

    val borderStroke = if (isTargetHint) {
        BorderStroke(2.dp, Color(0xFF38BDF8).copy(alpha = hintAlpha))
    } else {
        BorderStroke(1.dp, tableTheme.slotBorderColor)
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(tableTheme.slotBgColor, shape)
            .border(borderStroke, shape)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun FoundationSlot(
    suitIndex: Int,
    cards: List<Card>,
    width: Dp,
    height: Dp,
    tableTheme: TableTheme,
    cardBackTheme: CardBackTheme,
    hintInfo: HintInfo?,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    val associatedSuit = when (suitIndex) {
        0 -> Suit.HEARTS
        1 -> Suit.SPADES
        2 -> Suit.DIAMONDS
        else -> Suit.CLUBS
    }

    val isTargetHint = hintInfo?.to is CardPileLocation.Foundation &&
            (hintInfo.to as CardPileLocation.Foundation).suitIndex == suitIndex

    Box(modifier = modifier) {
        if (cards.isEmpty()) {
            EmptySlot(
                width = width,
                height = height,
                tableTheme = tableTheme,
                isTargetHint = isTargetHint
            ) {
                // Subtle suit watermark
                SuitIcon(
                    suit = associatedSuit,
                    size = (width * 0.42f).coerceAtLeast(16.dp),
                    colorOverride = tableTheme.slotBorderColor.copy(alpha = 0.45f)
                )
            }
        } else {
            val topCard = cards.last()
            val isSourceHint = hintInfo?.from is CardPileLocation.Foundation &&
                    (hintInfo.from as CardPileLocation.Foundation).suitIndex == suitIndex

            CardComponent(
                card = topCard,
                cardWidth = width,
                cardHeight = height,
                cardBackTheme = cardBackTheme,
                isHintSource = isSourceHint,
                isHintTarget = isTargetHint,
                onClick = { onCardClick(topCard) },
                modifier = Modifier.testTag("foundation_${suitIndex}_top")
            )
        }
    }
}

@Composable
fun StockWasteRow(
    stockCards: List<Card>,
    wasteCards: List<Card>,
    width: Dp,
    height: Dp,
    tableTheme: TableTheme,
    cardBackTheme: CardBackTheme,
    hintInfo: HintInfo?,
    onStockClick: () -> Unit,
    onWasteCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    val isStockHint = hintInfo?.from is CardPileLocation.Stock
    val isRecycleHint = hintInfo?.from is CardPileLocation.Waste && hintInfo.to is CardPileLocation.Stock
    val isWasteSourceHint = hintInfo?.from is CardPileLocation.Waste && hintInfo.to !is CardPileLocation.Stock

    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stock Pile
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onStockClick
                )
                .testTag("stock_pile")
        ) {
            if (stockCards.isEmpty()) {
                EmptySlot(
                    width = width,
                    height = height,
                    tableTheme = tableTheme,
                    isTargetHint = isRecycleHint
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reciclar mazo",
                        tint = if (isRecycleHint) Color(0xFFFBBF24) else tableTheme.slotBorderColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // Stock pile with remaining badge
                CardComponent(
                    card = stockCards.first().copy(isFaceUp = false),
                    cardWidth = width,
                    cardHeight = height,
                    cardBackTheme = cardBackTheme,
                    isHintSource = isStockHint
                )

                // Badge with count
                Surface(
                    color = Color(0xCC0F172A),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0x40FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                ) {
                    Text(
                        text = "${stockCards.size}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Waste Pile
        Box(
            modifier = Modifier.testTag("waste_pile")
        ) {
            if (wasteCards.isEmpty()) {
                EmptySlot(
                    width = width,
                    height = height,
                    tableTheme = tableTheme
                )
            } else {
                // In draw 3, show up to 3 cards overlapping slightly
                val visibleCount = minOf(wasteCards.size, 3)
                val visibleCards = wasteCards.takeLast(visibleCount)

                Box(modifier = Modifier.width(width + (visibleCount - 1).coerceAtLeast(0).dp * 14)) {
                    visibleCards.forEachIndexed { index, card ->
                        val isTop = index == visibleCards.lastIndex
                        val isHint = isTop && isWasteSourceHint
                        CardComponent(
                            card = card,
                            cardWidth = width,
                            cardHeight = height,
                            cardBackTheme = cardBackTheme,
                            isHintSource = isHint,
                            onClick = { if (isTop) onWasteCardClick(card) },
                            modifier = Modifier
                                .offset(x = (index * 14).dp)
                                .testTag("waste_card_$index")
                        )
                    }
                }
            }
        }
    }
}
