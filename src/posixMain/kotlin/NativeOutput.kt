package io.github.theindifferent.timestampname

import kotlinx.cinterop.*
import platform.posix.*

private fun printToStream(stream: CPointer<FILE>?, message: String) {
    val i = fprintf(stream, message)
    if (i < 0) {
        val error = ferror(stream)
        val str = strerror(error)?.toKString()
        throw Exception(str)
    }
}

fun printStdout(message: String) {
    printToStream(stdout, message)
}

fun printStderr(message: String) {
    printToStream(stderr, message)
}
