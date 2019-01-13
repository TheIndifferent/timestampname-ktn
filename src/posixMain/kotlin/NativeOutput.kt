package io.github.theindifferent.timestampname

import kotlinx.cinterop.*
import platform.posix.*

private fun printToStream(stream: CPointer<FILE>?, message: String) {
    if (fprintf(stream, message) < 0) {
        throw ErrnoException("print to console", ferror(stream))
    }
//    if (fflush(stream) != 0) {
//        throw ErrnoException("flush the console", ferror(stream))
//    }
}

fun printStdout(message: String) {
    printToStream(stdout, message)
}

fun printStderr(message: String) {
    printToStream(stderr, message)
}
