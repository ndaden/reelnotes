package com.danstudios.reelnotes.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlParserTest {

    @Test
    fun `extractReelInfo correctly extracts clean URL and shortcode from standard reel link`() {
        val input = "https://www.instagram.com/reel/DDh2O36IEyL/"
        val info = UrlParser.extractReelInfo(input)

        assertNotNull(info)
        assertEquals("DDh2O36IEyL", info?.shortcode)
        assertEquals("https://www.instagram.com/reel/DDh2O36IEyL/", info?.cleanUrl)
    }

    @Test
    fun `extractReelInfo cleans tracking parameters like igsh and utm`() {
        val input = "https://www.instagram.com/reel/DDh2O36IEyL/?igsh=MXFhMmtvdWh6eTRlMQ%3D%3D&utm_source=ig_web_copy_link"
        val info = UrlParser.extractReelInfo(input)

        assertNotNull(info)
        assertEquals("DDh2O36IEyL", info?.shortcode)
        assertEquals("https://www.instagram.com/reel/DDh2O36IEyL/", info?.cleanUrl)
    }

    @Test
    fun `extractReelInfo extracts url embedded within shared text`() {
        val input = "Check out this amazing pasta recipe: https://www.instagram.com/reel/DDh2O36IEyL/?igsh=abc123xyz it looks so good!"
        val info = UrlParser.extractReelInfo(input)

        assertNotNull(info)
        assertEquals("DDh2O36IEyL", info?.shortcode)
        assertEquals("https://www.instagram.com/reel/DDh2O36IEyL/", info?.cleanUrl)
    }

    @Test
    fun `extractReelInfo supports instagram post URLs`() {
        val input = "https://www.instagram.com/p/DDh2O36IEyL/"
        val info = UrlParser.extractReelInfo(input)

        assertNotNull(info)
        assertEquals("DDh2O36IEyL", info?.shortcode)
        assertEquals("https://www.instagram.com/reel/DDh2O36IEyL/", info?.cleanUrl)
    }

    @Test
    fun `extractReelInfo supports reels plural and instagr am links`() {
        val input1 = "https://instagram.com/reels/DDh2O36IEyL"
        val info1 = UrlParser.extractReelInfo(input1)
        assertEquals("DDh2O36IEyL", info1?.shortcode)

        val input2 = "https://instagr.am/reel/DDh2O36IEyL/"
        val info2 = UrlParser.extractReelInfo(input2)
        assertEquals("DDh2O36IEyL", info2?.shortcode)
    }

    @Test
    fun `extractReelInfo returns null for non-instagram or invalid text`() {
        assertNull(UrlParser.extractReelInfo("just some random text"))
        assertNull(UrlParser.extractReelInfo("https://youtube.com/watch?v=12345"))
        assertNull(UrlParser.extractReelInfo("https://www.instagram.com/explore/"))
    }

    @Test
    fun `extractSharedTextCaption separates caption text from the reel url`() {
        val input = "Super fast 15min garlic parmesan noodles! https://www.instagram.com/reel/DDh2O36IEyL/?igsh=123"
        val caption = UrlParser.extractCaptionFromSharedText(input)
        assertEquals("Super fast 15min garlic parmesan noodles!", caption)
    }

    @Test
    fun `extractAuthorFromSharedText extracts author username from shared intent text`() {
        val frInput = "Regardez cette vidéo de @linstant_tesla sur Instagram : https://www.instagram.com/reel/DdoSXV8KeM2/?igsh=123"
        assertEquals("@linstant_tesla", UrlParser.extractAuthorFromSharedText(frInput))

        val enInput = "Watch this reel by @ikrcook on Instagram https://www.instagram.com/reel/C76zFvaovP3/"
        assertEquals("@ikrcook", UrlParser.extractAuthorFromSharedText(enInput))

        val bareInput = "Check @chef_john's latest tips https://instagram.com/reel/123"
        assertEquals("@chef_john", UrlParser.extractAuthorFromSharedText(bareInput))

        val noAuthorInput = "https://www.instagram.com/reel/DdoSXV8KeM2/"
        assertNull(UrlParser.extractAuthorFromSharedText(noAuthorInput))
    }
}
