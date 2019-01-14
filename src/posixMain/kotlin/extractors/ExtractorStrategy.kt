package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.readers.FileReader

class ExtractorStrategy {

    private val extractors = createExtractors()

    private fun createExtractors(): List<ExtractorFactory> {
        return listOf(
                TiffTimestampExtractor.Companion,
                JpegTimestampExtractor.Companion
        )
    }

    fun extractCreationTimestamp(fileName: String): String? {
        val ext = fileName.substringAfterLast('.').toLowerCase()
        for (extractor in extractors) {
            if (extractor.isSupported(ext)) {
                return extractor
                        .create(FileReader.createFileReader(fileName))
                        .extractMetadataCreationTimestamp()
            }
        }
        return null
    }
}
