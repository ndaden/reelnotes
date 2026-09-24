package com.danstudios.reelnotes.domain.extractor

import android.content.Context
import com.danstudios.reelnotes.data.network.GeminiAiOutput
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
        preferredLanguage: String = "fr",
        manualCaption: String? = null,
        context: Context? = null,
        onProgressUpdate: ((String) -> Unit)? = null
    ): ReelNote {
        val reelInfo = UrlParser.extractReelInfo(sharedInput)
            ?: throw IllegalArgumentException("Lien Instagram Reel introuvable dans le texte partagé.")

        val cleanUrl = reelInfo.cleanUrl
        val shortcode = reelInfo.shortcode
        val initialCaption = UrlParser.extractCaptionFromSharedText(sharedInput)

        // 1. Fetch metadata & media stream
        onProgressUpdate?.invoke("Chargement du Reel Instagram...")
        val fetchedMeta = InstagramMetadataFetcher.fetch(shortcode, cleanUrl, context)

        // 2. Check if blocked by age restriction or login wall
        val effectiveManualCaption = manualCaption?.trim().orEmpty()
        val combinedCaption = when {
            effectiveManualCaption.isNotBlank() -> effectiveManualCaption
            fetchedMeta.caption.length > initialCaption.length -> fetchedMeta.caption
            initialCaption.isNotBlank() -> initialCaption
            else -> fetchedMeta.caption
        }

        if ((fetchedMeta.isAgeRestricted || fetchedMeta.isLoginRequired) &&
            combinedCaption.isBlank() &&
            fetchedMeta.mediaBytes == null
        ) {
            throw InstagramRestrictedException(
                "Ce Reel est soumis à des restrictions d'âge ou nécessite une connexion Instagram. Connectez-vous à votre compte dans les Paramètres pour le débloquer."
            )
        }

        val author = fetchedMeta.author
        val thumb = fetchedMeta.thumbnailUrl

        var aiResult: GeminiAiOutput? = null

        // 3. Multimodal analysis (watching and listening to the video / audio stream)
        if (!apiKey.isNullOrBlank() && fetchedMeta.mediaBytes != null && fetchedMeta.mediaMimeType != null) {
            onProgressUpdate?.invoke("L'IA écoute et analyse la vidéo du Reel...")
            aiResult = GeminiSummarizer.summarizeMultimodal(
                mediaBytes = fetchedMeta.mediaBytes,
                mimeType = fetchedMeta.mediaMimeType,
                captionContext = combinedCaption,
                apiKey = apiKey,
                preferredLanguage = preferredLanguage
            )
        }

        // 4. Text-only fallback if multimodal wasn't applicable or failed
        if (aiResult == null && !apiKey.isNullOrBlank() && combinedCaption.isNotBlank()) {
            onProgressUpdate?.invoke("L'IA analyse les instructions textuelles...")
            aiResult = GeminiSummarizer.summarize(combinedCaption, apiKey, preferredLanguage)
        }

        // 5. Build structured note if AI succeeded
        if (aiResult != null) {
            onProgressUpdate?.invoke("Génération des notes structurées...")
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

        // 6. If API key is configured but neither media nor caption could be obtained
        if (!apiKey.isNullOrBlank() && combinedCaption.isBlank() && fetchedMeta.mediaBytes == null) {
            throw InstagramRestrictedException(
                "Impossible d'extraire le flux vidéo ou la légende de ce Reel Instagram. Le contenu nécessite peut-être une connexion. Connectez votre compte Instagram dans les Paramètres pour débloquer l'accès ou collez la légende via le bouton '+'."
            )
        }

        // 7. Offline Heuristic NLP Extractor
        onProgressUpdate?.invoke("Analyse locale hors-ligne...")
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
                val qty = listOfNotNull(ing.amount, ing.unit).filter { it.isNotBlank() }.joinToString(" ")
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
