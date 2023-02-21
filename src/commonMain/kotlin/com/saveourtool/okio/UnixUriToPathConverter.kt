package com.saveourtool.okio

import com.saveourtool.okio.UriToPathConverter.Companion.SCHEME_FILE
import com.saveourtool.okio.UriToPathConverter.Companion.SLASH
import com.saveourtool.text.isAscii
import okio.Path
import okio.Path.Companion.toPath
import kotlin.math.max
import kotlin.math.min

@Suppress("TooManyFunctions")
internal object UnixUriToPathConverter : UriToPathConverter {
    private val L_DIGIT = lowMask('0', '9')
    private const val H_DIGIT = 0L
    private const val L_UPALPHA = 0L
    private val H_UPALPHA = highMask('A', 'Z')
    private const val L_LOWALPHA = 0L
    private val H_LOWALPHA = highMask('a', 'z')
    private const val L_ALPHA = L_LOWALPHA or L_UPALPHA
    private val H_ALPHA = H_LOWALPHA or H_UPALPHA
    private val L_ALPHANUM = L_DIGIT or L_ALPHA
    private val H_ALPHANUM = H_DIGIT or H_ALPHA
    private val L_MARK = lowMask("-_.!~*'()")
    private val H_MARK = highMask("-_.!~*'()")
    private val L_UNRESERVED = L_ALPHANUM or L_MARK
    private val H_UNRESERVED = H_ALPHANUM or H_MARK
    private val L_PCHAR = L_UNRESERVED or lowMask(":@&=+$,")
    private val H_PCHAR = H_UNRESERVED or highMask(":@&=+$,")
    private val L_PATH = L_PCHAR or lowMask(";/")
    private val H_PATH = H_PCHAR or highMask(";/")

    @Suppress("MagicNumber")
    override fun Uri.toLocalPath(): Path {
        require(isAbsolute) {
            "URI is not absolute"
        }
        val rawPath = rawPath
        require(rawPath != null) {
            "URI is not hierarchical"
        }
        requireSchemeIs(SCHEME_FILE)
        require(authority == null) {
            "URI has an authority component"
        }
        require(rawPath.isNotEmpty()) {
            "URI path component is empty"
        }

        return when {
            toString().startsWith("$SCHEME_FILE:///") -> {
                var length = rawPath.length

                if (rawPath.endsWith(SLASH) && length > 1) {
                    length--
                }

                var result = ByteArray(length)
                var resultSize = 0
                var index = 0
                while (index < length) {
                    val byte = when (val char = rawPath[index++]) {
                        '%' -> {
                            check(index + 2 <= length)

                            val c1 = rawPath[index++]
                            val c2 = rawPath[index++]
                            (decode(c1) shl 4 or decode(c2)).toByte().also {
                                require(it.toInt() != 0) {
                                    "Nul character not allowed"
                                }
                            }
                        }

                        else -> {
                            require(char != NUL && char.isAscii()) {
                                "Bad escape"
                            }

                            char.code.toByte()
                        }
                    }

                    result[resultSize++] = byte
                }

                if (resultSize != result.size) {
                    result = result.copyOf(newSize = resultSize)
                }

                result.decodeToString().toPath()
            }

            else -> absoluteToNioPath()
        }
    }

    @Suppress("MagicNumber")
    override fun Path.toFileUri(): Uri {
        val pathBytes = absolutePathString().encodeToByteArray()
        val buffer = StringBuilder("$SCHEME_FILE:///")
        check(pathBytes[0].toInt() == SLASH.code)

        for (index in 1 until pathBytes.size) {
            val char = (pathBytes[index].toInt() and 0xff).toChar()

            when {
                match(char, L_PATH, H_PATH) -> buffer.append(char)
                else -> buffer.appendEscape(char.code.toByte())
            }
        }

        if (!buffer.endsWith(SLASH) && isDirectory()) {
            buffer.append(SLASH)
        }

        return Uri(buffer.toString())
    }

    /**
     * Converts an URI which doesn't start with `file:///`.
     */
    private fun Uri.absoluteToNioPath(): Path {
        val uriPath = path
        check(uriPath != null)

        return fromUriPath(uriPath).normalizeUnixPath().toPath()
    }

    private fun String.normalizeUnixPath(): String {
        val doubleSlashIndex = indexOf("//")

        return when {
            doubleSlashIndex >= 0 -> normalize(offset = doubleSlashIndex)

            endsWith(SLASH) -> normalize(offset = length - 1)

            else -> this
        }
    }

    private fun String.normalize(offset: Int): String {
        var effectiveLength = length
        while (effectiveLength > offset && this[effectiveLength - 1] == SLASH) {
            --effectiveLength
        }

        return when (effectiveLength) {
            0 -> "$SLASH"

            offset -> substring(0, offset)

            else -> {
                val buffer = StringBuilder(effectiveLength)
                if (offset > 0) {
                    buffer.append(this, 0, offset)
                }
                var previousChar = 0.toChar()
                for (index in offset until effectiveLength) {
                    val char = this[index]
                    if (previousChar != SLASH || char != SLASH) {
                        buffer.append(char)
                        previousChar = char
                    }
                }
                buffer.toString()
            }
        }
    }

    /**
     * Converts a URI path component to a UNIX path.
     */
    private fun fromUriPath(uriPath: String): String =
        when {
            uriPath.endsWith(SLASH) && uriPath.length > 1 -> uriPath.substring(0, uriPath.length - 1)

            else -> uriPath
        }

    @Suppress("MagicNumber")
    private fun lowMask(chars: String): Long {
        var mask = 0L
        for (char in chars) {
            if (char.code < 64) {
                mask = mask or (1L shl char.code)
            }
        }
        return mask
    }

    @Suppress("MagicNumber")
    private fun highMask(chars: String): Long {
        var mask = 0L
        for (char in chars) {
            if (char.code in 64 until '\u0080'.code) {
                mask = mask or (1L shl char.code) - 64
            }
        }
        return mask
    }

    @Suppress("MagicNumber")
    private fun lowMask(first: Char, last: Char): Long {
        var mask = 0L
        val first0 = max(min(first.code, 63), 0)
        val last0 = max(min(last.code, 63), 0)
        for (i in first0..last0) {
            mask = mask or (1L shl i)
        }
        return mask
    }

    @Suppress("MagicNumber")
    private fun highMask(first: Char, last: Char): Long {
        var mask = 0L
        val first0 = max(min(first.code, 127), 64) - 64
        val last0 = max(min(last.code, 127), 64) - 64
        for (i in first0..last0) {
            mask = mask or (1L shl i)
        }
        return mask
    }

    @Suppress("MagicNumber")
    private fun match(char: Char, lowMask: Long, highMask: Long): Boolean =
        when {
            char.code < 64 -> 1L shl char.code and lowMask != 0L
            char.isAscii() -> 1L shl char.code - 64 and highMask != 0L
            else -> false
        }

    @Suppress("MagicNumber")
    private fun decode(c: Char): Int =
        when (c) {
            in '0'..'9' -> c.code - '0'.code
            in 'a'..'f' -> c.code - 'a'.code + 10
            in 'A'..'F' -> c.code - 'A'.code + 10
            else -> throw AssertionError()
        }
}
