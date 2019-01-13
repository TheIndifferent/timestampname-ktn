package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.FileException
import io.github.theindifferent.timestampname.debug
import io.github.theindifferent.timestampname.readers.Endianess
import io.github.theindifferent.timestampname.readers.FileReader
import io.github.theindifferent.timestampname.readers.Reader

// https://www.adobe.io/content/dam/udp/en/open/standards/tiff/TIFF6.pdf
class TiffTimestampExtractor(private val fileName: String) : TimestampExtractor {

    companion object : ExtractorFactory {
        override fun isSupported(fileExtension: String): Boolean {
            return fileExtension == "dng" || fileExtension == "nef"
        }

        override fun create(filename: String): TimestampExtractor {
            return TiffTimestampExtractor(filename)
        }

    }

    private val tiffEndianessLittle: Int = 'I'.toInt().shl(8).plus('I'.toInt())
    private val tiffEndianessBig: Int = 'M'.toInt().shl(8).plus('M'.toInt())

    private val dateRegex = Regex("\\d{2}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}")
    private val dateRegexSamsungBug = Regex("\\d{2}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")

    override fun extractMetadataCreationTimestamp(): String {
        val fileReader = FileReader.createFileReader(fileName)
        try {

            val bo = checkTiffHeader(fileReader)

            // list of all IFD offsets:
            // Bytes 4-7 The offset (in bytes) of the first IFD.
            val ifdOffsets = mutableListOf(fileReader.readUInt32(bo))
            // list of all date tag offsets:
            val dateTagOffsets = mutableListOf<Long>()
            // earliest found date:
            var earliestDate = ""

            while (true) {
                if (ifdOffsets.size == 0 && dateTagOffsets.size == 0) {
                    debug("TIFF no more offsets to scavenge")
                    break
                }
                // TODO should sorting happen here?
                // sorting to traverse file forward-only:
                ifdOffsets.sort()
                dateTagOffsets.sort()

                if (ifdOffsets.size > 0 || dateTagOffsets.size > 0) {

                    // TODO remove this ugly hack, maybe split big method into submethods:
                    val nextDateOffset = if (dateTagOffsets.size > 0) {
                        dateTagOffsets[0]
                    } else {
                        Long.MAX_VALUE
                    }
                    val nextIfdOffset = if (ifdOffsets.size > 0) {
                        ifdOffsets[0]
                    } else {
                        Long.MAX_VALUE
                    }

                    if (nextDateOffset < nextIfdOffset) {
                        debug("TIFF collecting date at offset: $nextDateOffset")
                        dateTagOffsets.removeAt(0)
                        // check for overflow, seek position +20 bytes expected field length:
                        if (nextDateOffset + 20 >= fileReader.size()) {
                            throw FileException(fileName, "date value offset beyond file length")
                        }
                        fileReader.seek(nextDateOffset)
                        val dateValue = fileReader.readString(19)
                        debug("TIFF date value read: $dateValue")
                        if (earliestDate.length == 0) {
                            earliestDate = dateValue
                        } else {
                            if (dateValue < earliestDate) {
                                debug("TIFF replacing old value with new value: $earliestDate => $dateValue")
                                earliestDate = dateValue
                            }
                        }
                    } else {
                        debug("TIFF scavenging IFD at offset: $nextIfdOffset, all offsets: $ifdOffsets")
                        ifdOffsets.removeAt(0)
                        // check for overflow, seek position +2 bytes IFD field count +4 bytes next IFD offset:
                        if (nextIfdOffset + 6 >= fileReader.size()) {
                            throw FileException(fileName, "IFD offset goes over file length")
                        }
                        fileReader.seek(nextIfdOffset)

                        // 2-byte count of the number of directory entries (i.e., the number of fields)
                        val fields = fileReader.readUInt16(bo)
                        for (i in 0..fields) {
                            // Bytes 0-1 The Tag that identifies the field
                            val fieldTag = fileReader.readUInt16(bo)
                            // Bytes 2-3 The field Type
                            val fieldType = fileReader.readUInt16(bo)
                            // Bytes 4-7 The number of values, Count of the indicated Type
                            val fieldCount = fileReader.readUInt32(bo)
                            // Bytes 8-11 The Value Offset, the file offset (in bytes) of the Value for the field
                            val fieldValueOffset = fileReader.readUInt32(bo)

                            debug("TIFF field: tag=$fieldTag, type=$fieldType, count=$fieldCount, offset=$fieldValueOffset")

                            // 0x0132: DateTime
                            // 0x9003: DateTimeOriginal
                            // 0x9004: DateTimeDigitized
                            if (fieldTag == 0x0132 || fieldTag == 0x9003 || fieldTag == 0x9004) {
                                if (fieldType != 2) {
                                    throw FileException(fileName, "expected tag has unexpected type: $fieldTag == $fieldType")
                                }
                                if (fieldCount.toInt() != 20) {
                                    throw FileException(fileName, "expected tag has unexpected size: $fieldTag == $fieldCount")
                                }
                                debug("TIFF IFD value offset for tag: $fieldTag => $fieldValueOffset")
                                dateTagOffsets.add(fieldValueOffset)
                            }
                            // 0x8769: ExifIFDPointer
                            if (fieldTag == 0x8769) {
                                if (fieldType != 4) {
                                    throw FileException(fileName, "EXIF pointer tag has unexpected type: $fieldTag == $fieldType")
                                }
                                if (fieldCount.toInt() != 1) {
                                    throw FileException(fileName, "EXIF pointer tag has unexpected size: $fieldTag == $fieldCount")
                                }
                                debug("TIFF IFD Exif offset: $fieldValueOffset")
                                ifdOffsets.add(fieldValueOffset)
                            }
                        }

                        // followed by a 4-byte offset of the next IFD (or 0 if none).
                        // (Do not forget to write the 4 bytes of 0 after the last IFD.)
                        val parsedIfdOffset = fileReader.readUInt32(bo)
                        debug("TIFF IFD found next IFD offset: $parsedIfdOffset")
                        if (parsedIfdOffset.toInt() != 0) {
                            ifdOffsets.add(parsedIfdOffset)
                        }
                    }
                }
            }

            // TODO fast-forward to the end?

            if (dateRegex.matches(earliestDate) || dateRegexSamsungBug.matches(earliestDate)) {
                val sb = StringBuilder(earliestDate)
                debug("1: $sb")
                sb.deleteCharAt(16)
                debug("2: $sb")
                sb.deleteCharAt(13)
                debug("3: $sb")
                sb.deleteCharAt(10)
                debug("4: $sb")
                sb.deleteCharAt(7)
                debug("5: $sb")
                sb.deleteCharAt(4)
                debug("6: $sb")
                sb.insert(7, '-')
                debug("7: $sb")
                return sb.toString()
            }

            throw FileException(fileName, "failed to parse Exif date: $earliestDate")
        } finally {
            fileReader.close()
        }
    }

