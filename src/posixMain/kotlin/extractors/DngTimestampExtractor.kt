package io.github.theindifferent.timestampname.extractors

class DngTimestampExtractor(filename: String) : TimestampExtractor {
    override fun extractMetadataCreationTimestamp(file: String): String {
        return ""
    }

    companion object : ExtractorFactory {
        override fun isSupported(filename: String): Boolean {
            return filename.toLowerCase().endsWith(".dng")
        }

        override fun create(filename: String): TimestampExtractor {
            return DngTimestampExtractor(filename)
        }

    }

}
