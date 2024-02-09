package com.saveourtool.okio

import com.saveourtool.text.isAscii
import com.saveourtool.text.isSpaceCharOrIsoControl
import okio.Buffer

/**
 * @property scheme if `null`, this is a relative URI.
 * @property rawAuthority registry or server.
 * @property host if `null`, this is a registry-based URI.
 * @property rawPath if `null`, this is an opaque URI.
 */
class Uri internal constructor(
    val scheme: String?,
    val rawAuthority: String?,
    val host: String?,
    val rawPath: String?,
    rawSchemeSpecificPart: String? = null,
) {
    private val rawSchemeSpecificPart: String = rawSchemeSpecificPart ?: run {
        StringBuilder()
            .apply {
                appendSchemeSpecificPart(authority, host, path)
            }
            .toString()
    }

    fun normalize(): Uri =
        normalize(this)

    val isAbsolute: Boolean
        get() =
            scheme != null

    val isOpaque: Boolean
        get() =
            rawPath == null

    val schemeSpecificPart: String
        get() =
            decode(rawSchemeSpecificPart)

    val authority: String?
        get() =
            when (rawAuthority) {
                null -> null
                else -> decode(rawAuthority)
            }

    val path: String?
        get() =
            when (rawPath) {
                null -> null
                else -> decode(rawPath)
            }

    @Suppress("NestedBlockDepth")
    override fun toString(): String =
        StringBuilder()
            .apply {
                if (scheme != null) {
                    append(scheme)
                    append(':')
                }

                when (rawPath) {
                    null -> append(rawSchemeSpecificPart)

                    else -> {
                        when {
                            host != null -> {
                                append("//")
                                val needBrackets = (host.indexOf(':') >= 0
                                        && !host.startsWith("[")
                                        && !host.endsWith("]"))
                                if (needBrackets) {
                                    append('[')
                                }
                                append(host)
                                if (needBrackets) {
                                    append(']')
                                }
                            }

                            rawAuthority != null -> {
                                append("//")
                                append(rawAuthority)
                            }
                        }

                        append(rawPath)
                    }
                }
            }
            .toString()

    companion object {
        operator fun invoke(rawUri: String): Uri =
            UriParser(rawUri).parse(requireServerAuthority = false)

        operator fun invoke(
            host: String?,
            path: String?,
        ): Uri {
            val rawUri = rawUri(host, path)
            require(!path.isNullOrEmpty() && path[0] == SLASH) {
                "Relative path in absolute URI: $rawUri"
            }
            return UriParser(rawUri).parse(requireServerAuthority = true)
        }

        private fun rawUri(host: String?, path: String?): String =
            StringBuilder()
                .apply {
                    append(SCHEME_FILE)
                    append(':')
                    appendSchemeSpecificPart(
                        authority = null,
                        host = host,
                        path = path
                    )
                }
                .toString()

        @Suppress("NestedBlockDepth")
        private fun StringBuilder.appendAuthority(
            authority: String?,
            host: String?
        ) {
            when {
                host != null -> {
                    append("//")
                    val needBrackets = (host.indexOf(':') >= 0 &&
                            !host.startsWith("[") &&
                            !host.endsWith("]"))
                    if (needBrackets) {
                        append('[')
                    }
                    append(host)
                    if (needBrackets) {
                        append(']')
                    }
                }

                authority != null -> {
                    append("//")

                    when {
                        authority.startsWith("[") -> {
                            // authority should (but may not) contain an embedded IPv6 address
                            val endIndex = authority.indexOf(']')
                            var doquote = authority
                            var dontquote = ""

                            if (endIndex != -1 && authority.indexOf(':') != -1) {
                                // the authority contains an IPv6 address
                                when (endIndex) {
                                    authority.length -> {
                                        dontquote = authority
                                        doquote = ""
                                    }

                                    else -> {
                                        dontquote = authority.substring(0, endIndex + 1)
                                        doquote = authority.substring(endIndex + 1)
                                    }
                                }
                            }

                            append(dontquote)
                            append(doquote.quote(L_REG_NAME or L_SERVER, H_REG_NAME or H_SERVER))
                        }

                        else -> append(authority.quote(L_REG_NAME or L_SERVER, H_REG_NAME or H_SERVER))
                    }
                }
            }
        }

        private fun StringBuilder.appendSchemeSpecificPart(
            authority: String?,
            host: String?,
            path: String?
        ) {
            appendAuthority(authority, host)
            if (path != null) {
                append(path.quote(L_PATH, H_PATH))
            }
        }

        @Suppress("MagicNumber")
        private fun StringBuilder.appendEncoded(char: Char) {
            val bytes = char.toString().encodeToByteArray()
            bytes.forEach { signedByte ->
                val byte = signedByte.toInt() and 0xff
                when {
                    byte >= 0x80 -> appendEscape(byte.toByte())
                    else -> append(byte.toChar())
                }
            }
        }

        @Suppress("NestedBlockDepth")
        private fun String.quote(lowMask: Long, highMask: Long): String {
            var buffer: StringBuilder? = null
            val allowNonAscii = lowMask and L_ESCAPED != 0L

            for (index in indices) {
                val char = this[index]
                when {
                    char.isAscii() -> when {
                        match(char, lowMask, highMask) -> buffer?.append(char)

                        else -> {
                            if (buffer == null) {
                                buffer = StringBuilder()
                                buffer.append(this, 0, index)
                            }
                            buffer.appendEscape(char.code.toByte())
                        }
                    }

                    allowNonAscii && char.isSpaceCharOrIsoControl() -> {
                        if (buffer == null) {
                            buffer = StringBuilder()
                            buffer.append(this, 0, index)
                        }
                        buffer.appendEncoded(char)
                    }

                    else -> buffer?.append(char)
                }
            }

            return buffer?.toString() ?: this
        }

        @Suppress("MagicNumber")
        private fun decode(c: Char): Int =
            when (c) {
                in '0'..'9' -> c.code - '0'.code
                in 'a'..'f' -> c.code - 'a'.code + 10
                in 'A'..'F' -> c.code - 'A'.code + 10
                else -> {
                    check(false)
                    -1
                }
            }

        @Suppress("MagicNumber")
        private fun decode(c1: Char, c2: Char): Byte =
            (decode(c1) and 0xf shl 4 or
                    (decode(c2) and 0xf shl 0)).toByte()

        @Suppress("CyclomaticComplexMethod")
        private fun decode(raw: String): String {
            if (raw.indexOf('%') < 0) {
                return raw
            }

            val length = raw.length
            val buffer = StringBuilder(length)
            val bytes = Buffer()
            var char = raw[0]
            var betweenBrackets = false
            var index = 0

            @Suppress("LoopWithTooManyJumpStatements")
            while (index < length) {
                // Loop invariant
                check(char == raw[index])

                when {
                    char == '[' -> betweenBrackets = true
                    betweenBrackets && char == ']' -> betweenBrackets = false
                }

                if (char != '%' || betweenBrackets) {
                    buffer.append(char)
                    if (++index >= length) {
                        break
                    }
                    char = raw[index]
                    continue
                }

                bytes.clear()

                while (true) {
                    check(length - index >= 2)
                    val decoded: Byte = decode(raw[++index], raw[++index])
                    bytes.writeByte(decoded.toInt())
                    if (++index >= length) {
                        break
                    }
                    char = raw[index]
                    if (char != '%') {
                        break
                    }
                }

                buffer.append(bytes.readUtf8())
            }

            return buffer.toString()
        }
    }
}
