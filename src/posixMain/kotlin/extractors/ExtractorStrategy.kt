package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.readers.FileReader

class ExtractorStrategy(private val utc: Boolean) {

    private val extractors = createExtractors()

    private fun createExtractors(): List<ExtractorFactory> {
        return listOf(
                TiffTimestampExtractor.Companion,
                JpegTimestampExtractor.Companion,
                Cr3TimestampExtractor.Companion,
                Mp4TimestampExtractor.Companion
        )
    }

    fun extractCreationTimestamp(fileName: String): String? {
        val ext = fileName.substringAfterLast('.').toLowerCase()
        for (extractor in extractors) {
            if (extractor.isSupported(ext)) {
                val reader = FileReader.createFileReader(fileName)
                try {
                    return extractor
                            .create(reader, utc)
                            .extractMetadataCreationTimestamp()
                } finally {
                    reader.close()
                }
            }
        }
        return null
    }
}
