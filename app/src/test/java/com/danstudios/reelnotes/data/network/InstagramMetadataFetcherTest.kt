package com.danstudios.reelnotes.data.network

import com.danstudios.reelnotes.domain.extractor.InstagramRestrictedException
import com.danstudios.reelnotes.domain.model.NoteCategory
import org.junit.Assert.*
import org.junit.Test

class InstagramMetadataFetcherTest {

    @Test
    fun `parseHtml extracts caption, author and thumbnail from embed HTML`() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Recette de saison par @chef_mario</title>
                <meta property="og:image" content="https://instagram.cdn/thumb123.jpg" />
            </head>
            <body>
                <a class="FeedbackAuthor-author">chef_mario</a>
                <div class="Caption">
                    Délicieuse tarte tatin aux pommes caramélisées !<br>
                    Ingrédients : 4 pommes, 100g de sucre, 50g de beurre.
                </div>
            </body>
            </html>
        """.trimIndent()

        val meta = InstagramMetadataFetcher.parseHtml(sampleHtml)
        assertEquals("@chef_mario", meta.author)
        assertEquals("https://instagram.cdn/thumb123.jpg", meta.thumbnailUrl)
        assertTrue(meta.caption.contains("tarte tatin aux pommes"))
        assertTrue(meta.caption.contains("4 pommes"))
        assertFalse(meta.isAgeRestricted)
        assertFalse(meta.isLoginRequired)
    }

    @Test
    fun `parseHtml detects age-restricted reel content`() {
        val restrictedHtml = """
            <!DOCTYPE html>
            <html>
            <body>
                <div>Contenu soumis à des restrictions d’âge</div>
                <div>Ce contenu est soumis à des restrictions d’âge en fonction de votre âge ou des paramètres de votre compte. Connectez-vous pour continuer.</div>
            </body>
            </html>
        """.trimIndent()

        val meta = InstagramMetadataFetcher.parseHtml(restrictedHtml)
        assertTrue(meta.isAgeRestricted)
    }

    @Test
    fun `parseHtml detects login required page`() {
        val loginHtml = """
            <!DOCTYPE html>
            <html>
            <body>
                <h1>Instagram</h1>
                <p>Connectez-vous pour continuer</p>
                <form action="/accounts/login/"></form>
            </body>
            </html>
        """.trimIndent()

        val meta = InstagramMetadataFetcher.parseHtml(loginHtml)
        assertTrue(meta.isLoginRequired)
    }

    @Test
    fun `InstagramRestrictedException holds clear descriptive message`() {
        val exception = InstagramRestrictedException("Ce Reel nécessite d'être connecté à Instagram.")
        assertEquals("Ce Reel nécessite d'être connecté à Instagram.", exception.message)
    }
}
