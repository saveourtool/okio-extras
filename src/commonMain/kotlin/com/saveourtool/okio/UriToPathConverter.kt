package com.saveourtool.okio

import com.saveourtool.system.OsFamily
import okio.Path

internal sealed interface UriToPathConverter {
    fun Uri.toLocalPath(): Path

    fun Path.toFileUri(): Uri

    companion object : UriToPathConverter {
        const val SLASH = '/'
        const val BACKSLASH = '\\'
        const val SCHEME_FILE = "file"
        const val URI_UNC_PATH_PREFIX = "//"

        override fun Uri.toLocalPath(): Path =
            when {
                OsFamily.isWindows() -> WindowsUriToPathConverter
                else -> UnixUriToPathConverter
            }.run {
                toLocalPath()
            }

        override fun Path.toFileUri(): Uri =
            when {
                OsFamily.isWindows() -> WindowsUriToPathConverter
                else -> UnixUriToPathConverter
            }.run {
                toFileUri()
            }
    }
}
