package io.github.theindifferent.timestampname

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.system.exitProcess

private class CmdArgs(var dryRun: Boolean,
                      var debug: Boolean,
                      var noPrefix: Boolean,
                      var timezone: String)

private fun parseArgs(args: Array<String>): CmdArgs {
    var dryRun = false
    var debug = false
    var noPrefix = false
    var timezone = "0"
    for (arg in args) {
        if (arg == "-h") {
            // TODO show help and exit
            continue
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
        if (arg.startsWith("-timezone=")) {
            timezone = arg.substring(IntRange("-timezone=".length, arg.length - 1))
            continue
        }
        // if we are still here - got unrecognized argument:
        throw Exception("unrecognized argument: " + arg)
    }
    return CmdArgs(dryRun, debug, noPrefix, timezone)
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

fun main(args: Array<String>) {
    try {

        val cmdArgs = parseArgs(args)
        debugOutput = cmdArgs.debug

        info("Scanning files... ")
        for (file in FolderListing()) {
            info("    $file\n")
        }

    } catch (ex: Exception) {
        printStderr(ex.message ?: "Unknown failure")
        exitProcess(1)
    }
}

fun parseLine(line: String, separator: Char): List<String> {
    val result = mutableListOf<String>()
    val builder = StringBuilder()
    var quotes = 0
    for (ch in line) {
        when {
            ch == '\"' -> {
                quotes++
                builder.append(ch)
            }
            (ch == '\n') || (ch == '\r') -> {
            }
            (ch == separator) && (quotes % 2 == 0) -> {
                result.add(builder.toString())
                builder.setLength(0)
            }
            else -> builder.append(ch)
        }
    }
    return result
}

fun _main(args: Array<String>) {
    if (args.size != 3) {
        println("Usage: csvparser.kexe <file.csv> <column> <count>")
        return
    }
    val fileName = args[0]
    val column = args[1].toInt()
    val count = args[2].toInt()

    val file = fopen(fileName, "r")
    if (file == null) {
        perror("cannot open input file $fileName")
        return
    }

    val keyValue = mutableMapOf<String, Int>()

    try {
        memScoped {
            val bufferLength = 64 * 1024
            val buffer = allocArray<ByteVar>(bufferLength)

            for (i in 1..count) {
                val nextLine = fgets(buffer, bufferLength, file)?.toKString()
                if (nextLine == null || nextLine.isEmpty()) break

                val records = parseLine(nextLine, ',')
                val key = records[column]
                val current = keyValue[key] ?: 0
                keyValue[key] = current + 1
            }
        }
    } finally {
        fclose(file)
    }

    keyValue.forEach {
        println("${it.key} -> ${it.value}")
    }
}
