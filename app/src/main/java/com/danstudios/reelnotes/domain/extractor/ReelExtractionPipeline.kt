package com.danstudios.reelnotes.domain.extractor

import com.danstudios.reelnotes.data.network.GeminiSummarizer
import com.danstudios.reelnotes.data.network.InstagramMetadataFetcher
import com.danstudios.reelnotes.domain.model.NoteCategory
import com.danstudios.reelnotes.domain.model.ReelNote
import com.danstudios.reelnotes.domain.model.StructuredNoteData
import com.danstudios.reelnotes.domain.util.UrlParser

object ReelExtractionPipeline {

    suspend fun processReel(
        sharedInput: String,
        apiKey: String? = null,
        preferredLanguage: String = "fr"
    ): ReelNote {
        val reelInfo = UrlParser.extractReelInfo(sharedInput)
            ?: throw IllegalArgumentException("Lien Instagram Reel introuvable dans le texte partagé.")

        val cleanUrl = reelInfo.cleanUrl
        val shortcode = reelInfo.shortcode
        val initialCaption = UrlParser.extractCaptionFromSharedText(sharedInput)

        // 1. Fetch metadata from web/embed
        val fetchedMeta = InstagramMetadataFetcher.fetch(shortcode, cleanUrl)
        val combinedCaption = if (fetchedMeta.caption.length > initialCaption.length) {
            fetchedMeta.caption
        } else {
            initialCaption.ifBlank { fetchedMeta.caption }
        }
        val author = fetchedMeta.author
        val thumb = fetchedMeta.thumbnailUrl

        // 2. If API Key available and caption present, try Gemini AI
        if (!apiKey.isNullOrBlank() && combinedCaption.isNotBlank()) {
            val aiResult = GeminiSummarizer.summarize(combinedCaption, apiKey, preferredLanguage)
            if (aiResult != null) {
                val category = NoteCategory.fromString(aiResult.category)
                val structuredData = StructuredNoteData(
                    summary = aiResult.summary,
                    ingredients = aiResult.ingredients,
                    steps = aiResult.steps,
                    keyTakeaways = aiResult.keyTakeaways,
                    prepTime = aiResult.prepTime,
                    cookTime = aiResult.cookTime,
                    servings = aiResult.servings,
                    tips = aiResult.tips
                )

                val markdown = buildAiMarkdown(
                    title = aiResult.title,
                    author = author,
                    reelUrl = cleanUrl,
                    category = category,
                    structuredData = structuredData,
                    tags = aiResult.tags
                )

                return ReelNote(
                    reelUrl = cleanUrl,
                    shortcode = shortcode,
                    author = author,
                    title = aiResult.title.ifBlank { "${category.label} - Reel $shortcode" },
                    category = category,
                    summary = aiResult.summary.ifBlank { combinedCaption.take(120) },
                    rawCaption = combinedCaption,
                    markdownContent = markdown,
                    structuredData = structuredData,
                    thumbnailUrl = thumb,
                    tags = aiResult.tags
                )
            }
        }

        // 3. Fallback: Offline Heuristic NLP Extractor
        return OfflineHeuristicExtractor.extract(
            caption = combinedCaption.ifBlank { "Reel Instagram $shortcode" },
            reelUrl = cleanUrl,
            shortcode = shortcode,
            author = author,
            thumbnailUrl = thumb
        )
    }

    private fun buildAiMarkdown(
        title: String,
        author: String?,
        reelUrl: String,
        category: NoteCategory,
        structuredData: StructuredNoteData,
        tags: List<String>
    ): String {
        val sb = StringBuilder()
        sb.append("# $title\n\n")
        if (!author.isNullOrBlank()) {
            sb.append("**Auteur :** $author  \n")
        }
        sb.append("**Catégorie :** ${category.iconEmoji} ${category.label}  \n")
        sb.append("**Lien Reel :** [Voir sur Instagram]($reelUrl)\n\n")

        if (structuredData.summary.isNotBlank()) {
            sb.append("### Résumé\n${structuredData.summary}\n\n")
        }

        val metaTimes = listOfNotNull(
            structuredData.prepTime?.let { "⏱️ Préparation : $it" },
            structuredData.cookTime?.let { "🔥 Cuisson : $it" },
            structuredData.servings?.let { "👥 Portions : $it" }
        )
        if (metaTimes.isNotEmpty()) {
            sb.append(metaTimes.joinToString(" • ")).append("\n\n")
        }

        if (structuredData.ingredients.isNotEmpty()) {
            sb.append("## Ingrédients\n")
            for (ing in structuredData.ingredients) {
                val qty = listOf(ing.amount, ing.unit).filter { it.isNotBlank() }.joinToString(" ")
                if (qty.isNotBlank()) {
                    sb.append("- [ ] **$qty** ${ing.name}\n")
                } else {
                    sb.append("- [ ] ${ing.name}\n")
                }
            }
            sb.append("\n")
        }

        if (structuredData.steps.isNotEmpty()) {
            sb.append("## Étapes / Préparation\n")
            for (s in structuredData.steps) {
                sb.append("${s.stepNumber}. ${s.instruction}\n")
            }
            sb.append("\n")
        }

        if (structuredData.tips.isNotEmpty()) {
            sb.append("## Astuces du chef\n")
            for (tip in structuredData.tips) {
                sb.append("- 💡 $tip\n")
            }
            sb.append("\n")
        }

        if (structuredData.keyTakeaways.isNotEmpty()) {
            sb.append("## Points clés\n")
            for (pt in structuredData.keyTakeaways) {
                sb.append("- $pt\n")
            }
            sb.append("\n")
        }

        if (tags.isNotEmpty()) {
            sb.append("### Tags\n")
            sb.append(tags.joinToString(" ") { "#$it" }).append("\n")
        }

        return sb.toString().trim()
    }
}
