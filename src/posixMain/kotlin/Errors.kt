package io.github.theindifferent.timestampname

data class FileException(val fileName: String, val error: String) : Exception("")
