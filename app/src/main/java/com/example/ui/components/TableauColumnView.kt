package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.model.TableTheme

@Composable
fun TableauColumnView(
    columnIndex: Int,
    cards: List<Card>,
    cardWidth: Dp,
    cardHeight: Dp,
    maxHeight: Dp,
    tableTheme: TableTheme,
    cardBackTheme: CardBackTheme,
    hintInfo: HintInfo?,
    onCardClick: (cardIndex: Int, card: Card) -> Unit,
    onEmptyColumnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTargetHint = hintInfo?.to is CardPileLocation.Tableau &&
            (hintInfo.to as CardPileLocation.Tableau).colIndex == columnIndex

    if (cards.isEmpty()) {
        Box(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEmptyColumnClick
                )
                .testTag("tableau_col_${columnIndex}_empty")
        ) {
            EmptySlot(
                width = cardWidth,
                height = cardHeight,
                tableTheme = tableTheme,
                isTargetHint = isTargetHint
            ) {
                Text(
                    text = "K",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = tableTheme.slotBorderColor.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        // Calculate dynamic vertical offsets
        val faceDownCount = cards.count { !it.isFaceUp }
        val faceUpCount = cards.size - faceDownCount

        // Available vertical scroll/expansion space
        val availableExtraSpace = (maxHeight - cardHeight).coerceAtLeast(0.dp)
        val defaultFaceDownOffset = 14.dp
        val defaultFaceUpOffset = 26.dp

        val totalDefaultHeight = (faceDownCount * defaultFaceDownOffset.value + (faceUpCount - 1).coerceAtLeast(0) * defaultFaceUpOffset.value).dp

        val scaleFactor = if (totalDefaultHeight > availableExtraSpace && totalDefaultHeight.value > 0f) {
            (availableExtraSpace.value / totalDefaultHeight.value).coerceIn(0.55f, 1.0f)
        } else {
            1.0f
        }

        val actualFaceDownOffset = defaultFaceDownOffset * scaleFactor
        val actualFaceUpOffset = defaultFaceUpOffset * scaleFactor

        // Compute accumulated Y offsets for each card
        var accumulatedY = 0.dp
        val yOffsets = cards.mapIndexed { index, card ->
            val y = accumulatedY
            if (index < cards.lastIndex) {
                accumulatedY += if (card.isFaceUp) actualFaceUpOffset else actualFaceDownOffset
            }
            y
        }

        val columnHeight = accumulatedY + cardHeight

        Box(
            modifier = modifier
                .width(cardWidth)
                .height(columnHeight)
                .testTag("tableau_col_$columnIndex")
        ) {
            cards.forEachIndexed { cardIndex, card ->
                val yOffset = yOffsets[cardIndex]

                val isSourceHint = hintInfo?.from is CardPileLocation.Tableau &&
                        (hintInfo.from as CardPileLocation.Tableau).colIndex == columnIndex &&
                        (hintInfo.from as CardPileLocation.Tableau).cardIndex == cardIndex

                val isColTargetHint = isTargetHint && cardIndex == cards.lastIndex

                CardComponent(
                    card = card,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    cardBackTheme = cardBackTheme,
                    isHintSource = isSourceHint,
                    isHintTarget = isColTargetHint,
                    onClick = {
                        if (card.isFaceUp) {
                            onCardClick(cardIndex, card)
                        }
                    },
                    modifier = Modifier
                        .offset(y = yOffset)
                        .testTag("card_${columnIndex}_$cardIndex")
                )
            }
        }
    }
}
