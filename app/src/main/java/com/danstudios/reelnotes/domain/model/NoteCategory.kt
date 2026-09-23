package com.danstudios.reelnotes.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NoteCategory(val label: String, val iconEmoji: String) {
    RECIPE("Recette", "🍳"),
    TUTORIAL("Tutoriel", "🛠️"),
    WORKOUT("Sport & Fitness", "💪"),
    TIPS_INFO("Astuce & Info", "💡"),
    TRAVEL("Voyage & Lieux", "✈️"),
    PRODUCT("Produit & Avis", "🛍️"),
    GENERAL("Général", "📝");

    companion object {
        fun fromString(value: String?): NoteCategory {
            if (value.isNullOrBlank()) return GENERAL
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GENERAL
        }
    }
}
