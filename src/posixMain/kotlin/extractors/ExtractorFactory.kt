package io.github.theindifferent.timestampname.extractors

interface ExtractorFactory {
    fun isSupported(fileExtension: String): Boolean
    fun create(filename: String): TimestampExtractor
}
