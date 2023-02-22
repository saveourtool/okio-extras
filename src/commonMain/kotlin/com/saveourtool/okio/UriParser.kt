package com.saveourtool.okio

import com.saveourtool.text.isAscii
import com.saveourtool.text.isSpaceCharOrIsoControl

@Suppress("TooManyFunctions")
internal class UriParser(private val rawUri: String) {
    private val builder = UriBuilder()

    private var ipv6byteCount = 0

    fun parse(requireServerAuthority: Boolean): Uri {
        val length = rawUri.length
        var index = scan(0, length, "/?#", ":")
        index = when {
            index >= 0 && at(index, length, ':') -> {
                if (index == 0) {
                    failExpecting("scheme name", 0)
                }
                checkChar(0, L_ALPHA, H_ALPHA, "scheme name")
                checkChars(1, index, L_SCHEME, H_SCHEME, "scheme name")
                builder.withScheme(rawUri.substring(0, index))
                index++ // Skip ':'
                when {
                    at(index, length, SLASH) -> parseHierarchical(requireServerAuthority, index, length)

                    // opaque; need to create the schemeSpecificPart
                    else -> {
                        val q = scan(index, length, "#")
                        if (q <= index) failExpecting(
                            "scheme-specific part",
                            index
                        )
                        checkChars(
                            index,
                            q,
                            L_URIC,
                            H_URIC,
                            "opaque part"
                        )
                        builder.withRawSchemeSpecificPart(rawUri.substring(index, q))
                        q
                    }
                }
            }

            else -> parseHierarchical(requireServerAuthority, 0, length)
        }

        if (at(index, length, '#')) {
            checkChars(index + 1, length, L_URIC, H_URIC, "fragment")
            val fragment = rawUri.substring(index + 1, length)
            fail("URI fragments (\"$fragment\") are not supported", index)
        }

        if (index < length) {
            fail("end of URI", index)
        }

        return builder.build()
    }

    private fun parseHierarchical(
        requireServerAuthority: Boolean,
        startIndex: Int,
        n: Int
    ): Int {
        var index = startIndex
        if (at(index, n, SLASH) && at(index + 1, n, SLASH)) {
            index += 2
            val q = scan(index, n, "/?#")

            when {
                q > index -> {
                    index = parseAuthority(requireServerAuthority, index, q)
                }

                // DEVIATION: Allow empty authority prior to non-empty
                // path, query component or fragment identifier
                q < n -> Unit

                else -> failExpecting("authority", index)
            }
        }

        var q = scan(index, n, "?#") // DEVIATION: May be empty
        checkChars(index, q, L_PATH, H_PATH, "path")
        builder.withRawPath(rawUri.substring(index, q))
        index = q

        if (at(index, n, '?')) {
            index++
            q = scan(index, n, "#")
            checkChars(index, q, L_URIC, H_URIC, "query")
            index = q
        }

        return index
    }

    private fun parseAuthority(
        requireServerAuthority: Boolean,
        startIndex: Int,
        n: Int
    ): Int {
        val serverChars: Boolean = when {
            // contains a literal IPv6 address, therefore % is allowed
            scan(startIndex, n, "]") > startIndex -> scan(startIndex, n, L_SERVER_PERCENT, H_SERVER_PERCENT) == n

            else -> scan(startIndex, n, L_SERVER, H_SERVER) == n
        }

        val regChars: Boolean = scan(startIndex, n, L_REG_NAME, H_REG_NAME) == n

        if (regChars && !serverChars) {
            // Must be a registry-based authority
            builder.withRawAuthority(rawUri.substring(startIndex, n))
            return n
        }

        var index = startIndex
        var error: IllegalArgumentException? = null

        if (serverChars) {
            // Might be (probably is) a server-based authority, so attempt
            // to parse it as such.  If the attempt fails, try to treat it
            // as a registry-based authority.
            try {
                index = parseServer(startIndex, n)
                if (index < n) {
                    failExpecting("end of authority", index)
                }
                builder.withRawAuthority(rawUri.substring(startIndex, n))
            } catch (iae: IllegalArgumentException) {
                // Undo results of failed parse
                builder.withHost(null)
                if (requireServerAuthority) {
                    // If we're insisting upon a server-based authority,
                    // then just re-throw the exception
                    throw iae
                }

                // Save the exception in case it doesn't parse as a
                // registry either
                error = iae
                index = startIndex
            }
        }

        if (index < n) {
            when {
                // Registry-based authority
                regChars -> builder.withRawAuthority(rawUri.substring(startIndex, n))

                error == null -> fail("Illegal character in authority", index)

                // Re-throw exception; it was probably due to
                // a malformed IPv6 address
                else -> throw error
            }
        }

        return n
    }

