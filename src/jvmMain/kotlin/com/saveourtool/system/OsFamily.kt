package com.saveourtool.system

import kotlin.reflect.KFunction0

internal actual object OsFamily {
    actual fun isWindows(): Boolean =
        System.getProperty("os.name").let { osName: String? ->
            osName != null && osName.startsWith("Windows")
        }

    fun isLinux(): Boolean =
        System.getProperty("os.name") == "Linux"

    fun isMacOsX(): Boolean =
        System.getProperty("os.name") == "Mac OS X"

    fun isSolaris(): Boolean =
        System.getProperty("os.name") in sequenceOf("SunOS", "Solaris")

    fun isFreeBsd(): Boolean =
        System.getProperty("os.name") == "FreeBSD"

    fun isAix(): Boolean =
        System.getProperty("os.name") == "AIX"

    fun isHpUx(): Boolean =
        System.getProperty("os.name") == "HP-UX"

    actual fun isUnix(): Boolean =
        sequenceOf(
            OsFamily::isLinux,
            OsFamily::isMacOsX,
            OsFamily::isFreeBsd,
            OsFamily::isAix,
            OsFamily::isSolaris,
            OsFamily::isHpUx,
        ).any(KFunction0<Boolean>::invoke)

    actual fun isUnknown(): Boolean =
        !isWindows() && !isUnix()
}
