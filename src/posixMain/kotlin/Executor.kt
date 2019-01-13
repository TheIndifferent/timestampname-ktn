package io.github.theindifferent.timestampname

import platform.posix.*

class Executor {

    fun executeOperations(operations: List<RenameOperation>, dryRun: Boolean) {
        val mode = S_IRUSR.or(S_IRGRP).or(S_IROTH).toUShort()
        for ((index, operation) in operations.withIndex()) {
            info("\rRenaming files: ${index+1}/${operations.size}")
            if (!dryRun) {
                if (rename(operation.from, operation.to) != 0) {
                    throw ErrnoException("rename file ${operation.from} => ${operation.to}", errno)
                }
                if (chmod(operation.to, mode) != 0) {
                    throw ErrnoException("chmod file ${operation.to}", errno)
                }
            }
        }
        info(" done.\n")
    }

}
