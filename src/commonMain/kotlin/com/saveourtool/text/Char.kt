@file:JvmName("Char")

package com.saveourtool.text

import kotlin.jvm.JvmName

internal fun Char.isSpaceCharOrIsoControl(): Boolean =
    isSpaceChar() || isISOControl()

internal expect fun Char.isSpaceChar(): Boolean

internal fun Char.isAscii(): Boolean =
    this < '\u0080'
