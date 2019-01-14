package io.github.theindifferent.timestampname.readers

import io.github.theindifferent.timestampname.FileException

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

    var cursor: Long = offset

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
        if (cursor + 2 >= size()) {
            throw FileException(delegate.name(), "reading beyong section size")
        }
        cursor += 2
        return delegate.readUInt16(endianess)
    }

    override fun readUInt32(endianess: Endianess): Long {
        if (cursor + 4 >= size()) {
            throw FileException(delegate.name(), "reading beyond section size")
        }
        cursor += 4
        return delegate.readUInt32(endianess)
    }

    override fun readString(length: Int): String {
        if (cursor + length >= size()) {
            throw FileException(delegate.name(), "reading beyond section size")
        }
        cursor += length
        return delegate.readString(length)
    }

    override fun seek(position: Long) {
        if (position >= limit) {
            throw FileException(delegate.name(), "seeking beyond section size")
        }
        cursor = position
        delegate.seek(offset + position)
    }

    override fun fastForward(distance: Long) {
        val forwardCursor = cursor + distance
        if (forwardCursor >= size()) {
            throw FileException(delegate.name(), "seeking beyond file size")
        }
        delegate.fastForward(distance)
        cursor = forwardCursor
    }

    override fun sectionReader(limit: Long): Reader {
        return SectionReader.createSectionReader(this, cursor, limit)
    }
}
