package io.github.theindifferent.timestampname

fun printHelp() {
    printStdout("""
Usage: TimestampNameKtn [ options ]

Options:
    -h          Display help and exit.
    -dry        Only show the operations but do not perform a rename.
    -debug      Enable debug output.
    -noprefix   Do not add numerical prefix to the renamed files
                (works if not more than one file is shot per second).
    -utc        Do not reinterpret MP4 timestamps into local time zone.
                Even though specification suggests to use UTC for CreationDate
                and ModificationDate, some cameras (DJI?) are saving it
                in a local time zone, so the time zone offset will double
                if we will apply conversion to local time zone on top of it.
                This option will produce incorrectly named files if a folder
                contains video files from DJI and Samsung for example.
""")
}
