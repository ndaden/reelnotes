package com.danstudios.reelnotes.domain.extractor

import com.danstudios.reelnotes.domain.model.IngredientItem
import com.danstudios.reelnotes.domain.model.NoteCategory
import com.danstudios.reelnotes.domain.model.ReelNote
import com.danstudios.reelnotes.domain.model.StepItem
import com.danstudios.reelnotes.domain.model.StructuredNoteData

object OfflineHeuristicExtractor {

    private val HASHTAG_REGEX = Regex("""#([A-Za-z0-9_]+)""")

    private val RECIPE_KEYWORDS = listOf(
        "recette", "recipe", "ingrédient", "ingredient", "cuisson", "cuire", "bake", "cook",
        "cuillère", "tbsp", "tsp", "gramme", "g de", "ml", "cup", "tasse", "farine", "pasta",
        "pâtes", "sauce", "airfryer", "poêle", "casserole", "four", "four à", "four préchauffé"
    )

    private val WORKOUT_KEYWORDS = listOf(
        "workout", "fitness", "exercice", "exercise", "sets", "reps", "séries", "répétitions",
        "squat", "pompe", "bench", "haltère", "muscu", "musculation", "abdos", "cardio", "training",
        "repos", "rest"
    )

    private val TIPS_KEYWORDS = listOf(
        "astuce", "astuces", "tips", "hack", "hacks", "conseil", "conseils", "secret", "secrets",
        "savais-tu", "did you know", "booster", "optimiser", "saviez-vous"
    )

    private val TUTORIAL_KEYWORDS = listOf(
        "tuto", "tutorial", "diy", "how to", "comment faire", "guide", "étapes", "fabrication"
    )

    private val TRAVEL_KEYWORDS = listOf(
        "voyage", "travel", "hôtel", "hotel", "destination", "visiter", "explore", "trip", "vacances"
    )

    private val PRODUCT_KEYWORDS = listOf(
        "unboxing", "review", "revue", "avis", "test", "produit", "code promo", "haul"
    )

    // Regex to match: optional bullet (- or • or *), amount (e.g. 250, 1/2, 1.5), unit (g, ml, tbsp...), and ingredient name
    private val INGREDIENT_PATTERN = Regex(
        """^(?:[-*•\d.)]\s*)?(\d+(?:[.,/]\d+)?)\s*(g|kg|ml|cl|l|cuillères?|c\.?\s*à\s*(?:soupe|café)|tbsp|tsp|cups?|tasses?|tranches?|gousses?|pincées?|boîtes?)?\s*(?:de\s+|d'|of\s+)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    // Regex to match numbered steps: 1. or 1) or Étape 1: or Step 1:
    private val STEP_PATTERN = Regex(
        """^(?:(?:Étape|Step)\s*\d+[\s:.-]*|\d+[\s.)-]+)\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    fun extract(
        caption: String,
        reelUrl: String,
        shortcode: String,
        author: String? = null,
        thumbnailUrl: String? = null
    ): ReelNote {
        val lines = caption.lines().map { it.trim() }.filter { it.isNotBlank() }
        val lowerCaption = caption.lowercase()

        // 1. Extract hashtags
        val tags = HASHTAG_REGEX.findAll(caption)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .toList()

        // 2. Determine Category
        val category = detectCategory(lowerCaption, tags)

        // 3. Extract Title
        val title = extractTitle(lines, shortcode, category)

        // 4. Extract Structured Data (Ingredients, Steps, Takeaways)
        val structuredData = when (category) {
            NoteCategory.RECIPE -> parseRecipeData(lines, lowerCaption)
            NoteCategory.WORKOUT -> parseWorkoutData(lines)
            NoteCategory.TUTORIAL -> parseTutorialData(lines)
            NoteCategory.TIPS_INFO -> parseTipsData(lines)
            else -> parseGeneralData(lines)
        }

        // 5. Generate Markdown Representation
        val markdown = buildMarkdown(title, author, reelUrl, category, structuredData, tags)

        val summary = structuredData.summary.ifBlank {
            lines.firstOrNull { !it.startsWith("#") && it.length > 20 } ?: title
        }

        return ReelNote(
            reelUrl = reelUrl,
            shortcode = shortcode,
            author = author,
            title = title,
            category = category,
            summary = summary,
            rawCaption = caption,
            markdownContent = markdown,
            structuredData = structuredData,
            thumbnailUrl = thumbnailUrl,
            tags = tags
        )
    }

