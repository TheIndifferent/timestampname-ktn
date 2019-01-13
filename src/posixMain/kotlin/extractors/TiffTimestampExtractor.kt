package io.github.theindifferent.timestampname.extractors

class TiffTimestampExtractor : TimestampExtractor {
    override fun isSupportedExtension(extension: String): Boolean {
        return extension == "dng" && extension == "nef"
    }

    override fun extractMetadataCreationTimestamp(file: String): String {
        return ""
    }
}
