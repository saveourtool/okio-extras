@file:JvmName("UriNormalizationUtils")
@file:Suppress("TooManyFunctions")

package com.saveourtool.okio

import kotlin.jvm.JvmName

internal const val NUL = '\u0000'

internal fun normalize(original: Uri): Uri {
    val rawPath = original.rawPath
    if (rawPath.isNullOrEmpty()) {
        return original
    }

    return when (val normalizedPath = normalize(rawPath)) {
        rawPath -> original

        else -> Uri(
            original.scheme,
            original.rawAuthority,
            original.host,
            normalizedPath,
        )
    }
}

private fun needsNormalization(path: String): Int {
    var normal = true
    var segmentCount = 0 // Number of segments
    val endIndex = path.length - 1 // Index of last char in path
    var index = 0 // Index of next char in path

    // Skip initial slashes
    while (index <= endIndex) {
        if (path[index] != SLASH) {
            break
        }
        index++
    }
    if (index > 1) {
        normal = false
    }

    // Scan segments
    while (index <= endIndex) {
        // Looking at "." or ".." ?
        if (path.hasDotAtIndex(index) || path.hasTwoDotsAtIndex(index)) {
            normal = false
        }
        segmentCount++

        // Find beginning of next segment
        while (index <= endIndex) {
            if (path[index++] != SLASH) {
                continue
            }

            // Skip redundant slashes
            while (index <= endIndex) {
                if (path[index] != SLASH) {
                    break
                }
                normal = false
                index++
            }
            break
        }
    }
    return if (normal) -1 else segmentCount
}

private fun normalize(path: String): String {
    // Does this path need normalization?
    val segmentCount = needsNormalization(path) // Number of segments
    if (segmentCount < 0) {
        // Nope -- just return it
        return path
    }
    val pathChars = path.toCharArray() // Path in char-array form

    // Split path into segments
    val segments = IntArray(segmentCount) // Segment-index array
    split(pathChars, segments)

    // Remove dots
    removeDots(pathChars, segments)

    // Prevent scheme-name confusion
    maybeAddLeadingDot(pathChars, segments)

    // Join the remaining segments and return the result
    val normalizedPath = pathChars.concatToString(
        startIndex = 0,
        endIndex = join(pathChars, segments),
    )
    return when (normalizedPath) {
        // string was already normalized
        path -> path
        else -> normalizedPath
    }
}

private fun split(path: CharArray, segments: IntArray) {
    val endIndex = path.size - 1 // Index of last char in path
    var p = 0 // Index of next char in path
    var i = 0 // Index of current segment

    // Skip initial slashes
    while (p <= endIndex) {
        if (path[p] != SLASH) {
            break
        }
        path[p] = NUL
        p++
    }
    while (p <= endIndex) {

        // Note start of segment
        segments[i++] = p++

        // Find beginning of next segment
        while (p <= endIndex) {
            if (path[p++] != SLASH) continue
            path[p - 1] = NUL

            // Skip redundant slashes
            while (p <= endIndex) {
                if (path[p] != SLASH) break
                path[p++] = NUL
            }
            break
        }
    }
    check(i == segments.size)
}

/**
 * Not idempotent: mutates [path].
 */
private fun join(path: CharArray, segments: IntArray): Int {
    val endIndex = path.size - 1 // Index of last char in path
    var p = 0 // Index of next path char to write
    if (path[p] == NUL) {
        // Restore initial slash for absolute paths
        path[p++] = SLASH
    }
    for (segment in segments) {
        var q = segment // Current segment
        if (q == -1) // Ignore this segment
            continue
        check(p <= q)
        if (p == q) {
            // We're already at this segment, so just skip to its end
            while (p <= endIndex && path[p] != NUL) p++
            if (p <= endIndex) {
                // Preserve trailing slash
                path[p++] = SLASH
            }
        } else if (p < q) {
            // Copy q down to p
            while (q <= endIndex && path[q] != NUL) path[p++] = path[q++]
            if (q <= endIndex) {
                // Preserve trailing slash
                path[p++] = SLASH
            }
        }
    }
    return p
}

private fun removeDots(path: CharArray, segments: IntArray) {
    val segmentCount = segments.size
    val endIndex = path.size - 1
    var i = 0
    while (i < segmentCount) {
        var dots = 0 // Number of dots found (0, 1, or 2)

        // Find next occurrence of "." or ".."
        do {
            val segment = segments[i]
            if (path[segment] == '.') {
                if (segment == endIndex) {
                    dots = 1
                    break
                }
                if (path[segment + 1] == NUL) {
                    dots = 1
                    break
                }
                if (path[segment + 1] == '.' &&
                    (segment + 1 == endIndex ||
                            path[segment + 2] == NUL)
                ) {
                    dots = 2
                    break
                }
            }
            i++
        } while (i < segmentCount)
        if (i > segmentCount || dots == 0) break
        if (dots == 1) {
            // Remove this occurrence of "."
            segments[i] = -1
        } else {
            // If there is a preceding non-".." segment, remove both that
            // segment and this occurrence of ".."; otherwise, leave this
            // ".." segment as-is.
            var j = i - 1
            while (j >= 0) {
                if (segments[j] != -1) break
                j--
            }
            if (j >= 0) {
                val segment = segments[j]
                if (!(path[segment] == '.' &&
                            path[segment + 1] == '.' &&
                            path[segment + 2] == NUL)
                ) {
                    segments[i] = -1
                    segments[j] = -1
                }
            }
        }
        i++
    }
}

private fun maybeAddLeadingDot(path: CharArray, segments: IntArray) {
    if (path[0] == NUL) // The path is absolute
        return
    val segmentCount = segments.size
    var index = 0 // Index of first segment
    while (index < segmentCount) {
        if (segments[index] >= 0) break
        index++
    }
    if (index >= segmentCount || index == 0) // The path is empty, or else the original first segment survived,
    // in which case we already know that no leading "." is needed
        return
    var segment = segments[index]
    while (segment < path.size && path[segment] != ':' && path[segment] != NUL) {
        segment++
    }
    if (segment >= path.size || path[segment] == NUL) // No colon in first segment, so no "." needed
        return

    // At this point we know that the first segment is unused,
    // hence we can insert a "." segment at that position
    path[0] = '.'
    path[1] = NUL
    segments[0] = 0
}

private fun String.hasDotAtIndex(index: Int): Boolean =
    hasDotAtEnd(index) ||
            hasDotBeforeSlash(index)

private fun String.hasTwoDotsAtIndex(index: Int): Boolean =
    hasTwoDotsAtEnd(index) ||
            hasTwoDotsBeforeSlash(index)

private fun String.hasDotAtEnd(index: Int): Boolean =
    index == length - 1 &&
            endsWith('.')

private fun String.hasTwoDotsAtEnd(index: Int): Boolean =
    index == length - 2 &&
            endsWith("..")

private fun String.hasDotBeforeSlash(index: Int): Boolean =
    index <= length - 2 &&
            this[index] == '.' &&
            this[index + 1] == SLASH

private fun String.hasTwoDotsBeforeSlash(index: Int): Boolean =
    index <= length - 3 &&
            this[index] == '.' &&
            this[index + 1] == '.' &&
            this[index + 2] == SLASH