    private fun detectCategory(lowerText: String, tags: List<String>): NoteCategory {
        val allTokens = tags + lowerText.split(Regex("""\s+"""))

        fun score(keywords: List<String>): Int {
            return keywords.count { kw -> lowerText.contains(kw) }
        }

        val recipeScore = score(RECIPE_KEYWORDS)
        val workoutScore = score(WORKOUT_KEYWORDS)
        val tipsScore = score(TIPS_KEYWORDS)
        val tutorialScore = score(TUTORIAL_KEYWORDS)
        val travelScore = score(TRAVEL_KEYWORDS)
        val productScore = score(PRODUCT_KEYWORDS)

        return when {
            recipeScore >= 2 || (recipeScore >= 1 && (lowerText.contains("ingrédient") || lowerText.contains("ingredient"))) -> NoteCategory.RECIPE
            workoutScore >= 2 || (workoutScore >= 1 && (lowerText.contains("reps") || lowerText.contains("séries"))) -> NoteCategory.WORKOUT
            tipsScore >= 2 -> NoteCategory.TIPS_INFO
            tutorialScore >= 2 -> NoteCategory.TUTORIAL
            travelScore >= 2 -> NoteCategory.TRAVEL
            productScore >= 2 -> NoteCategory.PRODUCT
            recipeScore == 1 -> NoteCategory.RECIPE
            workoutScore == 1 -> NoteCategory.WORKOUT
            tipsScore == 1 -> NoteCategory.TIPS_INFO
            tutorialScore == 1 -> NoteCategory.TUTORIAL
            else -> NoteCategory.GENERAL
        }
    }

    private fun extractTitle(lines: List<String>, shortcode: String, category: NoteCategory): String {
        for (line in lines) {
            val clean = line.replace(Regex("""^[^\p{L}\p{N}]+"""), "").trim()
            if (clean.isNotBlank() && !clean.startsWith("#") && clean.length > 3) {
                return clean.take(100)
            }
        }
        return "${category.label} - Reel $shortcode"
    }

    private fun parseRecipeData(lines: List<String>, lowerCaption: String): StructuredNoteData {
        val ingredients = mutableListOf<IngredientItem>()
        val steps = mutableListOf<StepItem>()
        val tips = mutableListOf<String>()

        var inIngredients = false
        var inSteps = false

        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("ingrédient") || lower.contains("ingredients")) {
                inIngredients = true
                inSteps = false
                continue
            }
            if (lower.contains("étape") || lower.contains("instruction") || lower.contains("préparation") || lower.contains("preparation") || lower.contains("recette :")) {
                inSteps = true
                inIngredients = false
                continue
            }
            if (line.startsWith("#")) {
                inIngredients = false
                inSteps = false
                continue
            }

