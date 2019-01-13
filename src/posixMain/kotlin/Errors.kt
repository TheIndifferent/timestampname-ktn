package io.github.theindifferent.timestampname

data class FileException(val fileName: String, val error: String) : Exception()

data class ErrnoException(val operation: String, val err: Int) : Exception()
