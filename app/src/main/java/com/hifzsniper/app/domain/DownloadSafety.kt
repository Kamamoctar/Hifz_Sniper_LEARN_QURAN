package com.hifzsniper.app.domain

import java.io.File
import java.net.URI

object DownloadSafety {
    fun isSecureRemoteUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.fragment == null
    }.getOrDefault(false)

    fun safeFileName(id: String, extension: String = ".mp3"): String {
        val normalized = id.lowercase().replace(Regex("[^a-z0-9._-]"), "_").trim('.', '_', '-')
        val base = normalized.take(80).ifBlank { "audio" }
        val safeExtension = extension.takeIf { it.matches(Regex("^\\.[a-z0-9]{1,8}$")) } ?: ".bin"
        return base + safeExtension
    }

    fun isInside(parent: File, child: File): Boolean = runCatching {
        val parentPath = parent.canonicalFile.toPath()
        val childPath = child.canonicalFile.toPath()
        childPath.startsWith(parentPath) && childPath != parentPath
    }.getOrDefault(false)
}
