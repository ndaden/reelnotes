package com.danstudios.reelnotes.data.local

import com.danstudios.reelnotes.domain.model.IngredientItem
import com.danstudios.reelnotes.domain.model.NoteCategory
import com.danstudios.reelnotes.domain.model.ReelNote
import com.danstudios.reelnotes.domain.model.StepItem
import com.danstudios.reelnotes.domain.model.StructuredNoteData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelNoteMappingTest {

    @Test
    fun `domain model to entity and back preserves all fields and structured data`() {
        val domainNote = ReelNote(
            id = 42L,
            reelUrl = "https://www.instagram.com/reel/DDh2O36IEyL/",
            shortcode = "DDh2O36IEyL",
            author = "@chefnico",
            title = "Pâtes Carbonara Traditionnelles",
            category = NoteCategory.RECIPE,
            summary = "La vraie recette romaine sans crème.",
            rawCaption = "Recette complète des carbonara...",
            markdownContent = "# Pâtes Carbonara\n\n- [ ] 200g Guanciale",
            structuredData = StructuredNoteData(
                summary = "La vraie recette romaine sans crème.",
                ingredients = listOf(
                    IngredientItem(name = "Guanciale", amount = "200", unit = "g"),
                    IngredientItem(name = "Pecorino Romano", amount = "80", unit = "g")
                ),
                steps = listOf(
                    StepItem(stepNumber = 1, instruction = "Couper le guanciale en lardons épais.")
                ),
                prepTime = "10 min",
                cookTime = "15 min"
            ),
            thumbnailUrl = "https://example.com/thumb.jpg",
            tags = listOf("pasta", "carbonara", "italie"),
            isFavorite = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000005000L
        )

        val entity = domainNote.toEntity()
        assertEquals(42L, entity.id)
        assertEquals("RECIPE", entity.category)
        assertTrue(entity.tagsJson.contains("carbonara"))
        assertTrue(entity.structuredJson.contains("Guanciale"))

        val mappedBack = entity.toDomainModel()
        assertEquals(domainNote.id, mappedBack.id)
        assertEquals(domainNote.reelUrl, mappedBack.reelUrl)
        assertEquals(domainNote.shortcode, mappedBack.shortcode)
        assertEquals(domainNote.author, mappedBack.author)
        assertEquals(domainNote.title, mappedBack.title)
        assertEquals(domainNote.category, mappedBack.category)
        assertEquals(domainNote.summary, mappedBack.summary)
        assertEquals(domainNote.isFavorite, mappedBack.isFavorite)
        assertEquals(2, mappedBack.structuredData.ingredients.size)
        assertEquals("Guanciale", mappedBack.structuredData.ingredients[0].name)
        assertEquals(1, mappedBack.structuredData.steps.size)
        assertEquals(3, mappedBack.tags.size)
    }
}
