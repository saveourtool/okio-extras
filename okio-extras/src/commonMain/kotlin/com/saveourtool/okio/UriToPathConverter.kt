package com.saveourtool.okio

import com.saveourtool.system.OsFamily
import okio.Path

internal sealed interface UriToPathConverter {
    fun Uri.toLocalPath(): Path

    fun Path.toFileUri(): Uri

    companion object : UriToPathConverter {
        override fun Uri.toLocalPath(): Path =
            when {
                OsFamily.isWindows() -> WindowsUriToPathConverter
                OsFamily.isUnix() -> UnixUriToPathConverter
                else -> throw NotImplementedError("Path conversion not implemented for ${OsFamily.osName()}")
            }.run {
                toLocalPath()
            }

        override fun Path.toFileUri(): Uri =
            when {
                OsFamily.isWindows() -> WindowsUriToPathConverter
                OsFamily.isUnix() -> UnixUriToPathConverter
                else -> throw NotImplementedError("Path conversion not implemented for ${OsFamily.osName()}")
            }.run {
                toFileUri()
            }
    }
}
