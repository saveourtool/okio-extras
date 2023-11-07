package com.saveourtool.system

object OsFamily {
    /**
     * @return `true` if the current operating system is a _Windows_, `false`
     *   otherwise.
     */
    fun isWindows(): Boolean = isWindowsInternal()

    /**
     * @return `true` if the current operating system is a _UNIX_, `false`
     *   otherwise.
     */
    fun isUnix(): Boolean = isUnixInternal()

    /**
     * @return `true` if the current operating system is neither a _UNIX_ nor
     *   a _Windows_.
     */
    fun isUnknown(): Boolean = isUnknownInternal()

    /**
     * @return the name (and version, if known) of the current operating system.
     */
    fun osName(): String = osNameInternal()
}

/**
 * @return `true` if the current operating system is a _Windows_, `false`
 *   otherwise.
 */
internal expect fun isWindowsInternal(): Boolean

/**
 * @return `true` if the current operating system is a _UNIX_, `false`
 *   otherwise.
 */
internal expect fun isUnixInternal(): Boolean

/**
 * @return `true` if the current operating system is neither a _UNIX_ nor
 *   a _Windows_.
 */
internal expect fun isUnknownInternal(): Boolean

/**
 * @return the name (and version, if known) of the current operating system.
 */
internal expect fun osNameInternal(): String


