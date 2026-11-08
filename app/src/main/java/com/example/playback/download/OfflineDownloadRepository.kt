package com.example.playback.download

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.example.core.common.AppResult
import com.example.core.database.AniScrollDao
import com.example.core.database.OfflineDownloadEntity
import com.example.core.network.NetworkClient
import com.example.domain.model.Anime
import com.example.domain.model.HlsPlaylistInspection
import com.example.domain.model.HlsQualityVariant
import com.example.domain.model.OfflineDownloadItem
import com.example.domain.model.OfflineDownloadState
import com.example.domain.model.SubtitleTrack
import com.example.domain.repository.AniScrollRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(UnstableApi::class)
class OfflineDownloadRepository(
    private val appContext: Context,
    private val dao: AniScrollDao,
    private val aniScrollRepository: AniScrollRepository,
    private val okHttpClient: OkHttpClient,
    val media3DownloadStack: Media3DownloadStack
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = NetworkClient.json

    companion object {
        fun buildDownloadId(anilistId: Int, episode: Int, audioType: String): String {
            return "aniscroll_${anilistId}_ep${episode}_${audioType.lowercase()}"
        }
    }

    init {
        // Attach Media3 DownloadManager.Listener to sync state changes into Room
        media3DownloadStack.downloadManager.addListener(object : DownloadManager.Listener {
            override fun onInitialized(downloadManager: DownloadManager) {
                scope.launch { syncFromMedia3DownloadIndex() }
            }

            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                scope.launch {
                    syncSingleMedia3Download(download, finalException)
                }
            }

            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                scope.launch {
                    dao.deleteOfflineDownloadById(download.request.id)
                }
            }
        })

        // Continuous progress poller while Media3 has active downloads
        scope.launch {
            // Restore stream auth contexts from persisted Room records on startup
            val saved = dao.getAllOfflineDownloadsSnapshot()
            saved.forEach { entity ->
                media3DownloadStack.registerStreamAuthContext(
                    downloadId = entity.downloadId,
                    hlsProxyUrl = entity.hlsProxyUrl,
                    refererUrl = entity.refererUrl
                )
            }
            while (true) {
                val activeCount = media3DownloadStack.downloadManager.currentDownloads.size
                if (activeCount > 0) {
                    syncFromMedia3DownloadIndex()
                    delay(500L)
                } else {
                    delay(1500L)
                }
            }
        }
    }

    fun observeAllDownloads(): Flow<List<OfflineDownloadItem>> {
        return dao.observeAllOfflineDownloads().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeDownloadsForAnime(anilistId: Int): Flow<Map<Int, OfflineDownloadItem>> {
        return dao.observeOfflineDownloadsForAnime(anilistId).map { list ->
            list.associate { it.episode to it.toDomain() }
        }
    }

    /**
     * Step 1-4 of Workflow:
     * 1. Call the AniScroll watch endpoint.
     * 2. Extract the actual `hlsProxyUrl` from the verified API response.
     * 3. Validate that it is a supported `#EXTM3U` HLS manifest without DRM restrictions.
     * 4. Inspect the master playlist and available quality variants (`360p`, `480p`, `720p`, `1080p`)
     *    using both manifest parsing and Media3 [DownloadHelper].
     */
    suspend fun inspectEpisodeForDownload(
        anime: Anime,
        episode: Int,
        audioType: String
    ): AppResult<HlsPlaylistInspection> = withContext(Dispatchers.IO) {
        val normalizedAudio = if (audioType.equals("dub", ignoreCase = true)) "dub" else "sub"

        // 1. Call the AniScroll watch endpoint
        val streamResult = aniScrollRepository.resolveEpisodeStream(
            anilistId = anime.anilistId,
            episode = episode,
            audioType = normalizedAudio
        )
        val resolution = when (streamResult) {
            is AppResult.Success -> streamResult.data
            is AppResult.Error -> return@withContext AppResult.Error(streamResult.message, streamResult.throwable)
            AppResult.Loading -> return@withContext AppResult.Error("Stream resolution still in progress.")
        }

        // 2 & 3. Extract hlsProxyUrl and validate that it is a supported HLS manifest
        val validation = HlsManifestValidator.validateAndInspectResolution(
            okHttpClient = okHttpClient,
            resolution = resolution
        )
        val validManifest = when (validation) {
            is HlsManifestValidationResult.Valid -> validation
            is HlsManifestValidationResult.Invalid -> {
                return@withContext AppResult.Error(
                    "${validation.failureTitle}: ${validation.exactFailureReason}"
                )
            }
        }

        // 4. Inspect available quality variants (use parsed #EXT-X-STREAM-INF variants when present,
        // or fall back to Media3 DownloadHelper track inspection for single-variant manifests)
        val finalVariants = if (validManifest.isMasterPlaylist && validManifest.variants.any { it.isAvailable }) {
            validManifest.variants
        } else {
            val helperResult = media3DownloadStack.inspectVariantsWithDownloadHelper(
                hlsProxyUrl = validManifest.hlsProxyUrl,
                refererUrl = validManifest.refererUrl,
                manifestVariants = validManifest.variants
            )
            helperResult.getOrElse { err ->
                if (validManifest.variants.any { it.isAvailable }) {
                    validManifest.variants
                } else {
                    return@withContext AppResult.Error(
                        "Media3 DownloadHelper could not prepare the HLS playlist from hlsProxyUrl: ${err.localizedMessage ?: "Unsupported or inaccessible HLS stream"}."
                    )
                }
            }
        }

        if (finalVariants.none { it.isAvailable }) {
            return@withContext AppResult.Error(
                "The HLS playlist at hlsProxyUrl does not expose any supported 360p, 480p, 720p, or 1080p video variants for offline download."
            )
        }

        AppResult.Success(
            HlsPlaylistInspection(
                anilistId = anime.anilistId,
                episode = episode,
                audioType = normalizedAudio,
                animeTitle = anime.displayTitle,
                posterUrl = anime.posterUrl,
                coverUrl = anime.coverUrl,
                totalEpisodes = anime.availableEpisodeCount.coerceAtLeast(episode),
                hlsProxyUrl = validManifest.hlsProxyUrl,
                refererUrl = validManifest.refererUrl,
                isMasterPlaylist = validManifest.isMasterPlaylist,
                variants = finalVariants,
                subtitles = resolution.subtitles
            )
        )
    }

    /**
     * Step 5-8 of Workflow:
     * 5. Allow the user to select 360p, 480p, 720p, or 1080p only when available.
     * 6. Use Media3 `DownloadHelper` to select the desired video, audio, and subtitle tracks.
     * 7. Download the selected HLS video and audio segments via `DownloadService` & `DownloadManager`.
     * 8. Download permitted subtitle tracks where supported.
     */
    suspend fun startHlsDownload(
        inspection: HlsPlaylistInspection,
        selectedVariant: HlsQualityVariant
    ): AppResult<OfflineDownloadItem> = withContext(Dispatchers.IO) {
        if (!selectedVariant.isAvailable) {
            return@withContext AppResult.Error(
                "${selectedVariant.qualityLabel} is not available in the master playlist for Episode ${inspection.episode}."
            )
        }

        val downloadId = buildDownloadId(
            anilistId = inspection.anilistId,
            episode = inspection.episode,
            audioType = inspection.audioType
        )

        media3DownloadStack.registerStreamAuthContext(
            downloadId = downloadId,
            hlsProxyUrl = inspection.hlsProxyUrl,
            refererUrl = inspection.refererUrl
        )

        // Download permitted external subtitle tracks locally so offline playback has full captions
        val localSubs = downloadPermittedSubtitles(
            downloadId = downloadId,
            refererUrl = inspection.refererUrl,
            subtitles = inspection.subtitles
        )

        val now = System.currentTimeMillis()
        val initialEntity = OfflineDownloadEntity(
            downloadId = downloadId,
            anilistId = inspection.anilistId,
            episode = inspection.episode,
            totalEpisodes = inspection.totalEpisodes,
            audioType = inspection.audioType,
            animeTitle = inspection.animeTitle,
            posterUrl = inspection.posterUrl,
            coverUrl = inspection.coverUrl,
            qualityLabel = selectedVariant.qualityLabel,
            videoHeight = selectedVariant.height,
            hlsProxyUrl = inspection.hlsProxyUrl,
            refererUrl = inspection.refererUrl,
            state = OfflineDownloadState.QUEUED.name,
            progressPercent = 0f,
            bytesDownloaded = 0L,
            contentLengthBytes = 0L,
            failureReasonText = null,
            localSubtitlesJson = json.encodeToString(localSubs),
            createdAtMs = now,
            updatedAtMs = now
        )
        dao.insertOfflineDownload(initialEntity)

        // Build Media3 DownloadRequest using DownloadHelper with selected track overrides
        val customDataJson = json.encodeToString(initialEntity.toDomain())
        val requestResult = media3DownloadStack.buildDownloadRequestWithDownloadHelper(
            downloadId = downloadId,
            hlsProxyUrl = inspection.hlsProxyUrl,
            refererUrl = inspection.refererUrl,
            selectedVariant = selectedVariant,
            customDataJson = customDataJson
        )

        val downloadRequest = requestResult.getOrElse { err ->
            val exactFailure = explainDownloadException(err)
            val failedEntity = initialEntity.copy(
                state = OfflineDownloadState.FAILED.name,
                failureReasonText = exactFailure,
                updatedAtMs = System.currentTimeMillis()
            )
            dao.insertOfflineDownload(failedEntity)
            return@withContext AppResult.Error(exactFailure, err)
        }

        // Enqueue in Media3 DownloadService + DownloadManager
        runCatching {
            DownloadService.sendAddDownload(
                appContext,
                AniScrollHlsDownloadService::class.java,
                downloadRequest,
                /* foreground= */ false
            )
        }
        media3DownloadStack.downloadManager.addDownload(downloadRequest)
        media3DownloadStack.downloadManager.resumeDownloads()

        AppResult.Success(initialEntity.toDomain())
    }

    suspend fun pauseDownload(downloadId: String) = withContext(Dispatchers.IO) {
        runCatching {
            DownloadService.sendSetStopReason(
                appContext,
                AniScrollHlsDownloadService::class.java,
                downloadId,
                Media3DownloadStack.STOP_REASON_USER_PAUSED,
                /* foreground= */ false
            )
        }
        media3DownloadStack.downloadManager.setStopReason(
            downloadId,
            Media3DownloadStack.STOP_REASON_USER_PAUSED
        )
        val existing = dao.getOfflineDownloadById(downloadId)
        if (existing != null) {
            dao.insertOfflineDownload(
                existing.copy(
                    state = OfflineDownloadState.PAUSED.name,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun resumeDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getOfflineDownloadById(downloadId)
        if (existing != null) {
            media3DownloadStack.registerStreamAuthContext(
                downloadId = downloadId,
                hlsProxyUrl = existing.hlsProxyUrl,
                refererUrl = existing.refererUrl
            )
            dao.insertOfflineDownload(
                existing.copy(
                    state = OfflineDownloadState.DOWNLOADING.name,
                    failureReasonText = null,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        }
        val media3Download = media3DownloadStack.getMedia3Download(downloadId)
        if (media3Download != null && media3Download.state == Download.STATE_FAILED) {
            runCatching {
                DownloadService.sendAddDownload(
                    appContext,
                    AniScrollHlsDownloadService::class.java,
                    media3Download.request,
                    /* foreground= */ false
                )
            }
            media3DownloadStack.downloadManager.addDownload(media3Download.request)
        }
        runCatching {
            DownloadService.sendSetStopReason(
                appContext,
                AniScrollHlsDownloadService::class.java,
                downloadId,
                Download.STOP_REASON_NONE,
                /* foreground= */ false
            )
            DownloadService.sendResumeDownloads(
                appContext,
                AniScrollHlsDownloadService::class.java,
                /* foreground= */ false
            )
        }
        media3DownloadStack.downloadManager.setStopReason(downloadId, Download.STOP_REASON_NONE)
        media3DownloadStack.downloadManager.resumeDownloads()
    }

    suspend fun cancelOrDeleteDownload(downloadId: String) = withContext(Dispatchers.IO) {
        runCatching {
            DownloadService.sendRemoveDownload(
                appContext,
                AniScrollHlsDownloadService::class.java,
                downloadId,
                /* foreground= */ false
            )
        }
        media3DownloadStack.downloadManager.removeDownload(downloadId)
        deleteLocalSubtitlesForDownload(downloadId)
        dao.deleteOfflineDownloadById(downloadId)
    }

    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        runCatching {
            DownloadService.sendRemoveAllDownloads(
                appContext,
                AniScrollHlsDownloadService::class.java,
                /* foreground= */ false
            )
        }
        media3DownloadStack.downloadManager.removeAllDownloads()
        runCatching {
            media3DownloadStack.subtitleDirectory.listFiles()?.forEach { it.deleteRecursively() }
        }
        dao.clearAllOfflineDownloads()
    }

    /**
     * Checks if `(anilistId, episode, audioType)` has a verified, completed offline download in Room
     * and Media3 `DownloadIndex`. Incomplete, paused, queued, or failed downloads are never marked playable.
     */
    suspend fun getCompletedOfflinePlaybackInfo(
        anilistId: Int,
        episode: Int,
        audioType: String
    ): Pair<OfflineDownloadItem, DownloadRequest?>? = withContext(Dispatchers.IO) {
        val entity = dao.getOfflineDownloadForEpisode(anilistId, episode, audioType) ?: return@withContext null
        val item = entity.toDomain()
        val media3Download = media3DownloadStack.getMedia3Download(item.downloadId)
        val isVerifiedComplete = (item.isPlayableOffline || media3Download?.state == Download.STATE_COMPLETED) &&
            media3Download?.state != Download.STATE_DOWNLOADING &&
            media3Download?.state != Download.STATE_QUEUED &&
            media3Download?.state != Download.STATE_STOPPED &&
            media3Download?.state != Download.STATE_FAILED
        if (isVerifiedComplete) {
            item.copy(state = OfflineDownloadState.COMPLETED, progressPercent = 100f) to media3Download?.request
        } else {
            null
        }
    }

    private suspend fun downloadPermittedSubtitles(
        downloadId: String,
        refererUrl: String?,
        subtitles: List<SubtitleTrack>
    ): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        if (subtitles.isEmpty()) return@withContext emptyList()
        val targetFolder = File(media3DownloadStack.subtitleDirectory, downloadId).apply {
            if (!exists()) mkdirs()
        }
        val headers = HlsManifestValidator.buildAuthorizedHeaders(refererUrl)

        subtitles.mapIndexedNotNull { idx, sub ->
            try {
                val reqBuilder = Request.Builder().url(sub.url).get()
                headers.forEach { (k, v) -> reqBuilder.header(k, v) }
                okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@mapIndexedNotNull null
                    val bodyBytes = resp.body?.bytes() ?: return@mapIndexedNotNull null
                    if (bodyBytes.isEmpty()) return@mapIndexedNotNull null

                    val ext = if (sub.url.contains(".srt", ignoreCase = true)) "srt" else "vtt"
                    val safeLang = sub.lang.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "sub_$idx" }
                    val outFile = File(targetFolder, "${safeLang}_$idx.$ext")
                    outFile.writeBytes(bodyBytes)

                    SubtitleTrack(
                        url = Uri.fromFile(outFile).toString(),
                        lang = sub.lang,
                        isDefault = sub.isDefault
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun deleteLocalSubtitlesForDownload(downloadId: String) {
        runCatching {
            val folder = File(media3DownloadStack.subtitleDirectory, downloadId)
            if (folder.exists()) {
                folder.deleteRecursively()
            }
        }
    }

    private suspend fun syncFromMedia3DownloadIndex() {
        val media3Map = media3DownloadStack.getAllMedia3Downloads()
        val roomList = dao.getAllOfflineDownloadsSnapshot()
        for (entity in roomList) {
            val d = media3Map[entity.downloadId] ?: continue
            syncSingleMedia3Download(d, null, entity)
        }
    }

    private suspend fun syncSingleMedia3Download(
        download: Download,
        finalException: Exception?,
        existingEntity: OfflineDownloadEntity? = null
    ) {
        val current = existingEntity ?: dao.getOfflineDownloadById(download.request.id) ?: return
        val mappedState = when (download.state) {
            Download.STATE_QUEUED -> OfflineDownloadState.QUEUED
            Download.STATE_DOWNLOADING, Download.STATE_RESTARTING -> OfflineDownloadState.DOWNLOADING
            Download.STATE_STOPPED -> OfflineDownloadState.PAUSED
            Download.STATE_COMPLETED -> OfflineDownloadState.COMPLETED
            Download.STATE_FAILED -> OfflineDownloadState.FAILED
            Download.STATE_REMOVING -> OfflineDownloadState.REMOVING
            else -> OfflineDownloadState.QUEUED
        }

        val rawPercent = download.percentDownloaded
        val safePercent = when {
            mappedState == OfflineDownloadState.COMPLETED -> 100f
            rawPercent == C.PERCENTAGE_UNSET.toFloat() || rawPercent.isNaN() || rawPercent < 0f -> current.progressPercent
            else -> rawPercent.coerceIn(0f, 100f)
        }

        val failureExplanation = if (mappedState == OfflineDownloadState.FAILED) {
            finalException?.let { explainDownloadException(it) }
                ?: current.failureReasonText
                ?: explainMedia3FailureReason(download.failureReason)
        } else {
            null
        }

        val updated = current.copy(
            state = mappedState.name,
            progressPercent = safePercent,
            bytesDownloaded = download.bytesDownloaded.coerceAtLeast(current.bytesDownloaded),
            contentLengthBytes = if (download.contentLength > 0L) download.contentLength else current.contentLengthBytes,
            failureReasonText = failureExplanation,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.insertOfflineDownload(updated)
    }

    private fun explainDownloadException(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("429") ->
                "HLS proxy rate limit reached (HTTP 429 Too Many Requests). Tap Resume to continue downloading remaining segments."
            msg.contains("403") || msg.contains("401") ->
                "HLS proxy or CDN rejected segment download (HTTP 403/401). The stream token may have expired or offline segment access is restricted."
            msg.contains("404") ->
                "HLS segment or variant playlist was not found (HTTP 404). One or more video segments are unavailable on the upstream server."
            msg.contains("410") ->
                "The hlsProxyUrl stream session has expired (HTTP 410). Tap Retry to resolve a fresh hlsProxyUrl."
            msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504") ->
                "The AniScroll HLS proxy returned a server error while downloading HLS segments ($msg)."
            else ->
                "HLS download failed: ${e.localizedMessage ?: "Unable to fetch HLS segments from hlsProxyUrl"}."
        }
    }

    private fun explainMedia3FailureReason(failureReason: Int): String {
        return if (failureReason == Download.FAILURE_REASON_UNKNOWN) {
            "HLS segment download failed: The hlsProxyUrl session expired, a media segment was unavailable, or the proxy rejected concurrent segment requests."
        } else {
            "HLS download stopped (code $failureReason)."
        }
    }

    private fun OfflineDownloadEntity.toDomain(): OfflineDownloadItem {
        val parsedState = runCatching { OfflineDownloadState.valueOf(state) }
            .getOrDefault(OfflineDownloadState.QUEUED)
        val parsedSubs = runCatching {
            json.decodeFromString<List<SubtitleTrack>>(localSubtitlesJson)
        }.getOrDefault(emptyList())

        return OfflineDownloadItem(
            downloadId = downloadId,
            anilistId = anilistId,
            episode = episode,
            totalEpisodes = totalEpisodes,
            audioType = audioType,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            coverUrl = coverUrl,
            qualityLabel = qualityLabel,
            videoHeight = videoHeight,
            hlsProxyUrl = hlsProxyUrl,
            refererUrl = refererUrl,
            state = parsedState,
            progressPercent = progressPercent,
            bytesDownloaded = bytesDownloaded,
            contentLengthBytes = contentLengthBytes,
            failureReasonText = failureReasonText,
            localSubtitleTracks = parsedSubs,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs
        )
    }
}
