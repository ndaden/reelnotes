package com.danstudios.reelnotes.domain.util

data class ReelUrlInfo(
    val shortcode: String,
    val cleanUrl: String
)

object UrlParser {
    // Matches instagram.com/reel/SHORTCODE, /reels/SHORTCODE, /p/SHORTCODE, instagr.am/reel/SHORTCODE, etc.
    private val INSTAGRAM_URL_REGEX = Regex(
        """https?://(?:www\.)?(?:instagram\.com|instagr\.am)/(?:reel|reels|p)/([A-Za-z0-9_-]+)""",
        RegexOption.IGNORE_CASE
    )

    fun extractReelInfo(input: String): ReelUrlInfo? {
        val match = INSTAGRAM_URL_REGEX.find(input) ?: return null
        val shortcode = match.groupValues[1]
        val cleanUrl = "https://www.instagram.com/reel/$shortcode/"
        return ReelUrlInfo(shortcode = shortcode, cleanUrl = cleanUrl)
    }

    fun extractCaptionFromSharedText(input: String): String {
        val match = INSTAGRAM_URL_REGEX.find(input) ?: return input.trim()
        val textWithoutUrl = input.replace(Regex("""https?://\S+"""), "").trim()
        return textWithoutUrl
    }
}
