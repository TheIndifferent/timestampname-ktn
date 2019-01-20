package io.github.theindifferent.timestampname.readers

import io.github.theindifferent.timestampname.FileException
import io.github.theindifferent.timestampname.debug

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
class SectionReader private constructor(private val delegate: Reader,
                                        private val offset: Long,
                                        private val limit: Long): Reader {

    companion object {
        fun createSectionReader(reader: Reader, offset: Long, limit: Long): Reader {
            if (offset + limit > reader.size()) {
                throw FileException(reader.name(), "section reader overflows delegate size")
            }
            return SectionReader(reader, offset, limit)
        }
    }

    private var cursor: Long = 0

    override fun close() {
        delegate.close()
    }

    override fun size(): Long {
        return limit
    }

    override fun name(): String {
        return delegate.name()
    }

    override fun readUInt16(endianess: Endianess): Int {
//        debug("${name()} SectionReader.readUInt16($endianess), cursor: $cursor, size: ${size()}")
        if (cursor + 2 >= size()) {
            throw FileException(delegate.name(), "reading beyong section size")
        }
        cursor += 2
        return delegate.readUInt16(endianess)
    }

    override fun readUInt32(endianess: Endianess): Long {
//        debug("${name()} SectionReader.readUInt32($endianess), cursor: $cursor, size: ${size()}")
        if (cursor + 4 >= size()) {
            throw FileException(delegate.name(), "reading beyond section size")
        }
        cursor += 4
        return delegate.readUInt32(endianess)
    }

    override fun readString(length: Int): String {
//        debug("${name()} SectionReader.readString($length), cursor: $cursor, size: ${size()}")
        if (cursor + length >= size()) {
            throw FileException(delegate.name(), "reading beyond section size")
        }
        cursor += length
        return delegate.readString(length)
    }

    override fun seek(position: Long): Boolean {
        if (position >= limit) {
            throw FileException(delegate.name(), "seeking beyond section size")
        }
        cursor = position
        debug("${name()} SectionReader.seek(..), position: $position, size: ${size()}")
        delegate.seek(offset + position)
        return cursor == size() - 1
    }

    override fun fastForward(distance: Long): Boolean {
        val forwardCursor = cursor + distance
        if (forwardCursor >= size()) {
            throw FileException(delegate.name(), "seeking beyond file size")
        }
        debug("${name()} SectionReader.fastForward(..), position: $forwardCursor, size: ${size()}")
        delegate.fastForward(distance)
        cursor = forwardCursor
        return cursor == size() - 1
    }

    override fun sectionReader(limit: Long): Reader {
        if (cursor.toULong() + limit.toULong() > Long.MAX_VALUE.toULong()) {
            throw FileException(name(), "section reader overflows uint32")
        }
        if (cursor + limit > size()) {
            throw FileException(name(), "section reader overflows section size")
        }
        return SectionReader.createSectionReader(this, cursor, limit)
    }
}
