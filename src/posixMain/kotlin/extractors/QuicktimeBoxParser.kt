package io.github.theindifferent.timestampname.extractors

import io.github.theindifferent.timestampname.FileException
import io.github.theindifferent.timestampname.debug
import io.github.theindifferent.timestampname.readers.Endianess
import io.github.theindifferent.timestampname.readers.Reader

// following documents were used to implement this parser:
// http://l.web.umkc.edu/lizhu/teaching/2016sp.video-communication/ref/mp4.pdf
// https://mpeg.chiariglione.org/standards/mpeg-4/iso-base-media-file-format
class QuicktimeBoxParser {

    private fun quicktimeSearchBox(reader: Reader,
                                   matchName: (boxName: String) -> Boolean,
                                   matchUuid: ((boxUuidMsb: Long, boxUuidLsb: Long) -> Boolean)?): Reader? {
        while (true) {
            val boxLength = reader.readUInt32(Endianess.BIG)
            val boxType = reader.readString(4)
            debug("quicktime encountered box '$boxType'")
            // checking for large box:
            var boxBodyLength: Long
            if (boxLength.toInt() == 1) {
                val boxLargeLength = reader
                        .readUInt32(Endianess.BIG)
                        .toULong()
                        .shl(32)
                        .plus(reader.readUInt32(Endianess.BIG).toULong())
                // fseek operation only accepts Long:
                if (boxLargeLength > Long.MAX_VALUE.toULong()) {
                    throw FileException(reader.name(), "box size overflows 4 bytes")
                }
                // box lenght includes header, have to make adjustments:
                // 4 bytes for box length
                // 4 bytes for box type
                // 8 bytes for box large length
                boxBodyLength = boxLargeLength.toLong() - 16
            } else {
                // box lenght includes header, have to make adjustments:
                // 4 bytes for box length
                // 4 bytes for box type
                boxBodyLength = boxLength - 8
            }
            if (matchName(boxType)) {
                if (matchUuid == null) {
                    debug("quicktime box found with length: $boxBodyLength")
                    return reader.sectionReader(boxBodyLength)
                }
                val msb = reader.readUInt32(Endianess.BIG).shl(32)
                        .plus(reader.readUInt32(Endianess.BIG))
                val lsb = reader.readUInt32(Endianess.BIG).shl(32)
                        .plus(reader.readUInt32(Endianess.BIG))
                // 16 bytes read from the total box length:
                boxBodyLength -= 16
                debug("quicktime box reading UUID, msb: $msb, lsb: $lsb")
                if (matchUuid(msb, lsb)) {
                    debug("quicktime box found with length: $boxBodyLength")
                    return reader.sectionReader(boxBodyLength)
                }
            }

            if (reader.fastForward(boxBodyLength)) {
                debug("quicktime reached EOF")
                break
            }
        }
        return null
    }

    fun quicktimeSearchUuidBox(reader: Reader, boxUuidNeeded: String): Reader {
        debug("quicktime searching for UUID box: $boxUuidNeeded")
        val msb = boxUuidNeeded.substring(0, 8)
                .toLong(16).and(0xffffffffL)
                .shl(32) +
                boxUuidNeeded.substring(8, 16)
                        .toLong(16).and(0xffffffffL)
        val lsb = boxUuidNeeded.substring(16, 24).toLong(16).and(0xffffffffL)
                .shl(32) +
                boxUuidNeeded.substring(24).toLong(16).and(0xffffffffL)
        debug("quicktime searching for UUID box: $boxUuidNeeded, msb: $msb, lsb: $lsb")
        return quicktimeSearchBox(
                reader,
                { boxName ->
                    boxName == "uuid"
                },
                { boxUuidMsb, boxUuidLsb ->
                    boxUuidMsb == msb && boxUuidLsb == lsb
                }) ?: throw FileException(reader.name(), "box not found: uuid:$boxUuidNeeded")

    }

    fun quicktimeSearchBox(reader: Reader, boxTypeNeeded: String): Reader {
        debug("quicktime searching for box: $boxTypeNeeded")
        return quicktimeSearchBox(
                reader,
                { boxName -> boxName == boxTypeNeeded },
                null
        ) ?: throw FileException(reader.name(), "box not fund: $boxTypeNeeded")
    }

}
