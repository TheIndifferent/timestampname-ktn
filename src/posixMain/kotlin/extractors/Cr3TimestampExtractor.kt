package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.debug
import io.github.theindifferent.timestampname.readers.Reader

// following resources were used to implement this parser:
// https://github.com/lclevy/canon_cr3
class Cr3TimestampExtractor(private val reader: Reader) : TimestampExtractor {

    companion object : ExtractorFactory {
        override fun isSupported(fileExtension: String): Boolean {
            return fileExtension == "cr3"
        }

        override fun create(reader: Reader, utc: Boolean): TimestampExtractor {
            return Cr3TimestampExtractor(reader)
        }
    }

    override fun extractMetadataCreationTimestamp(): String {
        val qtParser = QuicktimeBoxParser()
        val moovBoxReader = qtParser.quicktimeSearchBox(reader, "moov")
        val canonBoxReader = qtParser.quicktimeSearchUuidBox(moovBoxReader, "85c0b687820f11e08111f4ce462b6a48")

        val cmt1BoxReader = qtParser.quicktimeSearchBox(canonBoxReader, "CMT1")
        val cmt1Timestamp = TiffTimestampExtractor.create(cmt1BoxReader, false).extractMetadataCreationTimestamp()

        // TODO find a way to read TIFF to the end so we don't have to seek:
        debug("cr3 rewing to the beginning of canon box")
        canonBoxReader.seek(0)

        val cmt2BoxReader = qtParser.quicktimeSearchBox(canonBoxReader, "CMT2")
        val cmt2Timestamp = TiffTimestampExtractor.create(cmt2BoxReader, false).extractMetadataCreationTimestamp()

        return if (cmt1Timestamp < cmt2Timestamp) {
            cmt1Timestamp
        } else {
            cmt2Timestamp
        }
    }
}