    private fun parseServer(startIndex: Int, n: Int): Int {
        var index = startIndex

        // userinfo
        var q = scan(index, n, "/?#", "@")
        if (q >= index && at(q, n, '@')) {
            checkChars(index, q, L_USERINFO, H_USERINFO, "user info")
            index = q + 1 // Skip '@'
        }

        // hostname, IPv4 address, or IPv6 address
        when {
            at(index, n, '[') -> {
                // DEVIATION from RFC2396: Support IPv6 addresses, per RFC2732
                index++
                q = scan(index, n, "/?#", "]")
                index = when {
                    q > index && at(q, n, ']') -> {
                        // look for a "%" scope id
                        val r = scan(index, q, "%")

                        when {
                            r > index -> {
                                parseIPv6Reference(index, r)

                                if (r + 1 == q) {
                                    fail("scope id expected")
                                }

                                checkChars(r + 1, q, L_SCOPE_ID, H_SCOPE_ID, "scope id")
                            }

                            else -> parseIPv6Reference(index, q)
                        }

                        builder.withHost(rawUri.substring(index - 1, q + 1))
                        q + 1
                    }

                    else -> failExpecting("closing bracket for IPv6 address", q)
                }
            }

            else -> {
                q = parseIPv4Address(index, n)
                if (q <= index) {
                    q = parseHostname(index, n)
                }
                index = q
            }
        }

        // port
        if (at(index, n, ':')) {
            index++
            q = scan(index, n, "/")
            if (q > index) {
                checkChars(
                    index,
                    q,
                    L_DIGIT,
                    H_DIGIT,
                    "port number"
                )
                index = q
            }
        }

        return when {
            index < n -> failExpecting("port number", index)

            else -> index
        }
    }

    private fun parseIPv6Reference(startIndex: Int, n: Int) {
        var index = startIndex
        var compressedZeros = false
        val q = scanHexSeq(index, n)

        when {
            q > index -> {
                index = q
                when {
                    at(index, n, "::") -> {
                        compressedZeros = true
                        index = scanHexPost(index + 2, n)
                    }

                    at(index, n, ':') -> {
                        index = takeIPv4Address(index + 1, n, "IPv4 address")
                        ipv6byteCount += 4
                    }
                }
            }

            at(index, n, "::") -> {
                compressedZeros = true
                index = scanHexPost(index + 2, n)
            }
        }

        if (index < n) {
            fail("Malformed IPv6 address", startIndex)
        }

        if (ipv6byteCount > 16) {
            fail("IPv6 address too long", startIndex)
        }

        if (!compressedZeros && ipv6byteCount < 16) {
            fail("IPv6 address too short", startIndex)
        }

        if (compressedZeros && ipv6byteCount == 16) {
            fail("Malformed IPv6 address", startIndex)
        }
    }

    private fun parseIPv4Address(startIndex: Int, n: Int): Int {
        var index = try {
            scanIPv4Address(startIndex, n, false)
        } catch (_: IllegalArgumentException) {
            return -1
        }

        if (index in (startIndex + 1) until n) {
            // IPv4 address is followed by something - check that
            // it's a ":" as this is the only valid character to
            // follow an address.
            if (rawUri[index] != ':') {
                index = -1
            }
        }

        if (index > startIndex) {
            builder.withHost(rawUri.substring(startIndex, index))
        }

        return index
    }

