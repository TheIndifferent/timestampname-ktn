package io.github.theindifferent.timestampname.extractors

interface TimestampExtractor {
    fun extractMetadataCreationTimestamp(file: String): String

    fun isSupportedExtension(extension: String): Boolean
}
