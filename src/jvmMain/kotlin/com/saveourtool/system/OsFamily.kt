package com.saveourtool.system

import kotlin.reflect.KFunction0

actual object OsFamily {
    actual fun isWindows(): Boolean =
        osNameOrNull.let { osName: String? ->
            osName != null && osName.startsWith("Windows")
        }

    fun isLinux(): Boolean =
        osNameOrNull == "Linux"

    fun isMacOsX(): Boolean =
        osNameOrNull == "Mac OS X"

    fun isSolaris(): Boolean =
        osNameOrNull in sequenceOf("SunOS", "Solaris")

    fun isFreeBsd(): Boolean =
        osNameOrNull == "FreeBSD"

    fun isAix(): Boolean =
        osNameOrNull == "AIX"

    fun isHpUx(): Boolean =
        osNameOrNull == "HP-UX"

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

    actual fun osName(): String =
        osNameOrNull + ' ' + System.getProperty("os.version")

    private val osNameOrNull: String?
        get() =
            System.getProperty("os.name")
}
