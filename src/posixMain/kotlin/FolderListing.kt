package io.github.theindifferent.timestampname

import kotlinx.cinterop.*
import platform.posix.*

private class NativeDirIterator(val path: String) : Iterator<String> {

    var openDir: CPointer<DIR>? = null
    var next: String? = null

    override fun hasNext(): Boolean {
        debug("iterating over files in dir: $path")
        if (openDir == null) {
            debug("opening dir: $path")
            openDir = opendir(path) ?: throw Exception(strerror(errno)?.toKString())
        }
        val errnoBefore = errno
        var dp: CPointer<dirent>?
        while (true) {

            // doing this operation breaks before assigning the variable,
            // so the clean will never be invoked:
            // dp = readdir(openDir) ?: break
            // instead have to break it into 2 different operations:
            dp = readdir(openDir)
            if (dp == null) {
                debug("breaking dir iteration loop, dp = $dp")
                break
            }
            val name = dp.pointed.d_name.toKString()
            if (dp.pointed.d_type.toInt() == DT_REG) {
                next = name
                debug("next file: $next")
                return true
            } else {
                debug("skipping non-regular file: $name")
                continue
            }

        }
        debug("dir iteration loop finished, dp = $dp")
        // if we finished reading - close the dir:
        if (dp == null) {
            debug("closing dir $openDir")
            closedir(openDir)
        }
        // if error changed during the iteration - throw:
        if (errnoBefore != errno) {
            debug("error changed: $errnoBefore => $errno")
            throw Exception(strerror(errno)?.toKString())
        }
        return false
    }

    override fun next(): String {
        return next ?: throw Exception("next() invoked without hasNext() check")
    }

}

class FolderListing : Iterable<String> {

    private fun cwd(): String {
        memScoped {
            val buffer = allocArray<ByteVar>(PATH_MAX)
            if (getcwd(buffer, PATH_MAX) == null) {
                throw Exception(strerror(errno)?.toKString())
            }
            return buffer.toKString()
        }
    }

    override fun iterator(): Iterator<String> {
        return NativeDirIterator(cwd())
    }

}
