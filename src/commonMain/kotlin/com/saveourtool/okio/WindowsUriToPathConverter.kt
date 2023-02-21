package com.saveourtool.okio

import com.saveourtool.okio.UriToPathConverter.Companion.BACKSLASH
import com.saveourtool.okio.UriToPathConverter.Companion.SCHEME_FILE
import com.saveourtool.okio.UriToPathConverter.Companion.SLASH
import com.saveourtool.okio.UriToPathConverter.Companion.URI_UNC_PATH_PREFIX
import okio.Path
import okio.Path.Companion.toPath

internal object WindowsUriToPathConverter : UriToPathConverter {
    private const val UNC_PATH_PREFIX = "\\\\"
    private const val IPV6_LITERAL_SUFFIX = ".ipv6-literal.net"

    private fun toUri(
        absolutePath: String,
        isUnc: Boolean,
        addTrailingSlash: Boolean
    ): Uri {
        var uriHost: String
        var uriPath: String
        if (isUnc) {
            val backslashIndex = absolutePath.indexOf(BACKSLASH, startIndex = 2)
            uriHost = absolutePath.substring(2, backslashIndex)
            uriPath = absolutePath.substring(backslashIndex).slashify()

            // handle IPv6 literal addresses
            // 1. drop .ivp6-literal.net
            // 2. replace "-" with ":"
            // 3. replace "s" with "%" (zone/scopeID delimiter)
            if (uriHost.endsWith(IPV6_LITERAL_SUFFIX)) {
                uriHost = uriHost
                    .substring(0, uriHost.length - IPV6_LITERAL_SUFFIX.length)
                    .replace('-', ':')
                    .replace('s', '%')
            }
        } else {
            uriHost = ""
            uriPath = SLASH + absolutePath.slashify()
        }

        // append slash if known to be directory
        if (addTrailingSlash) {
            uriPath += SLASH
        }

        // return `file:///C:/My%20Documents` or `file://server/share/foo`
        return try {
            Uri(uriHost, uriPath)
        } catch (iae: IllegalArgumentException) {
            when {
                // if we get here it means we've got a UNC with reserved characters
                // in the server name. The authority component cannot contain escaped
                // octets so fallback to encoding the server name into the URI path
                // component.
                isUnc -> {
                    uriPath = URI_UNC_PATH_PREFIX + absolutePath.slashify()
                    if (addTrailingSlash) {
                        uriPath += SLASH
                    }
                    Uri(null, uriPath)
                }

                else -> throw iae
            }
        }
    }

    override fun Path.toFileUri(): Uri {
        val absolutePath = absolutePathString()

        // trailing slash will be added if file is a directory. Skip check if
        // already have trailing space
        val addTrailingSlash = !absolutePath.endsWith(BACKSLASH) &&
                isDirectory()

        return toUri(
            absolutePath = absolutePath,
            isUnc = isUnc(),
            addTrailingSlash = addTrailingSlash,
        )
    }

    override fun Uri.toLocalPath(): Path {
        require(isAbsolute) {
            "URI is not absolute"
        }
        var uriPath = path
        require(uriPath != null) {
            "URI is not hierarchical"
        }
        requireSchemeIs(SCHEME_FILE)
        require(uriPath.isNotEmpty()) {
            "URI path component is empty"
        }

        // UNC
        val auth = authority
        if (!auth.isNullOrEmpty()) {
            var host = host
            require(host != null) {
                "URI authority component has undefined host"
            }

            // IPv6 literal
            // 1. drop enclosing brackets
            // 2. replace ":" with "-"
            // 3. replace "%" with "s" (zone/scopeID delimiter)
            // 4. Append .ivp6-literal.net
            if (host.startsWith("[")) {
                host = host.substring(1, host.length - 1)
                    .replace(':', '-')
                    .replace('%', 's')
                host += IPV6_LITERAL_SUFFIX
            }

            // reconstitute the UNC
            uriPath = UNC_PATH_PREFIX + host + uriPath
        } else {
            if (uriPath.length > 2 && uriPath[2] == ':') {
                // "/c:/foo" --> "c:/foo"
                uriPath = uriPath.substring(1)
            }
        }
        return uriPath.backslashify().toPath()
    }

    private fun Path.isUnc(): Boolean =
        pathString.startsWith(UNC_PATH_PREFIX)
}
