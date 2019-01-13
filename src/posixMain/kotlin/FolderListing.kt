package io.github.theindifferent.timestampname

import kotlinx.cinterop.*
import platform.posix.*

fun listFiles(): List<String> {
    val files = mutableListOf<String>()
    memScoped {
        val buffer = allocArray<ByteVar>(PATH_MAX)
        if (getcwd(buffer, PATH_MAX) == null) {
            throw Exception(strerror(errno)?.toKString())
        }
        val path = buffer.toKString()
        debug("iterating over files in dir: $path")
        val openDir = opendir(path) ?: throw Exception(strerror(errno)?.toKString())
        try {
            val errnoBefore = errno
            while (true) {

                // doing this operation breaks before assigning the variable,
                // so the clean will never be invoked:
                // dp = readdir(openDir) ?: break
                // instead have to break it into 2 different operations:
                val dp = readdir(openDir)
                if (dp == null) {
                    debug("breaking dir iteration loop")
                    break
                }
                val name = dp.pointed.d_name.toKString()
                debug("checking file: $name")
                if (dp.pointed.d_type.toInt() != DT_REG) {
                    debug("skipping non-regular file: $name")
                    continue
                }
                debug("adding file: $name")
                files.add(name)

            }
            debug("dir iteration loop finished")
            // if error changed during the iteration - throw:
            if (errnoBefore != errno) {
                debug("error changed: $errnoBefore => $errno")
                throw Exception(strerror(errno)?.toKString())
            }
        } finally {
            debug("closing dir $openDir")
            closedir(openDir)
        }
    }
    return files
}
