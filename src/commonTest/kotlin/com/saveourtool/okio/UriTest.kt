package com.saveourtool.okio

import com.saveourtool.okio.UriToPathConverter.Companion.SCHEME_FILE
import com.saveourtool.system.OsFamily
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UriTest {
    @Test
    fun `spaces in the path field`() {
        sequenceOf(
            ASCII,
        )
            .map { it.toPath() }
            .map(Path::absolute)
            .map(Path::toFileUri)
            .forEach { uri ->
                uri.toString() shouldContain "%20"
            }
    }

    @Test
    fun `non-ASCII in the path field`() {
        sequenceOf(
            CYRILLIC,
        )
            .map { it.toPath() }
            .map(Path::absolute)
            .map(Path::toFileUri)
            .forEach { uri ->
                uri.toString() shouldContain "%20"
            }
    }

    @Test
    fun `UNC paths`() {
        if (OsFamily.isWindows()) {
            val uri = "\\\\127.0.0.1\\C$\\Windows".toPath().toFileUri()
            assertEquals("127.0.0.1", uri.authority)

            val path = uri.toLocalPath().normalized()
            assertEquals(
                expected = "\\\\127.0.0.1\\C$\\Windows",
                actual = path.pathString
            )

            assertEquals("127.0.0.1", path.toFileUri().authority)
        }
    }

    @Test
    fun `UNC paths with reserved characters`() {
        if (OsFamily.isWindows()) {
            val uri = "\\\\WSL$\\Debian\\etc\\passwd".toPath().toFileUri()
            assertNull(uri.authority)

            val path = uri.toLocalPath().normalized()
            assertEquals(
                expected = "\\\\WSL$\\Debian\\etc\\passwd",
                actual = path.pathString
            )

            assertNull(path.toFileUri().authority)
        }
    }

    @Test
    fun `IPv6 UNC paths`() {
        if (OsFamily.isWindows()) {
            assertEquals(
                expected = "[2001:db8::85b:3c51:f5ff:ffdb]",
                actual = "\\\\2001-db8--85b-3c51-f5ff-ffdb.ipv6-literal.net\\C$\\Windows".toPath().toFileUri().authority
            )
            assertEquals(
                expected = "[::1]",
                actual = "\\\\--1.ipv6-literal.net\\C$\\Windows".toPath().toFileUri().authority
            )
        }
    }

    @Test
    fun `local path with a hostname part`() {
        val uri = Uri("file://[::1]/path")
        assertTrue(uri.isAbsolute)
        assertFalse(uri.isOpaque)
        assertEquals("[::1]", uri.authority)
        assertEquals("[::1]", uri.host)
        assertEquals(SCHEME_FILE, uri.scheme)
        assertEquals("/path", uri.path)
        assertEquals("//[::1]/path", uri.schemeSpecificPart)

        if (OsFamily.isWindows()) {
            assertEquals("\\\\--1.ipv6-literal.net\\path", uri.toLocalPath().pathString)
        }
    }

    @Test
    fun `Unicode space characters`() {
        val uris = sequenceOf(
            '\u00a0',
            '\u2007',
            '\u202F',
        )
            .map(Char::toString)
            .map { it.toPath() }
            .map(Path::toFileUri)
            .toList()
        uris[0].toString() shouldEndWith "/%c2%a0"
        uris[1].toString() shouldEndWith "/%e2%80%87"
        uris[2].toString() shouldEndWith "/%e2%80%af"
    }

    @Test
    fun normalization() {
        Uri("file:///path/to/.././file").normalize().toString() shouldBe "file:/path/file"
        Uri("file:///path/to/.././file//").normalize().toString() shouldBe "file:/path/file/"
        Uri("file:///path/to/.././file/.").normalize().toString() shouldBe "file:/path/file/"
    }

    @Test
    fun decoding() {
        val uri = Uri("file:///$CYRILLIC_ENCODED")
        uri.path shouldBe "/$CYRILLIC"
        uri.toLocalPath().pathString shouldEndWith CYRILLIC
    }

    @Test
    fun `URI with a fragment`() {
        assertEquals(
            expected = "URI fragments (\"\") are not supported at index 20: file:///path/to/file#",
            actual = assertFailsWith<IllegalArgumentException> {
                Uri("file:///path/to/file#")
            }.message,
        )
        assertEquals(
            expected = "URI fragments (\"fragment\") are not supported at index 20: file:///path/to/file#fragment",
            actual = assertFailsWith<IllegalArgumentException> {
                Uri("file:///path/to/file#fragment")
            }.message,
        )
    }

    @Test
    fun `conversion should fail for non-local URIs`() {
        assertEquals(
            expected = "URI schemes other than \"file\" are not supported: \"https\"",
            actual = assertFailsWith<IllegalArgumentException> {
                Uri("https://example.com").toLocalPath()
            }.message,
        )
    }

    @Test
    fun `absolute URIs from relative paths`() {
        sequenceOf(
            "path/to/file.ext",
            "./path/to/file.ext",
        )
            .map { it.toPath() }
            .map(Path::toFileUri)
            .flatMap { uri ->
                sequenceOf(
                    uri,
                    uri.normalize(),
                )
            }
            .distinctBy(Uri::toString)
            .map(Uri::toLocalPath)
            .map(Path::normalized)
            .forEach { path ->
                assertEquals(
                    expected = "".toPath().absolute() / "path" / "to" / "file.ext",
                    actual = path.normalized(),
                )
            }
    }

    @Test
    fun `absolute URIs from absolute Windows paths`() {
        sequenceOf(
            "file:///C:/autoexec.bat",
            "file:/C:/autoexec.bat",
        )
            .map { Uri(it) }
            .forEach { uri ->
                assertTrue(uri.isAbsolute)
                assertFalse(uri.isOpaque)
                assertEquals(expected = "file", actual = uri.scheme)

                when {
                    OsFamily.isWindows() -> assertEquals(
                        expected = "C:\\autoexec.bat".toPath(),
                        actual = uri.toLocalPath().normalized()
                    )

                    else -> assertEquals(
                        expected = "/C:/autoexec.bat".toPath(),
                        actual = uri.toLocalPath().normalized()
                    )
                }

            }
    }

    @Test
    fun `absolute URIs from absolute UNIX paths`() {
        sequenceOf(
            "file:///etc/passwd",
            "file:/etc/passwd",
        )
            .map { Uri(it) }
            .forEach { uri ->
                assertTrue(uri.isAbsolute)
                assertFalse(uri.isOpaque)
                assertEquals(expected = "file", actual = uri.scheme)

                when {
                    OsFamily.isUnix() -> assertEquals(
                        expected = "/etc/passwd".toPath(),
                        actual = uri.toLocalPath().normalized()
                    )

                    else -> assertEquals(
                        expected = "\\etc\\passwd".toPath(),
                        actual = uri.toLocalPath()
                    )
                }
            }
    }

    private companion object {
        private const val ASCII = "The quick brown fox jumps over the lazy dog"

        @Suppress("MaxLineLength")
        private const val CYRILLIC =
            "\u0421\u044a\u0435\u0448\u044c\u0020\u0436\u0435\u0020\u0435\u0449\u0451\u0020\u044d\u0442\u0438\u0445\u0020\u043c\u044f\u0433\u043a\u0438\u0445\u0020\u0444\u0440\u0430\u043d\u0446\u0443\u0437\u0441\u043a\u0438\u0445\u0020\u0431\u0443\u043b\u043e\u043a\u0020\u0434\u0430\u0020\u0432\u044b\u043f\u0435\u0439\u0020\u0447\u0430\u044e"

        @Suppress("MaxLineLength")
        private const val CYRILLIC_ENCODED =
            "%D0%A1%D1%8A%D0%B5%D1%88%D1%8C%20%D0%B6%D0%B5%20%D0%B5%D1%89%D1%91%20%D1%8D%D1%82%D0%B8%D1%85%20%D0%BC%D1%8F%D0%B3%D0%BA%D0%B8%D1%85%20%D1%84%D1%80%D0%B0%D0%BD%D1%86%D1%83%D0%B7%D1%81%D0%BA%D0%B8%D1%85%20%D0%B1%D1%83%D0%BB%D0%BE%D0%BA%20%D0%B4%D0%B0%20%D0%B2%D1%8B%D0%BF%D0%B5%D0%B9%20%D1%87%D0%B0%D1%8E"
    }
}
