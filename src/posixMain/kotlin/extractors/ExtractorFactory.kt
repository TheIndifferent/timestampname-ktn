package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.readers.Reader

interface ExtractorFactory {
    fun isSupported(fileExtension: String): Boolean
    fun create(reader: Reader, utc: Boolean): TimestampExtractor
}