    private fun parseHostname(startIndex: Int, n: Int): Int {
        var index = startIndex
        var q: Int
        var l = -1 // Start of last parsed label
        do {
            // domainlabel = alphanum [ *( alphanum | "-" ) alphanum ]
            q = scan(index, n, L_ALPHANUM, H_ALPHANUM)
            if (q <= index) {
                break
            }

            l = index

            index = q
            q = scan(index, n, L_ALPHANUM or L_DASH, H_ALPHANUM or H_DASH)
            if (q > index) {
                if (rawUri[q - 1] == '-') {
                    fail("Illegal character in hostname", q - 1)
                }
                index = q
            }

            q = scan(index, n, '.')

            if (q <= index) {
                break
            }

            index = q
        } while (index < n)

        if (index < n && !at(index, n, ':')) {
            fail("Illegal character in hostname", index)
        }

        if (l < 0) {
            failExpecting("hostname", startIndex)
        }

        // for a fully qualified hostname check that the rightmost
        // label starts with an alpha character.
        if (l > startIndex && !match(rawUri[l], L_ALPHA, H_ALPHA)) {
            fail("Illegal character in hostname", l)
        }

        builder.withHost(rawUri.substring(startIndex, index))
        return index
    }

    private fun scanHexPost(startIndex: Int, n: Int): Int {
        var index = startIndex

        if (index == n) {
            return index
        }

        val q = scanHexSeq(index, n)

        when {
            q > index -> {
                index = q
                if (at(index, n, ':')) {
                    index++
                    index = takeIPv4Address(index, n, "hex digits or IPv4 address")
                    ipv6byteCount += 4
                }
            }

            else -> {
                index = takeIPv4Address(index, n, "hex digits or IPv4 address")
                ipv6byteCount += 4
            }
        }

        return index
    }

    private fun takeIPv4Address(
        startIndex: Int,
        n: Int,
        expected: String
    ): Int {
        val index = scanIPv4Address(startIndex, n, true)

        return when {
            index <= startIndex -> failExpecting(expected, startIndex)

            else -> index
        }
    }

    private fun scanIPv4Address(startIndex: Int, n: Int, strict: Boolean): Int {
        var index = startIndex
        var q: Int
        val m = scan(index, n, L_DIGIT or L_DOT, H_DIGIT or H_DOT)

        if (m <= index || strict && m != n) {
            return -1
        }

        while (true) {
            // Per RFC2732: At most three digits per byte
            // Further constraint: Each element fits in a byte
            q = scanByte(index, m)
            if (q <= index) {
                break
            }
            index = q
            q = scan(index, m, '.')
            if (q <= index) {
                break
            }
            index = q
            q = scanByte(index, m)
            if (q <= index) {
                break
            }
            index = q
            q = scan(index, m, '.')
            if (q <= index) {
                break
            }
            index = q
            q = scanByte(index, m)
            if (q <= index) {
                break
            }
            index = q
            q = scan(index, m, '.')
            if (q <= index) {
                break
            }
            index = q
            q = scanByte(index, m)
            if (q <= index || q < m) {
                break
            }
            return q
        }

        fail("Malformed IPv4 address", q)
    }

    private fun at(startIndex: Int, endIndex: Int, char: Char): Boolean =
        startIndex < endIndex && rawUri[startIndex] == char

    private fun at(startIndex: Int, endIndex: Int, string: String): Boolean {
        var index = startIndex
        val length = string.length
        if (length > endIndex - index) {
            return false
        }
        var i = 0
        while (i < length) {
            if (rawUri[index++] != string[i]) {
                break
            }
            i++
        }
        return i == length
    }

    private fun scan(startIndex: Int, endIndex: Int, char: Char): Int =
        when {
            startIndex < endIndex && rawUri[startIndex] == char -> startIndex + 1
            else -> startIndex
        }

