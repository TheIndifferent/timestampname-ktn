package io.github.theindifferent.timestampname.extractors

interface TimestampExtractor {
    fun extractMetadataCreationTimestamp(file: String): String

    companion object : ExtractorFactory {
        override fun isSupported(filename: String): Boolean {
            return false
        }
        override fun create(filename: String): TimestampExtractor {
            throw Exception("trying to create TimestampExtractor for file unsupported file: $filename")
        }
    }
}
