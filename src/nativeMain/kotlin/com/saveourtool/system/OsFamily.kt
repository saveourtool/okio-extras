package com.saveourtool.system

import kotlin.native.OsFamily.UNKNOWN
import kotlin.native.OsFamily.WASM
import kotlin.native.OsFamily.WINDOWS
import kotlin.native.OsFamily.values

actual object OsFamily {
    actual fun isWindows(): Boolean =
        Platform.osFamily == WINDOWS

    actual fun isUnix(): Boolean =
        Platform.osFamily in
                values().asSequence() - sequenceOf(WINDOWS, WASM, UNKNOWN)

    actual fun isUnknown(): Boolean =
        !isWindows() && !isUnix()

    actual fun osName(): String =
        Platform.osFamily.name
}
