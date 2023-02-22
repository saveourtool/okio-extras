package com.saveourtool.system

expect object OsFamily {
    /**
     * @return `true` if the current operating system is a _Windows_, `false`
     *   otherwise.
     */
    fun isWindows(): Boolean

    /**
     * @return `true` if the current operating system is a _UNIX_, `false`
     *   otherwise.
     */
    fun isUnix(): Boolean

    /**
     * @return `true` if the current operating system is neither a _UNIX_ nor
     *   a _Windows_.
     */
    fun isUnknown(): Boolean

    /**
     * @return the name (and version, if known) of the current operating system.
     */
    fun osName(): String
}
