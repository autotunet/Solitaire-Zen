package com.example

import com.example.logic.SolitaireEngine
import com.example.model.Card
import com.example.model.DealType
import com.example.model.Rank
import com.example.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolitaireEngineTest {

    @Test
    fun standardDeck_has52UniqueCards() {
        val deck = SolitaireEngine.createStandardDeck()
        assertEquals(52, deck.size)
        val uniqueIds = deck.map { it.id }.toSet()
        assertEquals(52, uniqueIds.size)

        Suit.values().forEach { suit ->
            val cardsOfSuit = deck.filter { it.suit == suit }
            assertEquals(13, cardsOfSuit.size)
        }
    }

    @Test
    fun dealGame_setsCorrectPiles() {
        val deal = SolitaireEngine.dealGame(DealType.RANDOM, randomSeed = 12345L)

        assertEquals(7, deal.tableau.size)
        for (i in 0 until 7) {
            assertEquals(i + 1, deal.tableau[i].size)
            // Last card should be face up
            assertTrue(deal.tableau[i].last().isFaceUp)
            // Preceding cards face down
            for (j in 0 until i) {
                assertFalse(deal.tableau[i][j].isFaceUp)
            }
        }

        assertEquals(24, deal.stock.size)
        assertTrue(deal.stock.all { !it.isFaceUp })
        assertEquals(0, deal.waste.size)
        assertEquals(4, deal.foundations.size)
        assertTrue(deal.foundations.all { it.isEmpty() })
    }

    @Test
    fun foundationRules_aceAndSequentialSameSuit() {
        val emptyFoundation = emptyList<Card>()
        val aceOfHearts = Card(1, Suit.HEARTS, Rank.ACE, isFaceUp = true)
        val twoOfHearts = Card(2, Suit.HEARTS, Rank.TWO, isFaceUp = true)
        val twoOfSpades = Card(3, Suit.SPADES, Rank.TWO, isFaceUp = true)
        val threeOfHearts = Card(4, Suit.HEARTS, Rank.THREE, isFaceUp = true)

        // Ace can go on empty foundation
        assertTrue(SolitaireEngine.canDropOnFoundation(aceOfHearts, emptyFoundation))
        // Two cannot go on empty foundation
        assertFalse(SolitaireEngine.canDropOnFoundation(twoOfHearts, emptyFoundation))

        val foundationWithAce = listOf(aceOfHearts)
        // Two of same suit can go on Ace
        assertTrue(SolitaireEngine.canDropOnFoundation(twoOfHearts, foundationWithAce))
        // Two of different suit cannot go on Ace of hearts
        assertFalse(SolitaireEngine.canDropOnFoundation(twoOfSpades, foundationWithAce))
        // Three cannot skip Two
        assertFalse(SolitaireEngine.canDropOnFoundation(threeOfHearts, foundationWithAce))
    }

    @Test
    fun tableauRules_alternatingColorDescendingRank() {
        val emptyColumn = emptyList<Card>()
        val kingOfSpades = Card(1, Suit.SPADES, Rank.KING, isFaceUp = true)
        val queenOfHearts = Card(2, Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
        val queenOfSpades = Card(3, Suit.SPADES, Rank.QUEEN, isFaceUp = true)
        val jackOfSpades = Card(4, Suit.SPADES, Rank.JACK, isFaceUp = true)

        // Only King on empty column
        assertTrue(SolitaireEngine.canDropOnTableau(kingOfSpades, emptyColumn))
        assertFalse(SolitaireEngine.canDropOnTableau(queenOfHearts, emptyColumn))

        val columnWithKing = listOf(kingOfSpades)
        // Red Queen on Black King: valid
        assertTrue(SolitaireEngine.canDropOnTableau(queenOfHearts, columnWithKing))
        // Black Queen on Black King: invalid
        assertFalse(SolitaireEngine.canDropOnTableau(queenOfSpades, columnWithKing))
        // Jack on King (skip rank): invalid
        assertFalse(SolitaireEngine.canDropOnTableau(jackOfSpades, columnWithKing))
    }

    @Test
    fun autoCompleteDetector_identifiesFinishedTableau() {
        val faceUpCol = listOf(Card(1, Suit.HEARTS, Rank.ACE, isFaceUp = true))
        val tableauAllFaceUp = List(7) { faceUpCol }

        // When stock is empty and all face up -> ready
        assertTrue(SolitaireEngine.isAutoCompleteReady(tableauAllFaceUp, stock = emptyList(), waste = emptyList()))

        // When stock has cards -> not ready
        assertFalse(SolitaireEngine.isAutoCompleteReady(tableauAllFaceUp, stock = listOf(Card(2, Suit.SPADES, Rank.KING)), waste = emptyList()))

        // When any tableau card is face down -> not ready
        val mixedCol = listOf(Card(3, Suit.CLUBS, Rank.TEN, isFaceUp = false), Card(4, Suit.DIAMONDS, Rank.NINE, isFaceUp = true))
        val tableauWithHidden = listOf(mixedCol) + List(6) { faceUpCol }
        assertFalse(SolitaireEngine.isAutoCompleteReady(tableauWithHidden, stock = emptyList(), waste = emptyList()))
    }

    @Test
    fun undoRedoStack_maintainsCompleteBoardConsistency() {
        val deal = SolitaireEngine.dealGame(DealType.RANDOM, randomSeed = 999L)
        val initialStockSize = deal.stock.size
        val initialWasteSize = deal.waste.size

        // Snapshot initial state
        val undoStack = mutableListOf<com.example.model.GameStateSnapshot>()
        val redoStack = mutableListOf<com.example.model.GameStateSnapshot>()

        fun snapshot(stateTableau: List<List<Card>>, stateStock: List<Card>, stateWaste: List<Card>, moves: Int) {
            undoStack.add(
                com.example.model.GameStateSnapshot(
                    tableau = stateTableau.map { col -> col.map { it.copy() } },
                    foundations = List(4) { emptyList() },
                    stock = stateStock.map { it.copy() },
                    waste = stateWaste.map { it.copy() },
                    moves = moves,
                    score = 0
                )
            )
            redoStack.clear()
        }

        // 1. Initial snapshot
        snapshot(deal.tableau, deal.stock, deal.waste, 0)
        assertEquals(1, undoStack.size)

        // 2. Perform a move: draw 1 card from stock to waste
        val drawn = deal.stock.take(1).map { it.copy(isFaceUp = true) }
        val stockAfterDraw = deal.stock.drop(1)
        val wasteAfterDraw = deal.waste + drawn
        snapshot(deal.tableau, stockAfterDraw, wasteAfterDraw, 1)

        assertEquals(2, undoStack.size)
        assertEquals(initialStockSize - 1, stockAfterDraw.size)
        assertEquals(1, wasteAfterDraw.size)

        // 3. Undo: pop top and restore previous
        val current = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(current)
        val restored = undoStack.last()

        assertEquals(initialStockSize, restored.stock.size)
        assertEquals(0, restored.waste.size)
        assertEquals(0, restored.moves)
        assertEquals(1, redoStack.size)

        // 4. Redo: pop redoStack and reapply
        val redoState = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(redoState)

        assertEquals(initialStockSize - 1, redoState.stock.size)
        assertEquals(1, redoState.waste.size)
        assertEquals(1, redoState.moves)
        assertEquals(0, redoStack.size)
    }

    @Test
    fun findNextAutoCompleteMove_prioritizesLowestRankToFoundations() {
        val aceOfSpades = Card(1, Suit.SPADES, Rank.ACE, isFaceUp = true)
        val twoOfHearts = Card(2, Suit.HEARTS, Rank.TWO, isFaceUp = true)
        val aceOfHearts = Card(3, Suit.HEARTS, Rank.ACE, isFaceUp = true)

        val foundations = listOf(
            listOf(aceOfHearts), // Hearts foundation has Ace, so Two can be placed
            emptyList(),
            emptyList(),
            emptyList()
        )

        val tableau = listOf(
            listOf(twoOfHearts),
            listOf(aceOfSpades),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )

        // Both Ace of Spades (Rank.ACE = 1) and Two of Hearts (Rank.TWO = 2) are valid foundation moves.
        // Auto-complete must prioritize Ace (lowest rank = 1).
        val move = SolitaireEngine.findNextAutoCompleteMove(tableau, waste = emptyList(), foundations = foundations)
        assertNotNull(move)
        assertEquals(Rank.ACE, move!!.card.rank)
        assertEquals(Suit.SPADES, move.card.suit)
    }
}
