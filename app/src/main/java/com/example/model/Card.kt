package com.example.model

enum class Suit(val symbol: String, val isRed: Boolean, val displayName: String) {
    HEARTS("♥", true, "Corazones"),
    DIAMONDS("♦", true, "Diamantes"),
    CLUBS("♣", false, "Tréboles"),
    SPADES("♠", false, "Picas")
}

enum class Rank(val value: Int, val label: String) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K")
}

data class Card(
    val id: Int,
    val suit: Suit,
    val rank: Rank,
    val isFaceUp: Boolean = false
) {
    val isRed: Boolean get() = suit.isRed
    val isBlack: Boolean get() = !suit.isRed

    fun flip(faceUp: Boolean = !isFaceUp): Card = copy(isFaceUp = faceUp)
}

sealed interface CardPileLocation {
    object Stock : CardPileLocation
    object Waste : CardPileLocation
    data class Foundation(val suitIndex: Int) : CardPileLocation
    data class Tableau(val colIndex: Int, val cardIndex: Int = -1) : CardPileLocation
}

data class MoveAction(
    val from: CardPileLocation,
    val to: CardPileLocation,
    val cards: List<Card>,
    val revealedPreviousCardIndex: Int? = null,
    val scoreDelta: Int = 0
)

data class HintInfo(
    val from: CardPileLocation,
    val to: CardPileLocation,
    val card: Card,
    val message: String
)

data class GameStateSnapshot(
    val tableau: List<List<Card>>,
    val foundations: List<List<Card>>,
    val stock: List<Card>,
    val waste: List<Card>,
    val moves: Int,
    val score: Int,
    val isWon: Boolean = false,
    val isAutoCompleteAvailable: Boolean = false
)
