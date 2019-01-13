package io.github.theindifferent.timestampname.readers

import io.github.theindifferent.timestampname.ErrnoException
import io.github.theindifferent.timestampname.debug
import kotlinx.cinterop.*
import platform.posix.*

class FileReader private constructor(private val fileName: String,
                                     private val fileSize: Long,
                                     private val openFile: CPointer<FILE>) : Reader {

    companion object {
        fun createFileReader(fileName: String): Reader {
            val openFile = fopen(fileName, "r") ?: throw ErrnoException("opening file: $fileName", errno)
            var fileSize: Long = 0
            memScoped {
                val fileStat = alloc<stat>()
                // TODO figure out how to do this with fstat:
                if (stat(fileName, fileStat.ptr) != 0) {
                    throw ErrnoException("stat on $fileName", errno)
                }
                debug("stat read, size: $fileName = ${fileStat.st_size}")
                fileSize = fileStat.st_size
            }

            return FileReader(fileName, fileSize, openFile)
        }

    }

    private val readBufferSize = 64
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

    override fun readUInt16(endianess: Endianess): Int {
        if (fread(readBuffer, 2, 1, openFile).toInt() != 1) {
            throw ErrnoException("reading file $fileName", errno)
        }
        return if (endianess == Endianess.LITTLE) {
            readBuffer[0].toInt().and(0xFF)
                    .plus(readBuffer[1].toInt().and(0xFF).shl(8))
        } else {
            readBuffer[0].toInt().and(0xFF).shl(8)
                    .plus(readBuffer[1].toInt().and(0xFF))
        }
    }

    override fun readUInt32(endianess: Endianess): Long {
        if (fread(readBuffer, 4, 1, openFile).toInt() != 1) {
            throw ErrnoException("reading file $fileName", errno)
        }
        return if (endianess == Endianess.LITTLE) {
            readBuffer[0].toLong().and(0xFF)
                    .plus(readBuffer[1].toLong().and(0xFF).shl(8))
                    .plus(readBuffer[2].toLong().and(0xFF).shl(16))
                    .plus(readBuffer[3].toLong().and(0xFF).shl(24))
        } else {
            readBuffer[0].toLong().and(0xFF).shl(24)
                    .plus(readBuffer[1].toLong().and(0xFF).shl(16))
                    .plus(readBuffer[2].toLong().and(0xFF).shl(8))
                    .plus(readBuffer[3].toLong().and(0xFF))
        }
    }

    override fun readString(length: Int): String {
        if (length > readBufferSize) {
            // TODO implement proper buffer sizing instead:
            throw Exception("requested more data than allocated read buffer")
        }
        if (fread(readBuffer, length.toULong(), 1, openFile).toInt() != 1) {
            throw ErrnoException("reading file $fileName", errno)
        }
        return readBuffer.toKString()
    }

    override fun seek(position: Long) {
        if (fseek(openFile, position, SEEK_SET) != 0) {
            throw ErrnoException("seeking in file $fileName", ferror(openFile))
        }
    }
}
