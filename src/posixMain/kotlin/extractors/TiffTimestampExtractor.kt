package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.readers.FileReader

class TiffTimestampExtractor(private val filename: String) : TimestampExtractor {

    companion object : ExtractorFactory {
        override fun isSupported(fileExtension: String): Boolean {
            return fileExtension == "dng" || fileExtension == "nef"
        }

        override fun create(filename: String): TimestampExtractor {
            return TiffTimestampExtractor(filename)
        }

    }

    override fun extractMetadataCreationTimestamp(): String {
        val fileReader = FileReader.createFileReader(filename)
        try {

            return ""
        } finally {
            fileReader.close()
        }
    }

}
