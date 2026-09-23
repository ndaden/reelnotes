package com.danstudios.reelnotes.data.network

import com.danstudios.reelnotes.domain.model.NoteCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiResponseParserTest {

    @Test
    fun `parseGeminiJsonOutput correctly parses structured recipe JSON response`() {
        val sampleAiJson = """
            {
                "category": "RECIPE",
                "title": "Tacos Birria Maison Fondants",
                "summary": "Recette complète des fameux tacos birria mexicains avec leur consommé riche et épicé.",
                "prepTime": "20 min",
                "cookTime": "2h30",
                "servings": "4 personnes",
                "ingredients": [
                    {"name": "bœuf à braiser (paleron)", "amount": "1", "unit": "kg"},
                    {"name": "piments ancho séchés", "amount": "3", "unit": ""},
                    {"name": "oignon blanc", "amount": "1", "unit": ""},
                    {"name": "tortillas de maïs", "amount": "12", "unit": ""},
                    {"name": "fromage Oaxaca ou mozzarella", "amount": "200", "unit": "g"}
                ],
                "steps": [
                    {"stepNumber": 1, "instruction": "Faire dorer la viande dans une cocotte en fonte."},
                    {"stepNumber": 2, "instruction": "Mixer les piments réhydratés avec l'oignon et les épices pour la marinade."},
                    {"stepNumber": 3, "instruction": "Laisser mijoter à feu doux pendant 2h30 jusqu'à ce que la viande s'effiloche."},
                    {"stepNumber": 4, "instruction": "Tremper les tortillas dans le bouillon et garnir de viande et fromage."}
                ],
                "keyTakeaways": [
                    "Garder le bouillon pour tremper les tacos à la dégustation"
                ],
                "tags": ["birria", "tacos", "mexicanfood"],
                "tips": [
                    "Préparez la viande la veille pour encore plus de saveur"
                ]
            }
        """.trimIndent()

        val parsed = GeminiSummarizer.parseAiJson(sampleAiJson)
        assertNotNull(parsed)
        assertEquals(NoteCategory.RECIPE, parsed?.categoryEnum)
        assertEquals("Tacos Birria Maison Fondants", parsed?.title)
        assertEquals(5, parsed?.ingredients?.size)
        assertEquals("1", parsed?.ingredients?.first()?.amount)
        assertEquals("kg", parsed?.ingredients?.first()?.unit)
        assertEquals(4, parsed?.steps?.size)
        assertEquals(3, parsed?.tags?.size)
        assertEquals("20 min", parsed?.prepTime)
    }

    @Test
    fun `parseGeminiJsonOutput handles markdown code block wrappers`() {
        val wrappedJson = """
            ```json
            {
                "category": "TIPS_INFO",
                "title": "5 Raccourcis Clavier Essentiels",
                "summary": "Gagnez du temps au quotidien avec ces raccourcis.",
                "ingredients": [],
                "steps": [],
                "keyTakeaways": [
                    "Ctrl+Shift+T : rouvrir un onglet fermé",
                    "Win+V : historique du presse-papier"
                ],
                "tags": ["productivite", "tech"]
            }
            ```
        """.trimIndent()

        val parsed = GeminiSummarizer.parseAiJson(wrappedJson)
        assertNotNull(parsed)
        assertEquals(NoteCategory.TIPS_INFO, parsed?.categoryEnum)
        assertEquals(2, parsed?.keyTakeaways?.size)
    }
}
