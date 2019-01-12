package io.github.theindifferent.timestampname.extractors

interface ExtractorFactory {
    fun isSupported(filename: String): Boolean
    fun create(filename: String): TimestampExtractor
}
