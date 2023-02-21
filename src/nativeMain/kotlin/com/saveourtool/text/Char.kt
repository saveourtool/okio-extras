package com.saveourtool.text

internal actual fun Char.isSpaceChar(): Boolean =
    when {
        isAscii() -> this == ' '
        else -> isWhitespace()
    }
