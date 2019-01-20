package io.github.theindifferent.timestampname.readers

import io.github.theindifferent.timestampname.ErrnoException
import io.github.theindifferent.timestampname.FileException
import io.github.theindifferent.timestampname.debug
import kotlinx.cinterop.*
import platform.posix.*

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
class FileReader private constructor(private val fileName: String,
                                     private val fileSize: Long,
                                     private val openFile: CPointer<FILE>) : Reader {

    companion object {
        fun createFileReader(fileName: String): Reader {
            val openFile = fopen(fileName, "r") ?: throw ErrnoException("opening file: $fileName", errno)
            var fileSize: Long = 0
            memScoped {
                val fileStat = alloc<stat>()
                val fileNo = fileno(openFile)
                if (fstat(fileNo, fileStat.ptr) != 0) {
                    throw ErrnoException("stat on $fileName", errno)
                }
                debug("stat read, size: $fileName = ${fileStat.st_size}")
                fileSize = fileStat.st_size
            }

            return FileReader(fileName, fileSize, openFile)
        }

    }

    private var cursor: Long = 0
    private val readBufferSize = 20
    private val readBuffer = nativeHeap.allocArray<ByteVar>(readBufferSize)

    override fun close() {
        nativeHeap.free(readBuffer)
        if (fclose(openFile) != 0) {
            throw ErrnoException("closing file: $fileName", errno)
        }
    }

    override fun size(): Long {
        return fileSize
    }

    override fun name(): String {
        return fileName
    }

    override fun readUInt16(endianess: Endianess): Int {
        if (cursor + 2 >= size()) {
            throw FileException(fileName, "reading beyond file size")
        }
        if (fread(readBuffer, 2, 1, openFile).toInt() != 1) {
            throw ErrnoException("reading file $fileName", errno)
        }
        // debug("${name()} FileReader.readUInt16($endianess), cursor: $cursor 2 bytes read: ${readBuffer[0]}, ${readBuffer[1]}")
        cursor += 2
        val b0 = readBuffer[0].toInt() and 0xFF
        val b1 = readBuffer[1].toInt() and 0xFF
        return if (endianess == Endianess.LITTLE) {
            b0 + (b1 shl 8)
        } else {
            (b0 shl 8) + b1
        }
    }

    override fun readUInt32(endianess: Endianess): Long {
        if (cursor + 4 >= size()) {
            throw FileException(fileName, "reading beyond file size")
        }
        if (fread(readBuffer, 4, 1, openFile).toInt() != 1) {
            throw ErrnoException("reading file $fileName", errno)
        }
        cursor += 4
        val b0 = readBuffer[0].toLong() and 0xFF
        val b1 = readBuffer[1].toLong() and 0xFF
        val b2 = readBuffer[2].toLong() and 0xFF
        val b3 = readBuffer[3].toLong() and 0xFF
        return if (endianess == Endianess.LITTLE) {
            b0 + (b1 shl 8) + (b2 shl 16) + (b3 shl 24)
        } else {
            (b0 shl 24) + (b1 shl 16) + (b2 shl 8) + b3
        }
    }

    override fun readString(length: Int): String {
        if (length > readBufferSize) {
            // TODO implement proper buffer sizing instead:
            throw Exception("requested more data than allocated read buffer")
        }
        if (cursor + length >= size()) {
            throw FileException(fileName, "reading beyond file size")
        }
        if (fread(readBuffer, length.toULong(), 1, openFile).toInt() != 1) {
            throw ErrnoException("reading file $fileName", errno)
        }
        cursor += length
        val result = readBuffer.toKString()
        memset(readBuffer, 0, readBufferSize.toULong())
        return result
    }

    override fun seek(position: Long): Boolean {
        if (position >= size()) {
            throw FileException(fileName, "seeking beyond file size")
        }
        // debug("${name()} FileReader.seek(..), position: $position, size: ${size()}")
        if (fseek(openFile, position, SEEK_SET) != 0) {
            val streamError = ferror(openFile)
            val err = if (streamError != 0) {
                streamError
            } else {
                errno
            }
            throw ErrnoException("seeking in file $fileName", err)
        }
        cursor = position
        return cursor == size() - 1
    }

    override fun fastForward(distance: Long): Boolean {
        val forwardCursor = cursor + distance
        if (forwardCursor >= size()) {
            throw FileException(fileName, "seeking beyond file size")
        }
        // debug("${name()} FileReader.fastForward(..), position: $forwardCursor, size: ${size()}")
        if (fseek(openFile, distance, SEEK_CUR) != 0) {
            throw ErrnoException("seeking in file $fileName", ferror(openFile))
        }
        cursor = forwardCursor
        return cursor == size() - 1
    }

    override fun sectionReader(limit: Long): Reader {
        if (cursor.toULong() + limit.toULong() > Long.MAX_VALUE.toULong()) {
            throw FileException(name(), "section reader overflows uint32")
        }
        if (cursor + limit > size()) {
            throw FileException(name(), "section reader overflows file size")
        }
        return SectionReader.createSectionReader(this, cursor, limit)
    }
}
