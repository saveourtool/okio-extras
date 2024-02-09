@file:JvmName("PathUtils")
@file:Suppress("TooManyFunctions")

package com.saveourtool.okio

import okio.IOException
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

/**
 * Returns the _real_ path of an existing file.
 *
 * If this path is relative then its absolute path is first obtained, as if by
 * invoking the [Path.absolute] method.
 *
 * @return an absolute path represent the _real_ path of the file located by
 *   this object.
 * @throws IOException if the file does not exist or an I/O error occurs.
 * @see Path.safeToRealPath
 */
@Throws(IOException::class)
fun Path.toRealPath(): Path =
    fileSystem.canonicalize(this)

/**
 * Same as [Path.toRealPath], but doesn't throw an exception if the path doesn't
 * exist.
 *
 * @return an absolute path represent the _real_ path of the file located by
 *   this object, or an absolute normalized path if the file doesn't exist.
 * @see Path.toRealPath
 */
fun Path.safeToRealPath(): Path =
    try {
        toRealPath()
    } catch (_: IOException) {
        absolute().normalized()
    }

/**
 * Checks if the file located by this path points to the same file or directory
 * as [other].
 *
 * @param other the other path.
 * @return `true` if, and only if, the two paths locate the same file.
 * @throws IOException if an I/O error occurs.
 * @see Path.safeIsSameFileAs
 */
@Throws(IOException::class)
fun Path.isSameFileAs(other: Path): Boolean =
    this.toRealPath() == other.toRealPath()

/**
 * Checks if the file located by this path points to the same file or directory
 * as [other].
 * Same as [Path.isSameFileAs], but doesn't throw an exception if any of the
 * paths doesn't exist.
 *
 * @param other the other path.
 * @return `true` if the two paths locate the same file.
 * @see Path.isSameFileAs
 */
fun Path.safeIsSameFileAs(other: Path): Boolean =
    try {
        this.isSameFileAs(other)
    } catch (_: IOException) {
        this.safeToRealPath() == other.safeToRealPath()
    }

/**
 * Creates a directory, ensuring that all nonexistent parent directories exist
 * by creating them first.
 *
 * If the directory already exists, this function does not throw an exception.
 *
 * @return this path.
 * @throws IOException if an I/O error occurs.
 */
@Suppress("unused")
@Throws(IOException::class)
fun Path.createDirectories(): Path {
    fileSystem.createDirectories(this)
    return this
}

/**
 * Same as [Path.relativeTo], but doesn't throw an [IllegalArgumentException] if
 * `this` and [other] are both absolute paths, but have different file system
 * roots.
 *
 * @param other the other path.
 * @return this path relativized against [other],
 *   or `this` if this and other have different file system roots.
 */
@Suppress("unused")
fun Path.safeRelativeTo(other: Path): Path =
    try {
        relativeTo(other)
    } catch (_: IllegalArgumentException) {
        this
    }

/**
 * Converts this path to a `file://` URI.
 *
 * UNC paths, such as `\\WSL$\Debian\etc\passwd` or
 * `\\--1.ipv6-literal.net\C$\Windows`, are supported on _Windows_.
 *
 * @receiver the local or UNC path to convert.
 * @return the `file://` URI which corresponds to this path.
 * @see Uri.toLocalPath
 */
fun Path.toFileUri(): Uri =
    with(UriToPathConverter) {
        toFileUri()
    }

/**
 * Converts this `file://` URI to a local or UNC path.
 *
 * @receiver the `file://` URI to convert.
 * @return the local or UNC path which corresponds to this URI.
 * @see Path.toFileUri
 */
fun Uri.toLocalPath(): Path =
    with(UriToPathConverter) {
        toLocalPath()
    }
