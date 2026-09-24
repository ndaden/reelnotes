package com.danstudios.reelnotes.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class IngredientItem(
    val name: String = "",
    val amount: String? = "",
    val unit: String? = "",
    var isChecked: Boolean = false
)

@Serializable
data class StepItem(
    val stepNumber: Int,
    val instruction: String,
    var isDone: Boolean = false
)

@Serializable
data class StructuredNoteData(
    val summary: String = "",
    val ingredients: List<IngredientItem> = emptyList(),
    val steps: List<StepItem> = emptyList(),
    val keyTakeaways: List<String> = emptyList(),
    val prepTime: String? = null,
    val cookTime: String? = null,
    val servings: String? = null,
    val tips: List<String> = emptyList()
)
