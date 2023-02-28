@file:JvmName("UriUtils")

package com.saveourtool.okio

import com.saveourtool.text.isAscii
import okio.ByteString
import kotlin.jvm.JvmName

const val SLASH = '/'
const val BACKSLASH = '\\'
const val URI_UNC_PATH_PREFIX = "//"

internal const val SCHEME_FILE = "file"

const val L_DIGIT = 0x3FF000000000000L // lowMask('0', '9');
const val H_DIGIT = 0L
const val L_UPALPHA = 0L
const val H_UPALPHA = 0x7FFFFFEL // highMask('A', 'Z');
const val L_LOWALPHA = 0L
const val H_LOWALPHA = 0x7FFFFFE00000000L // highMask('a', 'z');
const val L_ALPHA = L_LOWALPHA or L_UPALPHA
const val H_ALPHA = H_LOWALPHA or H_UPALPHA
const val L_ALPHANUM = L_DIGIT or L_ALPHA
const val H_ALPHANUM = H_DIGIT or H_ALPHA
const val L_HEX = L_DIGIT
const val H_HEX = 0x7E0000007EL // highMask('A', 'F') | highMask('a', 'f');
const val L_MARK = 0x678200000000L // lowMask("-_.!~*'()");
const val H_MARK = 0x4000000080000000L // highMask("-_.!~*'()");
const val L_UNRESERVED = L_ALPHANUM or L_MARK
const val H_UNRESERVED = H_ALPHANUM or H_MARK
const val L_RESERVED = -0x53ff67b000000000L // lowMask(";/?:@&=+$,[]");
const val H_RESERVED = 0x28000001L // highMask(";/?:@&=+$,[]");
const val L_ESCAPED = 1L
const val H_ESCAPED = 0L
const val L_URIC = L_RESERVED or L_UNRESERVED or L_ESCAPED
const val H_URIC = H_RESERVED or H_UNRESERVED or H_ESCAPED
const val L_PCHAR = L_UNRESERVED or L_ESCAPED or 0x2400185000000000L // lowMask(":@&=+$,");
const val H_PCHAR = H_UNRESERVED or H_ESCAPED or 0x1L // highMask(":@&=+$,");
const val L_PATH = L_PCHAR or 0x800800000000000L // lowMask(";/");
const val H_PATH = H_PCHAR // highMask(";/") == 0x0L;
const val L_DASH = 0x200000000000L // lowMask("-");
const val H_DASH = 0x0L // highMask("-");
const val L_DOT = 0x400000000000L // lowMask(".");
const val H_DOT = 0x0L // highMask(".");
const val L_USERINFO = L_UNRESERVED or L_ESCAPED or 0x2C00185000000000L // lowMask(";:&=+$,");
const val H_USERINFO = H_UNRESERVED or H_ESCAPED // | highMask(";:&=+$,") == 0L;
const val L_REG_NAME = L_UNRESERVED or L_ESCAPED or 0x2C00185000000000L // lowMask("$,;:@&=+");
const val H_REG_NAME = H_UNRESERVED or H_ESCAPED or 0x1L // highMask("$,;:@&=+");
const val L_SERVER = L_USERINFO or L_ALPHANUM or L_DASH or 0x400400000000000L // lowMask(".:@[]");
const val H_SERVER = H_USERINFO or H_ALPHANUM or H_DASH or 0x28000001L // highMask(".:@[]");
const val L_SERVER_PERCENT = L_SERVER or 0x2000000000L // lowMask("%");
const val H_SERVER_PERCENT = H_SERVER // | highMask("%") == 0L;
const val L_SCHEME = L_ALPHA or L_DIGIT or 0x680000000000L // lowMask("+-.");
const val H_SCHEME = H_ALPHA or H_DIGIT // | highMask("+-.") == 0L
const val L_SCOPE_ID = L_ALPHANUM or 0x400000000000L // lowMask("_.");
const val H_SCOPE_ID = H_ALPHANUM or 0x80000000L // highMask("_.");

@Suppress("MagicNumber")
internal fun match(char: Char, lowMask: Long, highMask: Long): Boolean =
    when {
        // 0 doesn't have a slot in the mask. So, it never matches.
        char == NUL -> false

        char.code < 64 -> 1L shl char.code and lowMask != 0L

        char.isAscii() -> 1L shl char.code - 64 and highMask != 0L

        else -> false
    }

internal fun StringBuilder.appendEscape(byte: Byte) {
    append('%')
    append(ByteString.of(byte).hex())
}

internal fun Uri.requireSchemeIs(desiredScheme: String) {
    require(scheme != null && scheme.equals(desiredScheme, ignoreCase = true)) {
        "URI schemes other than \"$desiredScheme\" are not supported: \"$scheme\""
    }
}

fun String.slashify(): String =
    replace(BACKSLASH, SLASH)

fun String.backslashify(): String =
    replace(SLASH, BACKSLASH)
