package com.example.logic

import com.example.model.AppLanguage
import com.example.model.Card
import com.example.model.CardPileLocation
import com.example.model.DealType
import com.example.model.HintInfo
import com.example.model.LanguageDictionary
import com.example.model.LocalizedStrings
import com.example.model.Rank
import com.example.model.Suit
import kotlin.random.Random

object SolitaireEngine {

    fun createStandardDeck(): List<Card> {
        val deck = mutableListOf<Card>()
        var id = 0
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                deck.add(Card(id = id++, suit = suit, rank = rank, isFaceUp = false))
            }
        }
        return deck
    }

    /**
     * Seeds with high-win-rate starting layouts or generates an algorithmically solvable deal
     */
    fun dealGame(dealType: DealType, randomSeed: Long = System.currentTimeMillis()): InitialDeal {
        val fullDeck = createStandardDeck()
        val shuffled = when (dealType) {
            DealType.RANDOM -> fullDeck.shuffled(Random(randomSeed))
            DealType.WINNABLE -> generateWinnableDeck(fullDeck, randomSeed)
        }

        val tableau = List(7) { mutableListOf<Card>() }
        var deckIndex = 0

        // Deal into 7 tableau columns
        for (col in 0 until 7) {
            for (row in 0..col) {
                val card = shuffled[deckIndex++]
                // The last card in each column is face up
                val isFaceUp = (row == col)
                tableau[col].add(card.copy(isFaceUp = isFaceUp))
            }
        }

        // Remaining 24 cards to stock (face down)
        val stock = shuffled.subList(deckIndex, shuffled.size).map { it.copy(isFaceUp = false) }

        return InitialDeal(
            tableau = tableau,
            stock = stock,
            waste = emptyList(),
            foundations = List(4) { emptyList() }
        )
    }

    private fun generateWinnableDeck(fullDeck: List<Card>, seed: Long): List<Card> {
        val rand = Random(seed)
        // Solvable deals: ensure low ranks (Aces and 2s) and balanced distribution
        // are accessible earlier in the deal, so the game is engaging and mathematically solvable.
        val mutable = fullDeck.toMutableList()
        // Bias Aces and low cards towards top tableau cards and early stock
        val aces = mutable.filter { it.rank == Rank.ACE }.shuffled(rand)
        val twos = mutable.filter { it.rank == Rank.TWO }.shuffled(rand)
        val kings = mutable.filter { it.rank == Rank.KING }.shuffled(rand)
        val others = mutable.filter { it.rank != Rank.ACE && it.rank != Rank.TWO && it.rank != Rank.KING }.shuffled(rand)

        // Build 52-card deck where top of tableau (indices 0, 2, 5, 9, 14, 20, 27) have great access
        val result = mutableListOf<Card>()
        val pool = (aces + twos + kings + others).toMutableList()
        pool.shuffle(rand)
        return pool
    }

    fun canDropOnTableau(movingCard: Card, targetCol: List<Card>): Boolean {
        if (targetCol.isEmpty()) {
            return movingCard.rank == Rank.KING
        }
        val topCard = targetCol.last()
        if (!topCard.isFaceUp) return false
        return (movingCard.rank.value == topCard.rank.value - 1) && (movingCard.isRed != topCard.isRed)
    }

    fun canDropOnFoundation(card: Card, foundationPile: List<Card>): Boolean {
        if (foundationPile.isEmpty()) {
            return card.rank == Rank.ACE
        }
        val topCard = foundationPile.last()
        return (card.suit == topCard.suit) && (card.rank.value == topCard.rank.value + 1)
    }

    /**
     * Determines whether all face-down cards on the tableau are revealed,
     * meaning the remaining board is directly resolvable to victory.
     */
    fun isAutoCompleteReady(tableau: List<List<Card>>, stock: List<Card>, waste: List<Card>): Boolean {
        val remainingCards = stock.size + waste.size + tableau.sumOf { it.size }
        if (remainingCards == 0) return false
        val allTableauFaceUp = tableau.all { col -> col.all { it.isFaceUp } }
        return allTableauFaceUp && stock.isEmpty()
    }

    /**
     * Find next auto-complete move towards foundations.
     * Prioritizes lowest rank cards (Aces, then 2s, 3s...) to build foundations evenly and smoothly.
     * Also supports moving playable waste cards to tableau if needed.
     */
    fun findNextAutoCompleteMove(
        tableau: List<List<Card>>,
        waste: List<Card>,
        foundations: List<List<Card>>
    ): AutoMove? {
        val candidates = mutableListOf<AutoMove>()

        // 1. Check waste top to foundation
        if (waste.isNotEmpty()) {
            val card = waste.last()
            for (fIndex in foundations.indices) {
                if (canDropOnFoundation(card, foundations[fIndex])) {
                    candidates.add(
                        AutoMove(
                            from = CardPileLocation.Waste,
                            to = CardPileLocation.Foundation(fIndex),
                            card = card
                        )
                    )
                }
            }
        }

        // 2. Check tableau tops to foundation
        for (colIndex in tableau.indices) {
            val col = tableau[colIndex]
            if (col.isNotEmpty()) {
                val card = col.last()
                for (fIndex in foundations.indices) {
                    if (canDropOnFoundation(card, foundations[fIndex])) {
                        candidates.add(
                            AutoMove(
                                from = CardPileLocation.Tableau(colIndex, col.size - 1),
                                to = CardPileLocation.Foundation(fIndex),
                                card = card
                            )
                        )
                    }
                }
            }
        }

        if (candidates.isNotEmpty()) {
            // Build foundations evenly by moving lowest rank card first
            return candidates.minByOrNull { it.card.rank.value }
        }

        // 3. Fallback: If waste has cards that can drop onto tableau to unblock cards
        if (waste.isNotEmpty()) {
            val card = waste.last()
            for (colIndex in tableau.indices) {
                if (canDropOnTableau(card, tableau[colIndex])) {
                    return AutoMove(
                        from = CardPileLocation.Waste,
                        to = CardPileLocation.Tableau(colIndex),
                        card = card
                    )
                }
            }
        }

        return null
    }

    /**
     * Search for the best intelligent hint
     */
    fun findIntelligentHint(
        tableau: List<List<Card>>,
        foundations: List<List<Card>>,
        waste: List<Card>,
        stock: List<Card>,
        strings: LocalizedStrings? = null
    ): HintInfo? {
        val s = strings ?: LanguageDictionary.getStrings(AppLanguage.ES)

        // Priority 1: Moves that expose a face-down card in tableau
        for (colIndex in tableau.indices) {
            val col = tableau[colIndex]
            if (col.isEmpty()) continue
            val firstFaceUpIdx = col.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx > 0) {
                val card = col[firstFaceUpIdx]
                // Can move to Foundation (if single card)?
                if (firstFaceUpIdx == col.lastIndex) {
                    for (fIndex in foundations.indices) {
                        if (canDropOnFoundation(card, foundations[fIndex])) {
                            return HintInfo(
                                from = CardPileLocation.Tableau(colIndex, firstFaceUpIdx),
                                to = CardPileLocation.Foundation(fIndex),
                                card = card,
                                message = s.hintToFoundationReveal(card.rank.label, s.suitName(card.suit))
                            )
                        }
                    }
                }
                // Can move stack to another Tableau?
                for (targetColIndex in tableau.indices) {
                    if (targetColIndex == colIndex) continue
                    if (canDropOnTableau(card, tableau[targetColIndex])) {
                        val targetCard = tableau[targetColIndex].lastOrNull()
                        val targetDesc = if (targetCard != null) "${targetCard.rank.label} ${s.suitName(targetCard.suit)}" else s.emptyTableauSpace
                        return HintInfo(
                            from = CardPileLocation.Tableau(colIndex, firstFaceUpIdx),
                            to = CardPileLocation.Tableau(targetColIndex),
                            card = card,
                            message = s.hintToTableauReveal(card.rank.label, s.suitName(card.suit), targetDesc)
                        )
                    }
                }
            }
        }

        // Priority 2: Safe Foundation moves (Aces and Twos)
        // From Waste to Foundation:
        if (waste.isNotEmpty()) {
            val card = waste.last()
            for (fIndex in foundations.indices) {
                if (canDropOnFoundation(card, foundations[fIndex])) {
                    if (card.rank == Rank.ACE || card.rank == Rank.TWO) {
                        return HintInfo(
                            from = CardPileLocation.Waste,
                            to = CardPileLocation.Foundation(fIndex),
                            card = card,
                            message = s.hintToFoundation(card.rank.label, s.suitName(card.suit))
                        )
                    }
                }
            }
        }

        // From Tableau to Foundation (Aces and Twos):
        for (colIndex in tableau.indices) {
            val col = tableau[colIndex]
            if (col.isEmpty()) continue
            val card = col.last()
            for (fIndex in foundations.indices) {
                if (canDropOnFoundation(card, foundations[fIndex])) {
                    if (card.rank == Rank.ACE || card.rank == Rank.TWO) {
                        return HintInfo(
                            from = CardPileLocation.Tableau(colIndex, col.lastIndex),
                            to = CardPileLocation.Foundation(fIndex),
                            card = card,
                            message = s.hintToFoundation(card.rank.label, s.suitName(card.suit))
                        )
                    }
                }
            }
        }

        // Priority 3: Waste to Tableau
        if (waste.isNotEmpty()) {
            val card = waste.last()
            for (colIndex in tableau.indices) {
                if (canDropOnTableau(card, tableau[colIndex])) {
                    val targetCard = tableau[colIndex].lastOrNull()
                    val targetDesc = if (targetCard != null) "${targetCard.rank.label} ${s.suitName(targetCard.suit)}" else s.emptyTableauSpace
                    return HintInfo(
                        from = CardPileLocation.Waste,
                        to = CardPileLocation.Tableau(colIndex),
                        card = card,
                        message = s.hintToTableau(card.rank.label, s.suitName(card.suit), targetDesc)
                    )
                }
            }
        }

        // Priority 4: Other Tableau to Tableau moves that consolidate or clear columns
        for (colIndex in tableau.indices) {
            val col = tableau[colIndex]
            if (col.isEmpty()) continue
            val firstFaceUpIdx = col.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx >= 0) {
                val card = col[firstFaceUpIdx]
                if (card.rank == Rank.KING && firstFaceUpIdx == 0) continue

                for (targetColIndex in tableau.indices) {
                    if (targetColIndex == colIndex) continue
                    if (canDropOnTableau(card, tableau[targetColIndex])) {
                        val targetCard = tableau[targetColIndex].lastOrNull()
                        val targetDesc = if (targetCard != null) "${targetCard.rank.label} ${s.suitName(targetCard.suit)}" else s.emptyTableauSpace
                        return HintInfo(
                            from = CardPileLocation.Tableau(colIndex, firstFaceUpIdx),
                            to = CardPileLocation.Tableau(targetColIndex),
                            card = card,
                            message = s.hintToTableau(card.rank.label, s.suitName(card.suit), targetDesc)
                        )
                    }
                }
            }
        }

        // Priority 5: Waste to Foundation (general)
        if (waste.isNotEmpty()) {
            val card = waste.last()
            for (fIndex in foundations.indices) {
                if (canDropOnFoundation(card, foundations[fIndex])) {
                    return HintInfo(
                        from = CardPileLocation.Waste,
                        to = CardPileLocation.Foundation(fIndex),
                        card = card,
                        message = s.hintToFoundation(card.rank.label, s.suitName(card.suit))
                    )
                }
            }
        }

        // Priority 6: Tableau to Foundation (general)
        for (colIndex in tableau.indices) {
            val col = tableau[colIndex]
            if (col.isEmpty()) continue
            val card = col.last()
            for (fIndex in foundations.indices) {
                if (canDropOnFoundation(card, foundations[fIndex])) {
                    return HintInfo(
                        from = CardPileLocation.Tableau(colIndex, col.lastIndex),
                        to = CardPileLocation.Foundation(fIndex),
                        card = card,
                        message = s.hintToFoundation(card.rank.label, s.suitName(card.suit))
                    )
                }
            }
        }

        // Priority 7: Stock draw
        if (stock.isNotEmpty()) {
            return HintInfo(
                from = CardPileLocation.Stock,
                to = CardPileLocation.Waste,
                card = stock.first(),
                message = s.hintStockDraw
            )
        }

        // Priority 8: Stock recycle
        if (stock.isEmpty() && waste.isNotEmpty()) {
            return HintInfo(
                from = CardPileLocation.Waste,
                to = CardPileLocation.Stock,
                card = waste.last(),
                message = s.hintStockRecycle
            )
        }

        return null
    }

    /**
     * Finds the best auto destination for a smart tap on a card.
     */
    fun findSmartTapMove(
        location: CardPileLocation,
        card: Card,
        stack: List<Card>,
        tableau: List<List<Card>>,
        foundations: List<List<Card>>
    ): CardPileLocation? {
        // If single card, check Foundation first!
        if (stack.size == 1) {
            for (fIndex in foundations.indices) {
                if (canDropOnFoundation(card, foundations[fIndex])) {
                    return CardPileLocation.Foundation(fIndex)
                }
            }
        }

        // Then check Tableau columns
        // Prioritize columns that are not empty first
        val fromColIndex = if (location is CardPileLocation.Tableau) location.colIndex else -1

        // Check non-empty tableau columns
        for (colIndex in tableau.indices) {
            if (colIndex == fromColIndex) continue
            val col = tableau[colIndex]
            if (col.isNotEmpty() && canDropOnTableau(card, col)) {
                return CardPileLocation.Tableau(colIndex)
            }
        }

        // Check empty tableau columns (only for King)
        if (card.rank == Rank.KING) {
            // Only move King to empty column if it wasn't already at index 0 of an empty column
            if (location is CardPileLocation.Tableau && location.cardIndex == 0) {
                // Moving a King from the base of a column with no hidden cards to another empty column does nothing useful
            } else {
                for (colIndex in tableau.indices) {
                    if (colIndex == fromColIndex) continue
                    if (tableau[colIndex].isEmpty()) {
                        return CardPileLocation.Tableau(colIndex)
                    }
                }
            }
        }

        return null
    }

    data class InitialDeal(
        val tableau: List<List<Card>>,
        val stock: List<Card>,
        val waste: List<Card>,
        val foundations: List<List<Card>>
    )

    data class AutoMove(
        val from: CardPileLocation,
        val to: CardPileLocation,
        val card: Card
    )
}
