package com.saveourtool.system

actual fun isWindowsInternal(): Boolean =
    osNameOrNull.let { osName: String? ->
        osName != null && osName.startsWith("Windows")
    }

actual fun isUnixInternal(): Boolean =
    osNameOrNull in sequenceOf(
        "Linux",
        "Mac OS X",
        "FreeBSD",
        "AIX",
        "SunOS", "Solaris",
        "HP-UX",
    )

actual fun isUnknownInternal(): Boolean =
    !isWindowsInternal() && !isUnixInternal()

actual fun osNameInternal(): String =
    osNameOrNull + ' ' + System.getProperty("os.version")

private val osNameOrNull: String?
    get() =
        System.getProperty("os.name")


