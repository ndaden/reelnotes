package com.danstudios.reelnotes.domain.extractor

class InstagramRestrictedException(
    override val message: String
) : Exception(message)
