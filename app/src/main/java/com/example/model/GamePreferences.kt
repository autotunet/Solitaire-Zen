package com.example.model

import androidx.compose.ui.graphics.Color

enum class DrawMode(val count: Int, val label: String, val subtitle: String) {
    DRAW_ONE(1, "Robo de 1 carta", "Fácil e ideal para principiantes"),
    DRAW_THREE(3, "Robo de 3 cartas", "Modo clásico reglamentario")
}

enum class DealType(val label: String, val subtitle: String) {
    WINNABLE("Partida Soluble", "Garantiza solución matemática ganadora"),
    RANDOM("Partida Aleatoria", "Barajado tradicional completamente azaroso")
}

enum class TableTheme(
    val id: String,
    val displayName: String,
    val primaryFeltColor: Color,
    val secondaryFeltColor: Color,
    val slotBorderColor: Color,
    val slotBgColor: Color,
    val isDark: Boolean
) {
    CLASSIC_GREEN(
        id = "classic_green",
        displayName = "Tapete Casino Verde",
        primaryFeltColor = Color(0xFF0F3822),
        secondaryFeltColor = Color(0xFF092014),
        slotBorderColor = Color(0x33FFFFFF),
        slotBgColor = Color(0x1A000000),
        isDark = true
    ),
    MIDNIGHT_ONYX(
        id = "midnight_onyx",
        displayName = "Noche Ónix Minimalista",
        primaryFeltColor = Color(0xFF131720),
        secondaryFeltColor = Color(0xFF0A0D12),
        slotBorderColor = Color(0x4038BDF8),
        slotBgColor = Color(0x221E293B),
        isDark = true
    ),
    ROYAL_BURGUNDY(
        id = "royal_burgundy",
        displayName = "Terciopelo Burdeos",
        primaryFeltColor = Color(0xFF38101C),
        secondaryFeltColor = Color(0xFF220710),
        slotBorderColor = Color(0x40F59E0B),
        slotBgColor = Color(0x223B0715),
        isDark = true
    ),
    DEEP_OCEAN(
        id = "deep_ocean",
        displayName = "Azul Zafiro Marino",
        primaryFeltColor = Color(0xFF0E2540),
        secondaryFeltColor = Color(0xFF061324),
        slotBorderColor = Color(0x4060A5FA),
        slotBgColor = Color(0x220F172A),
        isDark = true
    ),
    MODERN_SLATE(
        id = "modern_slate",
        displayName = "Grafito Platino",
        primaryFeltColor = Color(0xFF242730),
        secondaryFeltColor = Color(0xFF16181E),
        slotBorderColor = Color(0x40E2E8F0),
        slotBgColor = Color(0x20334155),
        isDark = true
    )
}

enum class CardBackTheme(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val accentColor: Color,
    val patternType: String
) {
    ROYAL_GOLD(
        id = "royal_gold",
        displayName = "Dorado Real",
        primaryColor = Color(0xFF1E293B),
        accentColor = Color(0xFFEAB308),
        patternType = "geometric"
    ),
    CLASSIC_NAVY(
        id = "classic_navy",
        displayName = "Clásico Azul",
        primaryColor = Color(0xFF1E3A8A),
        accentColor = Color(0xFF93C5FD),
        patternType = "diamonds"
    ),
    CRIMSON_VELVET(
        id = "crimson_velvet",
        displayName = "Carmesí Imperial",
        primaryColor = Color(0xFF881337),
        accentColor = Color(0xFFFDE047),
        patternType = "ornate"
    ),
    EMERALD_NATURE(
        id = "emerald_nature",
        displayName = "Esmeralda Nórdica",
        primaryColor = Color(0xFF064E3B),
        accentColor = Color(0xFF6EE7B7),
        patternType = "lattice"
    ),
    CYBER_VIOLET(
        id = "cyber_violet",
        displayName = "Cyberpunk Neón",
        primaryColor = Color(0xFF3B0764),
        accentColor = Color(0xFFF43F5E),
        patternType = "neon"
    )
}

data class GameSettings(
    val language: AppLanguage = AppLanguage.ES,
    val drawMode: DrawMode = DrawMode.DRAW_ONE,
    val dealType: DealType = DealType.WINNABLE,
    val tableTheme: TableTheme = TableTheme.CLASSIC_GREEN,
    val cardBackTheme: CardBackTheme = CardBackTheme.ROYAL_GOLD,
    val isDarkMode: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val autoPlayToFoundation: Boolean = true
)

data class GameStatistics(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val bestScore: Int = 0,
    val fewestMoves: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
) {
    val winRate: Float get() = if (gamesPlayed > 0) (gamesWon.toFloat() / gamesPlayed) * 100f else 0f
}
