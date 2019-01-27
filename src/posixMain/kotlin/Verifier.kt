package io.github.theindifferent.timestampname

import kotlinx.cinterop.*
import platform.posix.*

class Verifier {

    fun verifyOperations(operations: List<RenameOperation>,
                         longestSourceName: Int) {
        memScoped {
            val fileStat = alloc<stat>()
            val duplicatesSet = mutableSetOf<String>()
            for (operation in operations) {
                info("    ${operation.from.padStart(longestSourceName, ' ')}    =>    ${operation.to}\n")
                // check for target name duplicates:
                if (duplicatesSet.contains(operation.to)) {
                    throw Exception("duplicate rename: ${operation.to}")
                } else {
                    duplicatesSet.add(operation.to)
                }
                // check for renaming duplicates:
                if (operation.from != operation.to) {
                    if (stat(operation.to, fileStat.ptr) == 0) {
                        throw FileException(operation.to, "file exists on filesystem")
                    }
                    if (errno != ENOENT) {
                        throw FileException(operation.to, "expected 'Not Found', received: $errno")
                    }
                }
            }
        }
    }
}