    private fun checkTiffHeader(reader: Reader): Endianess {
        // Bytes 0-1: The byte order used within the file. Legal values are:
        // “II” (4949.H)
        // “MM” (4D4D.H)
        // smart thing about specification, we can supplly any endianess:
        val tiffHeaderEndianess = reader.readUInt16(Endianess.BIG)
        // In the “II” format, byte order is always from the least significant byte to the most
        // significant byte, for both 16-bit and 32-bit integers.
        // This is called little-endian byte order.
        //  In the “MM” format, byte order is always from most significant to least
        // significant, for both 16-bit and 32-bit integers.
        // This is called big-endian byte order
        val endianess = when (tiffHeaderEndianess) {
            tiffEndianessBig -> {
                Endianess.BIG
            }
            tiffEndianessLittle -> {
                Endianess.LITTLE
            }
            else -> {
                throw FileException(fileName, "invalid TIFF file header: $tiffHeaderEndianess")
            }
        }
        // Bytes 2-3 An arbitrary but carefully chosen number (42)
        // that further identifies the file as a TIFF file.
        val tiffHeaderMagic = reader.readUInt16(endianess)
        if (tiffHeaderMagic != 42) {
            throw FileException(fileName, "invalid TIFF magic number: $tiffHeaderMagic")
        }
        return endianess
    }

}
