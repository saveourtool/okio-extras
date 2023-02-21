@file:JvmName("CharJVM")

package com.saveourtool.text

internal actual fun Char.isSpaceChar(): Boolean =
    Character.isSpaceChar(this)
