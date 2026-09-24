package com.danstudios.reelnotes.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionSanitizerTest {

    @Test
    fun `sanitize extracts target reel caption from Note 20 multi-reel feed text`() {
        val rawFeedText = """
            For you
            linstant_tesla
            Follow
            Tesla : les 5 trucs les plus cool sur ma tesla model 3 #model3 #tesla #voiture #electrique
            … more
            800
            47
            arnaud_tesla
            Follow
            Model 3 Standard ou Premium ?
            … more
            1,200
            85
        """.trimIndent()

        val sanitized = CaptionSanitizer.sanitize(rawFeedText, author = "@linstant_tesla")

        assertEquals(
            "Tesla : les 5 trucs les plus cool sur ma tesla model 3 #model3 #tesla #voiture #electrique",
            sanitized
        )
        assertFalse(sanitized.contains("arnaud_tesla"))
        assertFalse(sanitized.contains("Model 3 Standard ou Premium"))
        assertFalse(sanitized.contains("Follow"))
        assertFalse(sanitized.contains("800"))
    }

    @Test
    fun `sanitize extracts target reel caption from Note 18 multi-reel feed text`() {
        val rawFeedText = """
            For you
            ikrcook
            Lyon, France
            Follow
            💢 MUFFIN GOURMAND 💢
            … more
            6,807
            67
            swiss.fitcook
            Follow
            Crousty Tenders Ail & Parmesan 🧄🧀🍃
            … more
            11.1K
            96
            adelfugazi
            Follow
        """.trimIndent()

        val sanitized = CaptionSanitizer.sanitize(rawFeedText, author = "@ikrcook")

        assertEquals("💢 MUFFIN GOURMAND 💢", sanitized)
        assertFalse(sanitized.contains("swiss.fitcook"))
        assertFalse(sanitized.contains("Crousty Tenders"))
        assertFalse(sanitized.contains("adelfugazi"))
    }

    @Test
    fun `extractFirstReelCaptionFromFeedText correctly extracts short caption without hashtags`() {
        val rawFeedText = """
            For you
            ikrcook
            Lyon, France
            Follow
            💢 MUFFIN GOURMAND 💢
            … more
            6,807
            67
            swiss.fitcook
            Follow
            Crousty Tenders Ail & Parmesan 🧄🧀🍃
            … more
            11.1K
            96
        """.trimIndent()

        val extracted = CaptionSanitizer.extractFirstReelCaptionFromFeedText(rawFeedText)
        assertEquals("💢 MUFFIN GOURMAND 💢", extracted)
    }

    @Test
    fun `extractFirstReelCaptionFromFeedText correctly extracts Tesla reel caption`() {
        val rawFeedText = """
            For you
            linstant_tesla
            Follow
            Tesla : les 5 trucs les plus cool sur ma tesla model 3 #model3 #tesla #voiture #electrique
            … more
            800
            47
            arnaud_tesla
            Follow
            Model 3 Standard ou Premium ?
            … more
        """.trimIndent()

        val extracted = CaptionSanitizer.extractFirstReelCaptionFromFeedText(rawFeedText)
        assertEquals(
            "Tesla : les 5 trucs les plus cool sur ma tesla model 3 #model3 #tesla #voiture #electrique",
            extracted
        )
    }

    @Test
    fun `sanitize preserves full multiline recipe caption without feed leakage`() {
        val recipeText = """
            Délicieux Cookies Moelleux aux pépites de chocolat 🍪
            
            Ingrédients :
            - 200g de farine
            - 100g de sucre roux
            - 1 œuf
            - 100g de pépites de chocolat
            
            Préparation :
            1. Mélanger le sucre et le beurre.
            2. Ajouter l'œuf puis la farine.
            3. Cuire 10 min à 180°C.
            
            Régalez-vous ! #cookies #recette #patisserie
        """.trimIndent()

        val sanitized = CaptionSanitizer.sanitize(recipeText)
        assertEquals(recipeText.trim(), sanitized)
    }

    @Test
    fun `sanitize handles empty or blank strings`() {
        assertEquals("", CaptionSanitizer.sanitize(""))
        assertEquals("", CaptionSanitizer.sanitize("   \n\t  "))
    }
}
