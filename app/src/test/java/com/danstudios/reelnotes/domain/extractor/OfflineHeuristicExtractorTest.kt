package com.danstudios.reelnotes.domain.extractor

import com.danstudios.reelnotes.domain.model.NoteCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineHeuristicExtractorTest {

    @Test
    fun `extract correctly parses French recipe with ingredients and steps`() {
        val caption = """
            🍝 Pâtes crémeuses à l'ail et parmesan express (15 min) !
            
            Ingrédients :
            - 250g de tagliatelles
            - 3 gousses d'ail émincées
            - 150ml de crème fraîche
            - 60g de parmesan râpé
            - 1 c. à soupe d'huile d'olive
            - Sel et poivre
            
            Étapes :
            1. Cuire les pâtes dans une grande casserole d'eau bouillante salée.
            2. Dans une poêle, faire dorer l'ail dans l'huile d'olive à feu doux.
            3. Verser la crème et le parmesan, mélanger jusqu'à obtenir une sauce onctueuse.
            4. Égoutter les pâtes et les enrober dans la sauce. Servir chaud avec du basilic frais.
            
            #recette #pasta #cuisinefacile
        """.trimIndent()

        val result = OfflineHeuristicExtractor.extract(
            caption = caption,
            reelUrl = "https://www.instagram.com/reel/DDh2O36IEyL/",
            shortcode = "DDh2O36IEyL",
            author = "@chefnico"
        )

        assertEquals(NoteCategory.RECIPE, result.category)
        assertTrue(result.title.contains("Pâtes crémeuses", ignoreCase = true))
        assertEquals("@chefnico", result.author)

        // Ingredients
        val ingredients = result.structuredData.ingredients
        assertTrue("Should have extracted at least 4 ingredients", ingredients.size >= 4)
        assertTrue(ingredients.any { it.name.contains("tagliatelles", ignoreCase = true) && it.amount == "250" && it.unit == "g" })
        assertTrue(ingredients.any { it.name.contains("ail", ignoreCase = true) })

        // Steps
        val steps = result.structuredData.steps
        assertEquals(4, steps.size)
        assertEquals(1, steps[0].stepNumber)
        assertTrue(steps[0].instruction.contains("Cuire les pâtes"))

        // Tags
        assertTrue(result.tags.contains("recette"))
        assertTrue(result.tags.contains("pasta"))

        // Markdown content
        assertTrue(result.markdownContent.contains("## Ingrédients"))
        assertTrue(result.markdownContent.contains("## Préparation"))
    }

    @Test
    fun `extract correctly parses English recipe with cups and tbsp`() {
        val caption = """
            Crispy Air Fryer Chicken Bites 🍗
            
            Ingredients:
            - 2 chicken breasts (diced)
            - 2 tbsp olive oil
            - 1 tsp garlic powder
            - 1 tsp paprika
            - 1/2 cup grated parmesan
            
            Instructions:
            1. Toss chicken pieces with olive oil and spices.
            2. Coat each piece evenly in parmesan.
            3. Air fry at 200C for 12 minutes shaking halfway.
            
            #airfryer #quickrecipes
        """.trimIndent()

        val result = OfflineHeuristicExtractor.extract(
            caption = caption,
            reelUrl = "https://www.instagram.com/reel/XYZ123/",
            shortcode = "XYZ123"
        )

        assertEquals(NoteCategory.RECIPE, result.category)
        assertEquals(5, result.structuredData.ingredients.size)
        assertEquals(3, result.structuredData.steps.size)
        assertTrue(result.tags.contains("airfryer"))
    }

    @Test
    fun `extract detects workout category and exercises`() {
        val caption = """
            Full Upper Body Blast 💪
            
            Warmup 5 minutes.
            Exercises:
            1. Bench Press: 4 sets x 10 reps
            2. Dumbbell Shoulder Press: 3 sets x 12 reps
            3. Pull Ups: 3 sets to failure
            4. Bicep Curls: 3 sets x 15 reps
            
            Rest 90 seconds between sets. Good luck! #fitness #workout
        """.trimIndent()

        val result = OfflineHeuristicExtractor.extract(
            caption = caption,
            reelUrl = "https://www.instagram.com/reel/FIT123/",
            shortcode = "FIT123"
        )

        assertEquals(NoteCategory.WORKOUT, result.category)
        assertTrue(result.structuredData.steps.isNotEmpty() || result.structuredData.keyTakeaways.isNotEmpty())
        assertTrue(result.tags.contains("fitness"))
    }

    @Test
    fun `extract detects tips category with bullet points`() {
        val caption = """
            3 astuces secrètes pour booster votre batterie Android ⚡
            
            • Désactivez la recherche WiFi et Bluetooth en arrière-plan
            • Activez le mode sombre pour les écrans AMOLED
            • Limitez les applications en veille profonde
            
            Enregistre ce reel pour plus tard ! #android #astuces #tech
        """.trimIndent()

        val result = OfflineHeuristicExtractor.extract(
            caption = caption,
            reelUrl = "https://www.instagram.com/reel/TECH123/",
            shortcode = "TECH123"
        )

        assertEquals(NoteCategory.TIPS_INFO, result.category)
        assertTrue(result.structuredData.keyTakeaways.size >= 3)
        assertTrue(result.tags.contains("tech"))
    }
}
