package io.github.theindifferent.timestampname

inline class Renamer(private val noPrefix: Boolean) {

    fun prepareRenameOperations(inputFileMetadata: List<FileMetadata>): List<RenameOperation> {
        val count = inputFileMetadata.size
        val padSize = when {
            // all pad sizes are +1 for dash separator:
            count < 10 -> 2
            count < 100 -> 3
            count < 1000 -> 4
            count < 10000 -> 5
            count < 100000 -> 6
            else -> throw Exception("too many files: $count")
        }
        return inputFileMetadata
                .sorted()
                .mapIndexed { index: Int, fileMetadata: FileMetadata ->
                    val number = if (noPrefix) {
                        ""
                    } else {
                        "${index+1}-"
                    }
                            .padStart(padSize, '0')
                    val name = fileMetadata.creationTimestamp
                    val ext = fileMetadata.fileName.substringAfterLast('.').toLowerCase()
                    val to = "$number$name.$ext"
                    RenameOperation(fileMetadata.fileName, to)
                }
    }
}
