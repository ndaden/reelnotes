package com.danstudios.reelnotes.data.network

import com.danstudios.reelnotes.domain.model.NoteCategory
import org.json.JSONObject
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

    @Test
    fun `parseGeminiJsonOutput handles conversational prefix and suffix text`() {
        val conversationalOutput = """
            Voici les notes extraites du Reel Instagram :

            ```json
            {
                "category": "WORKOUT",
                "title": "Séance Abdo Express 10 Min",
                "summary": "Circuit d'entraînement abdominal à haute intensité sans matériel.",
                "ingredients": [],
                "steps": [
                    {"stepNumber": 1, "instruction": "Gainage planche 45 secondes"},
                    {"stepNumber": 2, "instruction": "Crunchs bicyclette 30 répétitions"}
                ],
                "keyTakeaways": ["Pas de temps de repos entre les exercices"],
                "tags": ["fitness", "abdos"]
            }
            ```

            N'hésitez pas si vous avez besoin d'autres résumés !
        """.trimIndent()

        val parsed = GeminiSummarizer.parseAiJson(conversationalOutput)
        assertNotNull(parsed)
        assertEquals(NoteCategory.WORKOUT, parsed?.categoryEnum)
        assertEquals("Séance Abdo Express 10 Min", parsed?.title)
        assertEquals(2, parsed?.steps?.size)
    }

    @Test
    fun `parseGeminiJsonOutput extracts JSON even without code block markers`() {
        val textWithRawJson = """
            Certainement, voici le résultat :
            {
                "category": "TUTORIAL",
                "title": "Nettoyer ses baskets blanches",
                "summary": "Méthode efficace au bicarbonate de soude et dentifrice.",
                "ingredients": [],
                "steps": [
                    {"stepNumber": 1, "instruction": "Frotter avec une vieille brosse à dents"}
                ],
                "keyTakeaways": ["Laisser sécher à l'air libre"],
                "tags": ["astuce", "mode"]
            }
            Espérant que cela vous aide.
        """.trimIndent()

        val parsed = GeminiSummarizer.parseAiJson(textWithRawJson)
        assertNotNull(parsed)
        assertEquals(NoteCategory.TUTORIAL, parsed?.categoryEnum)
        assertEquals("Nettoyer ses baskets blanches", parsed?.title)
    }
}
