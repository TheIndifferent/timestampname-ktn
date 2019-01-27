package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.FileException
import io.github.theindifferent.timestampname.debug
import io.github.theindifferent.timestampname.readers.Endianess
import io.github.theindifferent.timestampname.readers.Reader
import kotlinx.cinterop.*
import platform.posix.*

@Suppress("EXPERIMENTAL_API_USAGE")
class Mp4TimestampExtractor(private val reader: Reader,
                            private val utc: Boolean) : TimestampExtractor {

    companion object : ExtractorFactory {
        override fun isSupported(fileExtension: String): Boolean {
            return fileExtension == "mp4"
        }

        override fun create(reader: Reader, utc: Boolean): TimestampExtractor {
            return Mp4TimestampExtractor(reader, utc)
        }

        /*
         * The following code can be used to calculate this offset (other than online tools):
        private val mp4EpochOffset = calculateMp4EpochOffset()
        private fun calculateMp4EpochOffset(): Long {
            memScoped {
                val tm = alloc<tm>()
                // tm starts year 1900
                tm.tm_year = 4
                tm.tm_mon = 0
                tm.tm_mday = 1
                val mp4EpochOffset = mktime(tm.ptr) - timezone_
                debug("mp4 epoch offset: $mp4EpochOffset")
                return mp4EpochOffset
            }
        }
         */
        private const val mp4EpochOffset = -2082844800L

    }

    override fun extractMetadataCreationTimestamp(): String {
        val qtParser = QuicktimeBoxParser()
        val moovBoxReader = qtParser.quicktimeSearchBox(reader, "moov")
        val mvhdBoxReader = qtParser.quicktimeSearchBox(moovBoxReader, "mvhd")

        val mvhdHeader = mvhdBoxReader.readUInt32(Endianess.BIG)
        val mvhdVersion = mvhdHeader.shr(24)
        debug("mp4 mvhd header: $mvhdHeader, version: $mvhdVersion")
        if (mvhdVersion > 1) {
            throw FileException(reader.name(), "unsupported mvhd box version: $mvhdVersion")
        }

        val creationTime = readTime(mvhdBoxReader, mvhdVersion)
        val modificationTime = readTime(mvhdBoxReader, mvhdVersion)
        debug("mp4 creation time: $creationTime, modification time: $modificationTime")

        val metadataTime = if (modificationTime < creationTime) {
            modificationTime
        } else {
            creationTime
        }
        debug("mp4 metadata time: $metadataTime")
        memScoped {
            debug("mp4 preferring UTC over local: $utc")
            val timeRef = if (utc) {
                gmtime(cValuesOf(metadataTime))
            } else {
                localtime(cValuesOf(metadataTime))
            }
            val timeTm = timeRef?.pointed ?: throw FileException(reader.name(), "mp4 failed to parse time")
            debug("mp4 time: ${timeTm.tm_year}-${timeTm.tm_mon}-${timeTm.tm_mday}T${timeTm.tm_hour}-${timeTm.tm_min}-${timeTm.tm_sec}")
            val buffer = allocArray<ByteVar>(16)
            if (strftime(buffer, 16, "%Y%m%d-%H%M%S", timeRef) == 0UL) {
                throw FileException(reader.name(), "mp4 failed to format date")
            }
            val timeString = buffer.toKString()
            debug("mp4 time string: $timeString")
            return timeString
        }
    }

    private fun readTime(reader: Reader, version: Long): Long {
        if (version == 0L) {
            val value = reader.readUInt32(Endianess.BIG)
            debug("mp4 value read: $value")
            // epoch offset is negative, so this should not overflow:
            if (value + mp4EpochOffset > Long.MAX_VALUE) {
                throw FileException(reader.name(), "mp4 date v$version overflows Long")
            }
            return value + mp4EpochOffset
        }
        // version is 1:
        val value = reader.readUInt32(Endianess.BIG)
                .toULong()
                .shl(32) +
                reader.readUInt32(Endianess.BIG).toULong()
        debug("mp4 value read: $value")
        val result = value + mp4EpochOffset.toULong()
        if (result > Long.MAX_VALUE.toULong()) {
            throw FileException(reader.name(), "mp4 date overflows Long")
        }
        return result.toLong()
    }
}
