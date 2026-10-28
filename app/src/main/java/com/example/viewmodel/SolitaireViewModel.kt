package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SolitairePreferencesRepository
import com.example.logic.SolitaireEngine
import com.example.model.Card
import com.example.model.CardPileLocation
import com.example.model.DealType
import com.example.model.DrawMode
import com.example.model.GameSettings
import com.example.model.GameStatistics
import com.example.model.GameStateSnapshot
import com.example.model.HintInfo
import com.example.model.LanguageDictionary
import com.example.model.Rank
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SolitaireUiState(
    val tableau: List<List<Card>> = List(7) { emptyList() },
    val foundations: List<List<Card>> = List(4) { emptyList() },
    val stock: List<Card> = emptyList(),
    val waste: List<Card> = emptyList(),
    val moves: Int = 0,
    val score: Int = 0,
    val timeSeconds: Long = 0L,
    val isGameActive: Boolean = false,
    val isWon: Boolean = false,
    val isAutoCompleteAvailable: Boolean = false,
    val isAutoCompleting: Boolean = false,
    val activeHint: HintInfo? = null,
    val undoCount: Int = 0,
    val redoCount: Int = 0,
    val settings: GameSettings = GameSettings(),
    val statistics: GameStatistics = GameStatistics(),
    val showSettingsDialog: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showNewGameDialog: Boolean = false,
    val toastMessage: String? = null
)

class SolitaireViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SolitairePreferencesRepository(application)

    private val _uiState = MutableStateFlow(SolitaireUiState())
    val uiState: StateFlow<SolitaireUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<GameStateSnapshot>()
    private val redoStack = mutableListOf<GameStateSnapshot>()
    private var timerJob: Job? = null
    private var autoCompleteJob: Job? = null
    private var initialDealSnapshot: SolitaireEngine.InitialDeal? = null

    private val _adTriggerCounter = MutableStateFlow(0L)
    val adTriggerCounter: StateFlow<Long> = _adTriggerCounter.asStateFlow()

    fun triggerAdEvent() {
        _adTriggerCounter.update { it + 1 }
    }

    init {
        val loadedSettings = repository.loadSettings()
        val loadedStats = repository.loadStatistics()
        _uiState.update { it.copy(settings = loadedSettings, statistics = loadedStats) }
        startNewGame()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.isGameActive && !_uiState.value.isWon && !_uiState.value.isAutoCompleting) {
                    _uiState.update { it.copy(timeSeconds = it.timeSeconds + 1) }
                }
            }
        }
    }

    private fun createSnapshot(): GameStateSnapshot {
        val state = _uiState.value
        return GameStateSnapshot(
            tableau = state.tableau.map { col -> col.map { it.copy() } },
            foundations = state.foundations.map { f -> f.map { it.copy() } },
            stock = state.stock.map { it.copy() },
            waste = state.waste.map { it.copy() },
            moves = state.moves,
            score = state.score,
            isWon = state.isWon,
            isAutoCompleteAvailable = state.isAutoCompleteAvailable
        )
    }

    private fun recordSnapshot() {
        undoStack.add(createSnapshot())
        redoStack.clear()
        _uiState.update {
            it.copy(
                undoCount = undoStack.size,
                redoCount = 0
            )
        }
    }

    fun startNewGame(dealType: DealType? = null) {
        if (_uiState.value.moves > 4) {
            triggerAdEvent()
        }
        autoCompleteJob?.cancel()
        val type = dealType ?: _uiState.value.settings.dealType
        val deal = SolitaireEngine.dealGame(type)
        initialDealSnapshot = deal
        undoStack.clear()
        redoStack.clear()

        _uiState.update { current ->
            current.copy(
                tableau = deal.tableau,
                stock = deal.stock,
                waste = deal.waste,
                foundations = deal.foundations,
                moves = 0,
                score = 0,
                timeSeconds = 0L,
                isGameActive = true,
                isWon = false,
                isAutoCompleteAvailable = false,
                isAutoCompleting = false,
                activeHint = null,
                undoCount = 0,
                redoCount = 0,
                showNewGameDialog = false,
                toastMessage = null
            )
        }
        startTimer()
    }

    fun restartCurrentGame() {
        val deal = initialDealSnapshot ?: return
        autoCompleteJob?.cancel()
        undoStack.clear()
        redoStack.clear()

        val strings = LanguageDictionary.getStrings(_uiState.value.settings.language)
        _uiState.update { current ->
            current.copy(
                tableau = deal.tableau.map { col -> col.map { it.copy() } },
                stock = deal.stock.map { it.copy() },
                waste = emptyList(),
                foundations = List(4) { emptyList() },
                moves = 0,
                score = 0,
                timeSeconds = 0L,
                isGameActive = true,
                isWon = false,
                isAutoCompleteAvailable = false,
                isAutoCompleting = false,
                activeHint = null,
                undoCount = 0,
                redoCount = 0,
                showNewGameDialog = false,
                toastMessage = strings.gameRestartedToast
            )
        }
        startTimer()
    }

    fun onStockClick() {
        _uiState.update { it.copy(activeHint = null) }
        val state = _uiState.value
        if (state.stock.isEmpty()) {
            if (state.waste.isEmpty()) return
            recordSnapshot()
            // Recycle waste back to stock
            val recycled = state.waste.reversed().map { it.copy(isFaceUp = false) }

            _uiState.update {
                it.copy(
                    stock = recycled,
                    waste = emptyList(),
                    moves = it.moves + 1,
                    undoCount = undoStack.size,
                    redoCount = redoStack.size
                )
            }
        } else {
            recordSnapshot()
            // Draw cards
            val count = minOf(state.settings.drawMode.count, state.stock.size)
            val drawn = state.stock.take(count).map { it.copy(isFaceUp = true) }
            val newStock = state.stock.drop(count)
            val newWaste = state.waste + drawn

            _uiState.update {
                it.copy(
                    stock = newStock,
                    waste = newWaste,
                    moves = it.moves + 1,
                    undoCount = undoStack.size,
                    redoCount = redoStack.size
                )
            }
        }
        checkAutoCompleteStatus()
    }

    fun onWasteCardClick(card: Card) {
        _uiState.update { it.copy(activeHint = null) }
        val state = _uiState.value
        val dest = SolitaireEngine.findSmartTapMove(
            location = CardPileLocation.Waste,
            card = card,
            stack = listOf(card),
            tableau = state.tableau,
            foundations = state.foundations
        ) ?: return

        if (!state.isAutoCompleting) {
            recordSnapshot()
        }

        when (dest) {
            is CardPileLocation.Foundation -> {
                val fIndex = dest.suitIndex
                val newFoundations = state.foundations.toMutableList()
                newFoundations[fIndex] = newFoundations[fIndex] + card
                val newWaste = state.waste.dropLast(1)
                val scoreDelta = 10

                _uiState.update {
                    it.copy(
                        waste = newWaste,
                        foundations = newFoundations,
                        moves = it.moves + 1,
                        score = it.score + scoreDelta,
                        undoCount = undoStack.size,
                        redoCount = redoStack.size
                    )
                }
                checkWinCondition()
            }
            is CardPileLocation.Tableau -> {
                val colIndex = dest.colIndex
                val newTableau = state.tableau.toMutableList().map { it.toMutableList() }
                newTableau[colIndex].add(card)
                val newWaste = state.waste.dropLast(1)
                val scoreDelta = 5

                _uiState.update {
                    it.copy(
                        waste = newWaste,
                        tableau = newTableau,
                        moves = it.moves + 1,
                        score = it.score + scoreDelta,
                        undoCount = undoStack.size,
                        redoCount = redoStack.size
                    )
                }
                checkAutoCompleteStatus()
            }
            else -> {}
        }
    }

    fun onTableauCardClick(colIndex: Int, cardIndex: Int, card: Card) {
        _uiState.update { it.copy(activeHint = null) }
        val state = _uiState.value
        val col = state.tableau[colIndex]
        if (cardIndex < 0 || cardIndex >= col.size) return
        val movingCards = col.subList(cardIndex, col.size)

        val dest = SolitaireEngine.findSmartTapMove(
            location = CardPileLocation.Tableau(colIndex, cardIndex),
            card = card,
            stack = movingCards,
            tableau = state.tableau,
            foundations = state.foundations
        ) ?: return

        if (!state.isAutoCompleting) {
            recordSnapshot()
        }

        when (dest) {
            is CardPileLocation.Foundation -> {
                if (movingCards.size != 1) return
                val fIndex = dest.suitIndex
                val newFoundations = state.foundations.toMutableList()
                newFoundations[fIndex] = newFoundations[fIndex] + card

                val newTableau = state.tableau.toMutableList().map { it.toMutableList() }
                newTableau[colIndex].removeAt(cardIndex)

                var extraScore = 0
                if (newTableau[colIndex].isNotEmpty() && !newTableau[colIndex].last().isFaceUp) {
                    val lastIdx = newTableau[colIndex].lastIndex
                    newTableau[colIndex][lastIdx] = newTableau[colIndex][lastIdx].copy(isFaceUp = true)
                    extraScore = 5
                }
                val scoreDelta = 10 + extraScore

                _uiState.update {
                    it.copy(
                        tableau = newTableau,
                        foundations = newFoundations,
                        moves = it.moves + 1,
                        score = it.score + scoreDelta,
                        undoCount = undoStack.size,
                        redoCount = redoStack.size
                    )
                }
                checkWinCondition()
                checkAutoCompleteStatus()
            }
            is CardPileLocation.Tableau -> {
                val targetCol = dest.colIndex
                val newTableau = state.tableau.toMutableList().map { it.toMutableList() }
                // Remove moving cards from source
                for (i in (col.size - 1) downTo cardIndex) {
                    newTableau[colIndex].removeAt(i)
                }
                // Add moving cards to target
                newTableau[targetCol].addAll(movingCards)

                var extraScore = 0
                if (newTableau[colIndex].isNotEmpty() && !newTableau[colIndex].last().isFaceUp) {
                    val lastIdx = newTableau[colIndex].lastIndex
                    newTableau[colIndex][lastIdx] = newTableau[colIndex][lastIdx].copy(isFaceUp = true)
                    extraScore = 5
                }

                _uiState.update {
                    it.copy(
                        tableau = newTableau,
                        moves = it.moves + 1,
                        score = it.score + extraScore,
                        undoCount = undoStack.size,
                        redoCount = redoStack.size
                    )
                }
                checkAutoCompleteStatus()
            }
            else -> {}
        }
    }

    fun onFoundationCardClick(topCard: Card) {
        // Option to move card back to tableau if valid
        _uiState.update { it.copy(activeHint = null) }
        val state = _uiState.value
        // Find which foundation
        val fIndex = state.foundations.indexOfFirst { it.lastOrNull() == topCard }
        if (fIndex < 0) return

        for (colIndex in state.tableau.indices) {
            if (SolitaireEngine.canDropOnTableau(topCard, state.tableau[colIndex])) {
                recordSnapshot()

                val newFoundations = state.foundations.toMutableList()
                newFoundations[fIndex] = newFoundations[fIndex].dropLast(1)
                val newTableau = state.tableau.toMutableList().map { it.toMutableList() }
                newTableau[colIndex].add(topCard)

                val scoreDelta = -15

                _uiState.update {
                    it.copy(
                        foundations = newFoundations,
                        tableau = newTableau,
                        moves = it.moves + 1,
                        score = maxOf(0, it.score + scoreDelta),
                        undoCount = undoStack.size,
                        redoCount = redoStack.size
                    )
                }
                break
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        autoCompleteJob?.cancel()
        _uiState.update { it.copy(activeHint = null, isAutoCompleting = false) }

        val currentSnapshot = createSnapshot()
        redoStack.add(currentSnapshot)

        val previous = undoStack.removeAt(undoStack.lastIndex)
        restoreSnapshot(previous)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        autoCompleteJob?.cancel()
        _uiState.update { it.copy(activeHint = null, isAutoCompleting = false) }

        val currentSnapshot = createSnapshot()
        undoStack.add(currentSnapshot)

        val next = redoStack.removeAt(redoStack.lastIndex)
        restoreSnapshot(next)
    }

    private fun restoreSnapshot(snapshot: GameStateSnapshot) {
        _uiState.update {
            it.copy(
                tableau = snapshot.tableau.map { col -> col.map { c -> c.copy() } },
                foundations = snapshot.foundations.map { f -> f.map { c -> c.copy() } },
                stock = snapshot.stock.map { c -> c.copy() },
                waste = snapshot.waste.map { c -> c.copy() },
                moves = snapshot.moves,
                score = snapshot.score,
                isWon = snapshot.isWon,
                isAutoCompleteAvailable = snapshot.isAutoCompleteAvailable,
                undoCount = undoStack.size,
                redoCount = redoStack.size
            )
        }
        checkAutoCompleteStatus()
    }

    fun requestHint() {
        val state = _uiState.value
        if (state.activeHint != null) {
            // Apply current hint directly!
            applyActiveHint(state.activeHint)
            return
        }

        val strings = LanguageDictionary.getStrings(state.settings.language)
        val hint = SolitaireEngine.findIntelligentHint(
            tableau = state.tableau,
            foundations = state.foundations,
            waste = state.waste,
            stock = state.stock,
            strings = strings
        )

        if (hint != null) {
            _uiState.update { it.copy(activeHint = hint, toastMessage = hint.message) }
        } else {
            _uiState.update { it.copy(toastMessage = strings.noMoreMovesToast) }
        }
    }

    private fun applyActiveHint(hint: HintInfo) {
        when (hint.from) {
            is CardPileLocation.Stock -> onStockClick()
            is CardPileLocation.Waste -> {
                if (hint.to is CardPileLocation.Stock) {
                    onStockClick()
                } else {
                    onWasteCardClick(hint.card)
                }
            }
            is CardPileLocation.Tableau -> {
                val col = hint.from.colIndex
                val cardIdx = hint.from.cardIndex
                onTableauCardClick(col, cardIdx, hint.card)
            }
            else -> {}
        }
    }

    fun toggleAutoComplete() {
        if (_uiState.value.isAutoCompleting) {
            stopAutoComplete()
        } else {
            executeAutoComplete()
        }
    }

    fun stopAutoComplete() {
        autoCompleteJob?.cancel()
        _uiState.update { it.copy(isAutoCompleting = false) }
    }

    fun executeAutoComplete() {
        if (_uiState.value.isAutoCompleting || _uiState.value.isWon) return
        recordSnapshot()
        _uiState.update { it.copy(isAutoCompleting = true, activeHint = null) }

        autoCompleteJob?.cancel()
        autoCompleteJob = viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                if (state.isWon) {
                    _uiState.update { it.copy(isAutoCompleting = false) }
                    break
                }

                val move = SolitaireEngine.findNextAutoCompleteMove(
                    tableau = state.tableau,
                    waste = state.waste,
                    foundations = state.foundations
                )

                if (move == null) {
                    _uiState.update { it.copy(isAutoCompleting = false) }
                    checkWinCondition()
                    break
                }

                delay(110)

                when (val from = move.from) {
                    is CardPileLocation.Waste -> {
                        onWasteCardClick(move.card)
                    }
                    is CardPileLocation.Tableau -> {
                        onTableauCardClick(from.colIndex, from.cardIndex, move.card)
                    }
                    else -> break
                }
            }
        }
    }

    private fun checkAutoCompleteStatus() {
        val state = _uiState.value
        val ready = SolitaireEngine.isAutoCompleteReady(
            tableau = state.tableau,
            stock = state.stock,
            waste = state.waste
        )
        _uiState.update { it.copy(isAutoCompleteAvailable = ready) }
    }

    private fun checkWinCondition() {
        val state = _uiState.value
        val totalInFoundations = state.foundations.sumOf { it.size }
        if (totalInFoundations == 52 && !state.isWon) {
            val updatedStats = repository.recordGameWon(
                timeSeconds = state.timeSeconds,
                moves = state.moves,
                score = state.score
            )
            _uiState.update {
                it.copy(
                    isWon = true,
                    isGameActive = false,
                    isAutoCompleting = false,
                    statistics = updatedStats
                )
            }
            // Trigger automatic AdMob interstitial on game victory
            triggerAdEvent()
        }
    }

    fun updateSettings(newSettings: GameSettings) {
        repository.saveSettings(newSettings)
        val shouldRestart = (newSettings.drawMode != _uiState.value.settings.drawMode) ||
                (newSettings.dealType != _uiState.value.settings.dealType)
        _uiState.update { it.copy(settings = newSettings) }
        if (shouldRestart) {
            startNewGame(newSettings.dealType)
        }
    }

    fun resetStatistics() {
        val strings = LanguageDictionary.getStrings(_uiState.value.settings.language)
        val reset = repository.resetStatistics()
        _uiState.update { it.copy(statistics = reset, toastMessage = strings.statsResetToast) }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setShowStatsDialog(show: Boolean) {
        _uiState.update { it.copy(showStatsDialog = show) }
    }

    fun setShowNewGameDialog(show: Boolean) {
        _uiState.update { it.copy(showNewGameDialog = show) }
    }

    fun triggerVictoryCelebration() {
        _uiState.update {
            it.copy(
                isWon = true,
                isGameActive = false,
                isAutoCompleting = false,
                showSettingsDialog = false
            )
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
