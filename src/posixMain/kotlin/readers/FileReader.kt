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

    override fun close() {
        if (fclose(openFile) != 0) {
            throw ErrnoException("closing file: $fileName", errno)
        }
    }
}
