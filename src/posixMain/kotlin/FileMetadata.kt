package io.github.theindifferent.timestampname

data class FileMetadata(val fileName: String, val creationTimestamp: String) : Comparable<FileMetadata> {
    override fun compareTo(other: FileMetadata): Int {
        if (creationTimestamp == other.creationTimestamp) {
            if (fileName == other.fileName) {
                throw Exception("File encountered twice: $fileName")
            }
            // workaround for Android way of dealing with same-second shots:
            // 20180430_184327.jpg
            // 20180430_184327(0).jpg
            if (fileName.length == other.fileName.length) {
                return if (fileName < other.fileName) {
                    -1
                } else {
                    1
                }
            }
            return if (fileName.length < other.fileName.length) {
                -1
            } else {
                1
            }
        }
        return if (creationTimestamp < other.creationTimestamp) {
            -1
        } else {
            1
        }
    }
}
