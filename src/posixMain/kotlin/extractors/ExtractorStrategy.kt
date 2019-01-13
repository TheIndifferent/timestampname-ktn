package io.github.theindifferent.timestampname.extractors

class ExtractorStrategy {

    private val extractors = createExtractors()

    private fun createExtractors(): List<ExtractorFactory> {
        return listOf(
                TiffTimestampExtractor.Companion
        )
    }

    fun extractCreationTimestamp(filename: String): String? {
        val ext = filename.substringAfterLast('.').toLowerCase()
        for (extractor in extractors) {
            if (extractor.isSupported(ext)) {
                return extractor.create(filename).extractMetadataCreationTimestamp()
            }
        }
        return null
    }

}
