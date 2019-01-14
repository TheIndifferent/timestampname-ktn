package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.FileException
import io.github.theindifferent.timestampname.readers.Endianess
import io.github.theindifferent.timestampname.readers.Reader

// following resources were used to implement this parser:
// https://www.media.mit.edu/pia/Research/deepview/exif.html
// https://www.fileformat.info/format/jpeg/egff.htm
// http://vip.sugovica.hu/Sardi/kepnezo/JPEG%20File%20Layout%20and%20Format.htm
class JpegTimestampExtractor(private val reader: Reader) : TimestampExtractor {

    companion object : ExtractorFactory {
        override fun isSupported(fileExtension: String): Boolean {
            return fileExtension == "jpg" || fileExtension == "jpeg"
        }

        override fun create(reader: Reader): TimestampExtractor {
            return JpegTimestampExtractor(reader)
        }
    }

    private val jpegSoiExpected: Int = 0xFFD8
    private val jpegApp1Marker: Int = 0xFFE1
    private val exifHeaderSuffixExpected: Int = 0x0000

    override fun extractMetadataCreationTimestamp(): String {
        // checking JPEG SOI:
        val jpegSoi = reader.readUInt16(Endianess.BIG)
        if (jpegSoi != jpegSoiExpected) {
            throw FileException(reader.name(), "unexpected header")
        }
        // scrolling through fields until we find APP1:
        while (true) {
            val fieldMarker = reader.readUInt16(Endianess.BIG)
            val fieldLength = reader.readUInt16(Endianess.BIG)
            if (fieldMarker == jpegApp1Marker) {
                // APP1 marker found, checking Exif header:
                val exifHeader = reader.readString(4)
                val exifHeaderSuffix = reader.readUInt16(Endianess.BIG)
                if (exifHeader != "Exif" || exifHeaderSuffix != exifHeaderSuffixExpected) {
                    throw FileException(reader.name(), "JPEG APP1 field does not have valid Exif header")
                }
                // body is a valid TIFF,
                // size decrements:
                //   -2 field length
                //   -4 exif header
                //   -2 exif header suffix
                val fieldReader = reader.sectionReader(fieldLength.toLong() - 8)
                return TiffTimestampExtractor
                        .create(fieldReader)
                        .extractMetadataCreationTimestamp()
            } else {
                // length includes the length itself:
                reader.fastForward(fieldLength.toLong() - 2)
            }

        }
    }

}
