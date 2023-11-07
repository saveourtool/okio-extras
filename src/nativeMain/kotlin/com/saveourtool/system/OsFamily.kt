package com.saveourtool.system

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.OsFamily
import kotlin.native.OsFamily.UNKNOWN
import kotlin.native.OsFamily.WASM
import kotlin.native.OsFamily.WINDOWS

@OptIn(ExperimentalNativeApi::class)
actual fun isWindowsInternal(): Boolean =
    Platform.osFamily == WINDOWS

@OptIn(ExperimentalNativeApi::class)
actual fun isUnixInternal(): Boolean =
    Platform.osFamily in
            OsFamily.entries.asSequence() - sequenceOf(WINDOWS, WASM, UNKNOWN)

actual fun isUnknownInternal(): Boolean =
    !isWindowsInternal() && !isUnixInternal()

@OptIn(ExperimentalNativeApi::class)
actual fun osNameInternal(): String =
    Platform.osFamily.name
