package io.github.theindifferent.timestampname

import io.github.theindifferent.timestampname.extractors.ExtractorStrategy
import kotlinx.cinterop.toKString
import platform.posix.strerror
import kotlin.system.exitProcess

private class CmdArgs(val dryRun: Boolean,
                      val debug: Boolean,
                      val noPrefix: Boolean,
                      val utc: Boolean,
                      val timezone: String)

private fun parseArgs(args: Array<String>): CmdArgs {
    var dryRun = false
    var debug = false
    var noPrefix = false
    var utc = false
    var timezone = "0"
    for (arg in args) {
        if (arg == "-h") {
            printHelp()
            exitProcess(0)
        }
        if (arg == "-dry") {
            dryRun = true
            continue
        }
        if (arg == "-debug") {
            debug = true
            continue
        }
        if (arg == "-noprefix") {
            noPrefix = true
            continue
        }
        if (arg == "-utc") {
            utc = true
            continue
        }
        if (arg.startsWith("-timezone=")) {
            // TODO implement timezone adjustment if shot in different TZ than processed
            timezone = arg.substring(IntRange("-timezone=".length, arg.length - 1))
            continue
        }
        // if we are still here - got unrecognized argument:
        throw Exception("unrecognized argument: $arg")
    }
    return CmdArgs(dryRun, debug, noPrefix, utc, timezone)
}

private var debugOutput = false

fun debug(message: String) {
    if (debugOutput) {
        printStdout("## $message\n")
    }
}

fun info(message: String) {
    printStdout(message)
}

private data class CollectedMetadata(val metadata: List<FileMetadata>, val longestSourceName: Int)

private fun processFiles(cmdArgs: CmdArgs, files: List<String>): CollectedMetadata {
    val list = mutableListOf<FileMetadata>()
    val strategy = ExtractorStrategy(cmdArgs.utc)
    var longestSourceName = 0

    for ((index, filename) in files.withIndex()) {
        info("\rProcessing files: ${index+1}/${files.size}...")
        val creationTimestamp = strategy.extractCreationTimestamp(filename) ?: continue
        list.add(FileMetadata(filename, creationTimestamp))
        if (filename.length > longestSourceName) {
            longestSourceName = filename.length
        }
    }
    info(" ${list.size} supported files found.\n")
    return CollectedMetadata(list, longestSourceName)
}

fun main(args: Array<String>) {
    try {

        val cmdArgs = parseArgs(args)
        debugOutput = cmdArgs.debug

        info("Scanning for files... ")
        val files = listFiles()
        info("${files.size} files found.\n")

        val collectedMetadata = processFiles(cmdArgs, files)
        info("Preparing rename operations...")
        val operations = Renamer(cmdArgs.noPrefix).prepareRenameOperations(collectedMetadata.metadata)
        info(" done.\n")

        info("Verifying:\n")
        Verifier().verifyOperations(operations, collectedMetadata.longestSourceName)
        info("done.\n")
        Executor().executeOperations(operations, cmdArgs.dryRun)

        info("\nFinished.\n")

        exitProcess(0)

    } catch (errnoEx: ErrnoException) {
        val strErr = strerror(errnoEx.err)?.toKString() ?: "error code ${errnoEx.err}"
        printStderr("\nOperation failed,\n\toperation: ${errnoEx.operation}\n\tfailure: $strErr\n")
    } catch (fex: FileException) {
        printStderr("\nFailed to process file:\n\tfile: ${fex.fileName}\n\tfailure: ${fex.error}")
    } catch (ex: Exception) {
        printStderr(ex.message ?: "Unknown failure")
    }
    exitProcess(1)
}
