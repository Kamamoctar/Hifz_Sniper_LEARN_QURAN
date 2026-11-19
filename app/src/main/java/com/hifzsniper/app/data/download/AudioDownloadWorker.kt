package com.hifzsniper.app.data.download

import android.content.Context
import android.os.storage.StorageManager
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.hifzsniper.app.data.local.AppDatabase
import com.hifzsniper.app.domain.AlafasyAudioSources
import com.hifzsniper.app.domain.DownloadSafety
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class AudioDownloadWorker(appContext: Context, parameters: WorkerParameters) : CoroutineWorker(appContext, parameters) {
    private val dao = AppDatabase.get(appContext).downloadDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure(errorData("Identifiant absent"))
        val item = dao.get(id) ?: return@withContext Result.failure(errorData("Téléchargement introuvable"))
        if (!DownloadSafety.isSecureRemoteUrl(item.remoteUrl)) {
            dao.update(item.copy(status = "FAILED", errorMessage = "URL HTTPS invalide", updatedAt = System.currentTimeMillis()))
            return@withContext Result.failure(errorData("URL HTTPS invalide"))
        }
        val audioDirectory = File(applicationContext.filesDir, "audio")
        if (!audioDirectory.exists() && !audioDirectory.mkdirs()) {
            dao.update(item.copy(status = "FAILED", errorMessage = "Stockage audio inaccessible", updatedAt = System.currentTimeMillis()))
            return@withContext Result.failure(errorData("Stockage audio inaccessible"))
        }
        val target = File(audioDirectory, DownloadSafety.safeFileName(item.id))
        val temporary = File(audioDirectory, ".${DownloadSafety.safeFileName(item.id, ".part")}")
        if (!DownloadSafety.isInside(audioDirectory, target) || !DownloadSafety.isInside(audioDirectory, temporary)) {
            dao.update(item.copy(status = "FAILED", errorMessage = "Chemin local invalide", updatedAt = System.currentTimeMillis()))
            return@withContext Result.failure(errorData("Chemin local invalide"))
        }
        if (target.isFile && runCatching { sha256(target).equals(item.checksum, ignoreCase = true) }.getOrDefault(false)) {
            val size = target.length()
            dao.update(
                item.copy(
                    status = "COMPLETED",
                    localPath = target.absolutePath,
                    bytesDownloaded = size,
                    totalBytes = size,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            return@withContext Result.success(progressData(size, size))
        }
        dao.update(item.copy(status = "DOWNLOADING", localPath = target.absolutePath, errorMessage = null, updatedAt = System.currentTimeMillis()))
        try {
            val candidates = AlafasyAudioSources.candidates(item.remoteUrl, item.resourceKey)
            var lastError: Exception? = null
            candidates.forEachIndexed { index, remoteUrl ->
                try {
                    var resumeOffset = temporary.length().coerceAtLeast(0L)
                    val connection = (URI(remoteUrl).toURL().openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        instanceFollowRedirects = false
                        requestMethod = "GET"
                        if (resumeOffset > 0L) setRequestProperty("Range", "bytes=$resumeOffset-")
                    }
                    try {
                        val status = connection.responseCode
                        if (status !in 200..299) throw IOException("Réponse HTTP $status")
                        val append = resumeOffset > 0L && status == HttpURLConnection.HTTP_PARTIAL
                        if (!append) {
                            resumeOffset = 0L
                        } else {
                            val contentRange = connection.getHeaderField("Content-Range").orEmpty()
                            if (!contentRange.startsWith("bytes $resumeOffset-")) {
                                throw IOException("Réponse de reprise HTTP incohérente")
                            }
                        }
                        val totalFromRange = connection.getHeaderField("Content-Range")
                            ?.substringAfterLast('/', "")
                            ?.toLongOrNull()
                        val responseLength = connection.contentLengthLong.takeIf { it >= 0 }
                        val total = totalFromRange ?: responseLength?.let { resumeOffset + it }
                        val remaining = total?.minus(resumeOffset)?.coerceAtLeast(0L)
                        if (remaining != null) {
                            val requiredBytes = remaining + MIN_FREE_SPACE_BYTES
                            val storageManager = applicationContext.getSystemService(StorageManager::class.java)
                            val storageUuid = storageManager.getUuidForPath(audioDirectory)
                            if (storageManager.getAllocatableBytes(storageUuid) < requiredBytes) {
                                throw IOException("Espace insuffisant : libérez du stockage puis réessayez")
                            }
                            storageManager.allocateBytes(storageUuid, requiredBytes)
                        }
                        var downloaded = resumeOffset
                        connection.inputStream.use { input ->
                            FileOutputStream(temporary, append).buffered().use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    setProgress(progressData(downloaded, total))
                                    dao.update(
                                        item.copy(
                                            status = "DOWNLOADING",
                                            localPath = target.absolutePath,
                                            bytesDownloaded = downloaded,
                                            totalBytes = total,
                                            errorMessage = null,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                        if (total != null && downloaded != total) throw IOException("Téléchargement incomplet")
                        val actualChecksum = sha256(temporary)
                        if (!actualChecksum.equals(item.checksum, ignoreCase = true)) {
                            FileOutputStream(temporary, false).use { }
                            throw IOException("Somme SHA-256 incorrecte")
                        }
                        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                        dao.update(
                            item.copy(
                                status = "COMPLETED",
                                localPath = target.absolutePath,
                                bytesDownloaded = downloaded,
                                totalBytes = total ?: downloaded,
                                errorMessage = null,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        return@withContext Result.success(progressData(downloaded, total ?: downloaded))
                    } finally {
                        connection.disconnect()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    lastError = error
                    if (index < candidates.lastIndex) {
                        val latest = dao.get(id) ?: item
                        dao.update(
                            latest.copy(
                                status = "DOWNLOADING",
                                bytesDownloaded = temporary.length(),
                                errorMessage = "Source ${index + 1}/${candidates.size} indisponible, essai du miroir suivant",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            throw lastError ?: IOException("Aucune source audio disponible")
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                val latest = dao.get(id) ?: item
                dao.update(
                    latest.copy(
                        status = "CANCELLED",
                        bytesDownloaded = temporary.length(),
                        errorMessage = null,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            throw cancelled
        } catch (error: Exception) {
            val latest = dao.get(id) ?: item
            dao.update(
                latest.copy(
                    status = "FAILED",
                    bytesDownloaded = temporary.length(),
                    errorMessage = error.message ?: "Erreur de téléchargement",
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure(errorData(error.message ?: "Erreur de téléchargement"))
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun progressData(downloaded: Long, total: Long?) = Data.Builder()
        .putLong(KEY_BYTES_DOWNLOADED, downloaded)
        .putLong(KEY_TOTAL_BYTES, total ?: -1)
        .build()

    private fun errorData(message: String) = Data.Builder().putString(KEY_ERROR, message).build()

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR = "error"
        private const val MAX_RETRIES = 3
        private const val MIN_FREE_SPACE_BYTES = 5L * 1024L * 1024L
    }
}
