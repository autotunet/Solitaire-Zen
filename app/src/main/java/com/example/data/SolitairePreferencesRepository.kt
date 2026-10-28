package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppLanguage
import com.example.model.CardBackTheme
import com.example.model.DealType
import com.example.model.DrawMode
import com.example.model.GameSettings
import com.example.model.GameStatistics
import com.example.model.TableTheme

class SolitairePreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("solitaire_modern_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): GameSettings {
        val langName = prefs.getString("app_language", AppLanguage.ES.name) ?: AppLanguage.ES.name
        val drawModeName = prefs.getString("draw_mode", DrawMode.DRAW_ONE.name) ?: DrawMode.DRAW_ONE.name
        val dealTypeName = prefs.getString("deal_type", DealType.WINNABLE.name) ?: DealType.WINNABLE.name
        val tableThemeId = prefs.getString("table_theme", TableTheme.CLASSIC_GREEN.id) ?: TableTheme.CLASSIC_GREEN.id
        val cardBackId = prefs.getString("card_back", CardBackTheme.ROYAL_GOLD.id) ?: CardBackTheme.ROYAL_GOLD.id
        val isDarkMode = prefs.getBoolean("is_dark_mode", true)
        val haptics = prefs.getBoolean("haptic_feedback", true)
        val autoPlay = prefs.getBoolean("auto_play", true)

        val language = try { AppLanguage.valueOf(langName) } catch (e: Exception) { AppLanguage.ES }
        val drawMode = try { DrawMode.valueOf(drawModeName) } catch (e: Exception) { DrawMode.DRAW_ONE }
        val dealType = try { DealType.valueOf(dealTypeName) } catch (e: Exception) { DealType.WINNABLE }
        val tableTheme = TableTheme.values().find { it.id == tableThemeId } ?: TableTheme.CLASSIC_GREEN
        val cardBack = CardBackTheme.values().find { it.id == cardBackId } ?: CardBackTheme.ROYAL_GOLD

        return GameSettings(
            language = language,
            drawMode = drawMode,
            dealType = dealType,
            tableTheme = tableTheme,
            cardBackTheme = cardBack,
            isDarkMode = isDarkMode,
            hapticFeedbackEnabled = haptics,
            autoPlayToFoundation = autoPlay
        )
    }

    fun saveSettings(settings: GameSettings) {
        prefs.edit()
            .putString("app_language", settings.language.name)
            .putString("draw_mode", settings.drawMode.name)
            .putString("deal_type", settings.dealType.name)
            .putString("table_theme", settings.tableTheme.id)
            .putString("card_back", settings.cardBackTheme.id)
            .putBoolean("is_dark_mode", settings.isDarkMode)
            .putBoolean("haptic_feedback", settings.hapticFeedbackEnabled)
            .putBoolean("auto_play", settings.autoPlayToFoundation)
            .apply()
    }

    fun loadStatistics(): GameStatistics {
        return GameStatistics(
            gamesPlayed = prefs.getInt("stat_games_played", 0),
            gamesWon = prefs.getInt("stat_games_won", 0),
            bestTimeSeconds = prefs.getLong("stat_best_time", 0L),
            bestScore = prefs.getInt("stat_best_score", 0),
            fewestMoves = prefs.getInt("stat_fewest_moves", 0),
            currentStreak = prefs.getInt("stat_current_streak", 0),
            bestStreak = prefs.getInt("stat_best_streak", 0)
        )
    }

    fun recordGameWon(timeSeconds: Long, moves: Int, score: Int): GameStatistics {
        val current = loadStatistics()
        val newPlayed = current.gamesPlayed + 1
        val newWon = current.gamesWon + 1
        val newStreak = current.currentStreak + 1
        val newBestStreak = maxOf(current.bestStreak, newStreak)
        val newBestTime = if (current.bestTimeSeconds == 0L) timeSeconds else minOf(current.bestTimeSeconds, timeSeconds)
        val newBestScore = maxOf(current.bestScore, score)
        val newFewestMoves = if (current.fewestMoves == 0) moves else minOf(current.fewestMoves, moves)

        val updated = GameStatistics(
            gamesPlayed = newPlayed,
            gamesWon = newWon,
            bestTimeSeconds = newBestTime,
            bestScore = newBestScore,
            fewestMoves = newFewestMoves,
            currentStreak = newStreak,
            bestStreak = newBestStreak
        )

        prefs.edit()
            .putInt("stat_games_played", updated.gamesPlayed)
            .putInt("stat_games_won", updated.gamesWon)
            .putLong("stat_best_time", updated.bestTimeSeconds)
            .putInt("stat_best_score", updated.bestScore)
            .putInt("stat_fewest_moves", updated.fewestMoves)
            .putInt("stat_current_streak", updated.currentStreak)
            .putInt("stat_best_streak", updated.bestStreak)
            .apply()

        return updated
    }

    fun recordGameAbandoned() {
        val current = loadStatistics()
        prefs.edit()
            .putInt("stat_games_played", current.gamesPlayed + 1)
            .putInt("stat_current_streak", 0)
            .apply()
    }

    fun resetStatistics(): GameStatistics {
        prefs.edit()
            .remove("stat_games_played")
            .remove("stat_games_won")
            .remove("stat_best_time")
            .remove("stat_best_score")
            .remove("stat_fewest_moves")
            .remove("stat_current_streak")
            .remove("stat_best_streak")
            .apply()
        return GameStatistics()
    }
}