    private fun scan(
        startIndex: Int,
        endIndex: Int,
        err: String,
        stop: String
    ): Int {
        var index = startIndex
        while (index < endIndex) {
            val c = rawUri[index]
            if (err.indexOf(c) >= 0) {
                return -1
            }
            if (stop.indexOf(c) >= 0) {
                break
            }
            index++
        }
        return index
    }

    private fun scan(startIndex: Int, endIndex: Int, stop: String): Int {
        var index = startIndex
        while (index < endIndex) {
            val c = rawUri[index]
            if (stop.indexOf(c) >= 0) {
                break
            }
            index++
        }
        return index
    }

    private fun scanEscape(startIndex: Int, n: Int, first: Char): Int {
        if (first == '%') {
            // Process escape pair
            return when {
                startIndex + 3 <= n
                        && match(rawUri[startIndex + 1], L_HEX, H_HEX)
                        && match(rawUri[startIndex + 2], L_HEX, H_HEX) -> startIndex + 3

                else -> fail("Malformed escape pair", startIndex)
            }
        }

        return when {
            first.isAscii() || first.isSpaceCharOrIsoControl() -> startIndex

            // Allow unescaped but visible non-US-ASCII chars
            else -> startIndex + 1
        }
    }

    private fun scan(
        startIndex: Int,
        n: Int,
        lowMask: Long,
        highMask: Long
    ): Int {
        var index = startIndex
        while (index < n) {
            val c = rawUri[index]
            if (match(c, lowMask, highMask)) {
                index++
                continue
            }
            if (lowMask and L_ESCAPED != 0L) {
                val q = scanEscape(index, n, c)
                if (q > index) {
                    index = q
                    continue
                }
            }
            break
        }
        return index
    }

    private fun checkChars(
        startIndex: Int,
        endIndex: Int,
        lowMask: Long,
        highMask: Long,
        what: String
    ) {
        val index = scan(startIndex, endIndex, lowMask, highMask)
        if (index < endIndex) {
            fail("Illegal character in $what", index)
        }
    }

    private fun checkChar(
        index: Int,
        lowMask: Long,
        highMask: Long,
        what: String
    ) =
        checkChars(index, index + 1, lowMask, highMask, what)

    private fun scanByte(startIndex: Int, n: Int): Int {
        val index = scan(startIndex, n, L_DIGIT, H_DIGIT)

        return when {
            index <= startIndex -> index

            rawUri.substring(startIndex, index).toInt() > 255 -> startIndex

            else -> index
        }
    }

    private fun scanHexSeq(startIndex: Int, n: Int): Int {
        var index = startIndex
        var q = scan(index, n, L_HEX, H_HEX)

        if (q <= index) {
            return -1
        }

        if (at(q, n, '.')) {
            // Beginning of IPv4 address
            return -1
        }

        if (q > index + 4) {
            fail("IPv6 hexadecimal digit sequence too long", index)
        }

        ipv6byteCount += 2
        index = q

        while (index < n) {
            if (!at(index, n, ':')) {
                break
            }

            if (at(index + 1, n, ':')) {
                break // "::"
            }

            index++
            q = scan(index, n, L_HEX, H_HEX)

            if (q <= index) {
                failExpecting("digits for an IPv6 address", index)
            }

            if (at(q, n, '.')) {    // Beginning of IPv4 address
                index--
                break
            }

            if (q > index + 4) {
                fail("IPv6 hexadecimal digit sequence too long", index)
            }

            ipv6byteCount += 2
            index = q
        }

        return index
    }

    private fun fail(reason: String): Nothing =
        throw IllegalArgumentException("$reason: $rawUri")

    private fun fail(reason: String, index: Int): Nothing =
        throw IllegalArgumentException("$reason at index $index: $rawUri")

    private fun failExpecting(expected: String, index: Int): Nothing =
        fail("Expected $expected", index)
}
