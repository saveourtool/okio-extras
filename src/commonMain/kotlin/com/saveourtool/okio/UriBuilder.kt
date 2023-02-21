package com.saveourtool.okio

internal class UriBuilder {
    private var scheme: String? = null

    private var rawAuthority: String? = null

    private var host: String? = null

    private var rawPath: String? = null

    private var rawSchemeSpecificPart: String? = null

    fun withScheme(scheme: String?) {
        this.scheme = scheme
    }

    fun withRawAuthority(rawAuthority: String?) {
        this.rawAuthority = rawAuthority
    }

    fun withHost(host: String?) {
        this.host = host
    }

    fun withRawPath(rawPath: String?) {
        this.rawPath = rawPath
    }

    fun withRawSchemeSpecificPart(rawSchemeSpecificPart: String?) {
        this.rawSchemeSpecificPart = rawSchemeSpecificPart
    }

    fun build(): Uri =
        Uri(
            scheme,
            rawAuthority,
            host,
            rawPath,
            rawSchemeSpecificPart,
        )
}