            if (inIngredients) {
                val match = INGREDIENT_PATTERN.find(line)
                if (match != null) {
                    val amount = match.groupValues[1]
                    val unit = match.groupValues[2].trim()
                    val name = match.groupValues[3].trim()
                    if (name.isNotBlank()) {
                        ingredients.add(IngredientItem(name = name, amount = amount, unit = unit))
                    }
                } else if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                    val clean = line.trimStart('-', '•', '*', ' ')
                    if (clean.isNotBlank()) {
                        ingredients.add(IngredientItem(name = clean))
                    }
                }
            } else if (inSteps) {
                val stepMatch = STEP_PATTERN.find(line)
                if (stepMatch != null) {
                    val instruction = stepMatch.groupValues[1].trim()
                    steps.add(StepItem(stepNumber = steps.size + 1, instruction = instruction))
                } else if (line.startsWith("-") || line.startsWith("•")) {
                    val instruction = line.trimStart('-', '•', ' ')
                    steps.add(StepItem(stepNumber = steps.size + 1, instruction = instruction))
                }
            } else {
                // If not explicitly inside a section header yet, check if line matches ingredient or step
                val stepMatch = STEP_PATTERN.find(line)
                if (stepMatch != null) {
                    steps.add(StepItem(stepNumber = steps.size + 1, instruction = stepMatch.groupValues[1].trim()))
                }
            }
        }

        // Fallback: If sections were not explicitly marked with headers
        if (ingredients.isEmpty()) {
            for (line in lines) {
                if (line.startsWith("#")) continue
                val match = INGREDIENT_PATTERN.find(line)
                if (match != null && (match.groupValues[2].isNotBlank() || line.startsWith("-") || line.startsWith("•"))) {
                    ingredients.add(
                        IngredientItem(
                            name = match.groupValues[3].trim(),
                            amount = match.groupValues[1],
                            unit = match.groupValues[2].trim()
                        )
                    )
                }
            }
        }

        return StructuredNoteData(
            summary = "Recette extraite du Reel",
            ingredients = ingredients,
            steps = steps,
            tips = tips
        )
    }

    private fun parseWorkoutData(lines: List<String>): StructuredNoteData {
        val steps = mutableListOf<StepItem>()
        val takeaways = mutableListOf<String>()

        for (line in lines) {
            if (line.startsWith("#")) continue
            val stepMatch = STEP_PATTERN.find(line)
            if (stepMatch != null) {
                steps.add(StepItem(stepNumber = steps.size + 1, instruction = stepMatch.groupValues[1].trim()))
            } else if (line.contains("x") || line.contains("sets") || line.contains("reps") || line.contains("séries")) {
                takeaways.add(line.trimStart('-', '•', '*').trim())
            }
        }

        return StructuredNoteData(
            summary = "Entraînement / Exercices du Reel",
            steps = steps,
            keyTakeaways = takeaways
        )
    }

    private fun parseTipsData(lines: List<String>): StructuredNoteData {
        val takeaways = mutableListOf<String>()

        for (line in lines) {
            if (line.startsWith("#")) continue
            if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                val clean = line.trimStart('•', '-', '*', ' ').trim()
                if (clean.length > 5) {
                    takeaways.add(clean)
                }
            } else {
                val stepMatch = STEP_PATTERN.find(line)
                if (stepMatch != null) {
                    takeaways.add(stepMatch.groupValues[1].trim())
                }
            }
        }

        return StructuredNoteData(
            summary = "Points clés et astuces",
            keyTakeaways = takeaways
        )
    }

    private fun parseTutorialData(lines: List<String>): StructuredNoteData {
        val steps = mutableListOf<StepItem>()
        val takeaways = mutableListOf<String>()

        for (line in lines) {
            if (line.startsWith("#")) continue
            val stepMatch = STEP_PATTERN.find(line)
            if (stepMatch != null) {
                steps.add(StepItem(stepNumber = steps.size + 1, instruction = stepMatch.groupValues[1].trim()))
            } else if (line.startsWith("-") || line.startsWith("•")) {
                takeaways.add(line.trimStart('-', '•', ' ').trim())
            }
        }

        return StructuredNoteData(
            summary = "Guide étape par étape",
            steps = steps,
            keyTakeaways = takeaways
        )
    }

    private fun parseGeneralData(lines: List<String>): StructuredNoteData {
        val takeaways = lines.filter {
            !it.startsWith("#") && (it.startsWith("-") || it.startsWith("•") || it.startsWith("*"))
        }.map { it.trimStart('-', '•', '*', ' ').trim() }

        return StructuredNoteData(
            summary = lines.firstOrNull { !it.startsWith("#") } ?: "",
            keyTakeaways = takeaways
        )
    }

    private fun buildMarkdown(
        title: String,
        author: String?,
        reelUrl: String,
        category: NoteCategory,
        data: StructuredNoteData,
        tags: List<String>
    ): String {
        val sb = StringBuilder()
        sb.append("# $title\n\n")
        if (!author.isNullOrBlank()) {
            sb.append("**Auteur :** $author  \n")
        }
        sb.append("**Catégorie :** ${category.iconEmoji} ${category.label}  \n")
        sb.append("**Lien Reel :** [Voir sur Instagram]($reelUrl)\n\n")

        if (data.summary.isNotBlank()) {
            sb.append("### Résumé\n${data.summary}\n\n")
        }

        if (data.ingredients.isNotEmpty()) {
            sb.append("## Ingrédients\n")
            for (ing in data.ingredients) {
                val qty = listOf(ing.amount, ing.unit).filter { it.isNotBlank() }.joinToString(" ")
                if (qty.isNotBlank()) {
                    sb.append("- [ ] **$qty** ${ing.name}\n")
                } else {
                    sb.append("- [ ] ${ing.name}\n")
                }
            }
            sb.append("\n")
        }

        if (data.steps.isNotEmpty()) {
            sb.append("## Préparation / Étapes\n")
            for (s in data.steps) {
                sb.append("${s.stepNumber}. ${s.instruction}\n")
            }
            sb.append("\n")
        }

        if (data.keyTakeaways.isNotEmpty()) {
            sb.append("## Points clés\n")
            for (pt in data.keyTakeaways) {
                sb.append("- $pt\n")
            }
            sb.append("\n")
        }

        if (tags.isNotEmpty()) {
            sb.append("### Tags\n")
            sb.append(tags.joinToString(" ") { "#$it" })
            sb.append("\n")
        }

        return sb.toString().trim()
    }
}
