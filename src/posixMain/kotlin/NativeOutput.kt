package io.github.theindifferent.timestampname

import kotlinx.cinterop.*
import platform.posix.*

private fun printToStream(stream: CPointer<FILE>?, message: String) {
    val i = fprintf(stream, message)
    if (i < 0) {
        val error = ferror(stream)
        throw ErrnoException("print to console", error)
    }
}

fun printStdout(message: String) {
    printToStream(stdout, message)
}

fun printStderr(message: String) {
    printToStream(stderr, message)
}
