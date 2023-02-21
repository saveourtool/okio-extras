package com.saveourtool.system

internal expect object OsFamily {
    fun isWindows(): Boolean

    fun isUnix(): Boolean

    fun isUnknown(): Boolean
}
