@file:JvmName("PathUtils")

package com.saveourtool.okio

import okio.Path
import okio.Path.Companion.toPath
import kotlin.jvm.JvmName

/**
 * Checks if the file located by this path is a directory.
 */
fun Path.isDirectory(): Boolean =
    fileSystem.metadataOrNull(this)?.isDirectory == true

/**
 * Checks if the file located by this path is a regular file.
 */
@Suppress("unused")
fun Path.isRegularFile(): Boolean =
    fileSystem.metadataOrNull(this)?.isRegularFile == true

/**
 * Converts this possibly relative path to an absolute path.
 *
 * If this path is already [absolute][Path.isAbsolute],
 * returns this path unchanged.
 * Otherwise, resolves this path,
 * usually against the default directory of the file system.
 *
 * May throw an exception if the file system is inaccessible,
 * or getting the default directory path is prohibited.
 */
fun Path.absolute(): Path =
    when {
        isAbsolute -> this
        else -> {
            val currentDir = "".toPath()
            fileSystem.canonicalize(currentDir) / (this)
        }
    }

/**
 * Converts this possibly relative path to an absolute path,
 * and returns its string representation.
 *
 * Basically,
 * this method is a combination of calling [absolute] and [pathString].
 *
 * May throw an exception if the file system is inaccessible,
 * or getting the default directory path is prohibited.
 */
fun Path.absolutePathString(): String =
    absolute().pathString

/**
 * Returns the string representation of this path.
 *
 * The returned path string uses the default name separator to separate
 * names in the path.
 *
 * This property is a synonym to [Path.toString] function.
 */
val Path.pathString: String
    get() =
        toString()

fun Path.toFileUri(): Uri =
    with(UriToPathConverter) {
        toFileUri()
    }

fun Uri.toLocalPath(): Path =
    with(UriToPathConverter) {
        toLocalPath()
    }
