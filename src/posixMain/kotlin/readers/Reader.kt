package io.github.theindifferent.timestampname.readers

interface Reader {

    fun close()
    fun size(): Long
    fun name(): String
    // TODO this should be UShort, but API is extremely inconvenient:
    fun readUInt16(endianess: Endianess): Int
    // TODO this should be UInt, but API is extremely inconvenient:
    fun readUInt32(endianess: Endianess): Long
    fun readString(length: Int): String
    fun seek(position: Long)
    fun fastForward(distance: Long)
    fun sectionReader(limit: Long): Reader

}
