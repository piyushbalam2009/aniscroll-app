package com.example.playback.download

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import com.example.domain.model.HlsQualityVariant
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(UnstableApi::class)
class Media3DownloadStack(
    private val appContext: Context
) {
    companion object {
        const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "aniscroll_hls_download_channel"
        const val STOP_REASON_USER_PAUSED = 1001
        private const val PROXY_RATE_LIMIT_COOLDOWN_MS = 60_000L
    }

    val databaseProvider = StandaloneDatabaseProvider(appContext)

    val downloadDirectory: File = File(appContext.filesDir, "media3_hls_downloads").apply {
        if (!exists()) mkdirs()
    }

    val subtitleDirectory: File = File(appContext.filesDir, "offline_subtitles").apply {
        if (!exists()) mkdirs()
    }

    val downloadCache: SimpleCache = SimpleCache(
        downloadDirectory,
        NoOpCacheEvictor(),
        databaseProvider
    )

    val downloadIndex: DownloadIndex
        get() = downloadManager.downloadIndex

    val defaultDownloadIndex: DefaultDownloadIndex = DefaultDownloadIndex(databaseProvider)

    val downloadNotificationHelper: DownloadNotificationHelper = DownloadNotificationHelper(
        appContext,
        DOWNLOAD_NOTIFICATION_CHANNEL_ID
    )

    // Store active stream context (referer + master proxy URL) so segment requests preserve headers & proxy routing
    private data class StreamAuthContext(
        val hlsProxyUrl: String,
        val refererUrl: String?
    )

    private val streamAuthByDownloadId = ConcurrentHashMap<String, StreamAuthContext>()

    @Volatile
    private var latestAuthContext: StreamAuthContext = StreamAuthContext(
        hlsProxyUrl = "",
        refererUrl = "https://megaplay.buzz/"
    )

    // Tracks when api.aniscroll.in/api/proxy/hls is returning HTTP 429 Too Many Requests
    private val proxyRateLimitedUntilMs = AtomicLong(0L)

    fun registerStreamAuthContext(downloadId: String, hlsProxyUrl: String, refererUrl: String?) {
        val ctx = StreamAuthContext(hlsProxyUrl = hlsProxyUrl, refererUrl = refererUrl)
        streamAuthByDownloadId[downloadId] = ctx
        latestAuthContext = ctx
    }

    /**
     * Creates a proxy-aware, rate-limit-resilient [DataSource.Factory] that:
     * 1. Preserves authorized `Referer`, `Origin`, and `User-Agent` headers.
     * 2. Rewrites relative child playlist/segment URLs through `hlsProxyUrl`.
     * 3. Handles `HTTP 429 Too Many Requests` (and `502`/`503`/`504`) from `api.aniscroll.in/api/proxy/hls`
     *    by activating a rate-limit cooldown and transparently fetching the segment/playlist from the
     *    embedded upstream URL in `hlsProxyUrl` while keeping the canonical `hlsProxyUrl` URI for `SimpleCache`.
     */
    fun createUpstreamHttpDataSourceFactory(
        explicitRefererUrl: String? = null,
        explicitMasterProxyUrl: String? = null
    ): DataSource.Factory {
        val baseHttpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")

        return DataSource.Factory {
            HlsProxyResilientDataSource(
                httpDataSourceFactory = baseHttpFactory,
                explicitRefererUrl = explicitRefererUrl,
                explicitMasterProxyUrl = explicitMasterProxyUrl
            )
        }
    }

    private inner class HlsProxyResilientDataSource(
        private val httpDataSourceFactory: DefaultHttpDataSource.Factory,
        private val explicitRefererUrl: String?,
        private val explicitMasterProxyUrl: String?
    ) : DataSource {

        private val transferListeners = mutableListOf<TransferListener>()
        private var activeDelegate: DataSource? = null
        private var reportedUri: Uri? = null
        private var lastResponseHeaders: Map<String, List<String>> = emptyMap()

        override fun addTransferListener(transferListener: TransferListener) {
            transferListeners.add(transferListener)
            activeDelegate?.addTransferListener(transferListener)
        }

        private fun createHttpDelegate(): DefaultHttpDataSource {
            val source = httpDataSourceFactory.createDataSource()
            transferListeners.forEach { source.addTransferListener(it) }
            return source
        }

        override fun open(dataSpec: DataSpec): Long {
            val activeCtx = latestAuthContext
            val effectiveReferer = explicitRefererUrl
                ?: activeCtx.refererUrl
                ?: "https://megaplay.buzz/"
            val effectiveMasterProxy = explicitMasterProxyUrl
                ?: activeCtx.hlsProxyUrl

            val resolvedProxyUri = HlsManifestValidator.rewriteMisresolvedProxyUriIfNeeded(
                requestedUri = dataSpec.uri,
                masterProxyUrl = effectiveMasterProxy
            )
            val authHeaders = HlsManifestValidator.buildAuthorizedHeaders(effectiveReferer)
            val mergedHeaders = HashMap<String, String>(dataSpec.httpRequestHeaders)
            authHeaders.forEach { (k, v) -> mergedHeaders.putIfAbsent(k, v) }

            val proxyDataSpec = dataSpec.buildUpon()
                .setUri(resolvedProxyUri)
                .setHttpRequestHeaders(mergedHeaders)
                .build()

            reportedUri = resolvedProxyUri

            val embeddedUpstreamUrl = HlsManifestValidator.extractEmbeddedUpstreamUrl(resolvedProxyUri.toString())
            val now = System.currentTimeMillis()
            val isProxyInCooldown = embeddedUpstreamUrl != null && now < proxyRateLimitedUntilMs.get()

            if (!isProxyInCooldown) {
                val primarySource = createHttpDelegate()
                try {
                    val length = primarySource.open(proxyDataSpec)
                    activeDelegate = primarySource
                    lastResponseHeaders = primarySource.responseHeaders
                    return length
                } catch (e: HttpDataSource.InvalidResponseCodeException) {
                    runCatching { primarySource.close() }
                    val code = e.responseCode
                    if ((code == 429 || code in 502..504) && embeddedUpstreamUrl != null) {
                        proxyRateLimitedUntilMs.set(System.currentTimeMillis() + PROXY_RATE_LIMIT_COOLDOWN_MS)
                        // Fall through to embeddedUpstreamUrl with authorized headers
                    } else {
                        throw e
                    }
                } catch (e: IOException) {
                    runCatching { primarySource.close() }
                    if (embeddedUpstreamUrl == null) {
                        throw e
                    }
                    // If proxy connection reset/timed out, also try embeddedUpstreamUrl
                }
            }

            // Fallback path when api.aniscroll.in/api/proxy/hls returns HTTP 429 or 5xx
            val targetUpstreamUrl = embeddedUpstreamUrl
                ?: throw IOException("HLS proxy unavailable and no embedded upstream URL found.")

            return openEmbeddedUpstreamWithRetry(
                originalProxySpec = proxyDataSpec,
                resolvedProxyUri = resolvedProxyUri,
                upstreamUrl = targetUpstreamUrl,
                headers = mergedHeaders
            )
        }

        private fun openEmbeddedUpstreamWithRetry(
            originalProxySpec: DataSpec,
            resolvedProxyUri: Uri,
            upstreamUrl: String,
            headers: Map<String, String>
        ): Long {
            val isManifestRequest = upstreamUrl.substringBefore("?").endsWith(".m3u8", ignoreCase = true)
            var lastException: IOException? = null
            val retryDelaysMs = longArrayOf(0L, 1200L, 2500L)

            for (attempt in retryDelaysMs.indices) {
                val delayMs = retryDelaysMs[attempt]
                if (delayMs > 0L) {
                    try {
                        Thread.sleep(delayMs)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Interrupted while waiting for rate-limit backoff.")
                    }
                }

                val upstreamDataSpec = originalProxySpec.buildUpon()
                    .setUri(Uri.parse(upstreamUrl))
                    .setHttpRequestHeaders(headers)
                    .build()

                val upstreamSource = createHttpDelegate()
                try {
                    val openedLen = upstreamSource.open(upstreamDataSpec)
                    lastResponseHeaders = upstreamSource.responseHeaders

                    if (isManifestRequest) {
                        // Read manifest text, rewrite relative URIs to canonical hlsProxyUrl form,
                        // and expose via ByteArrayDataSource so HlsPlaylistParser & SimpleCache stay in sync.
                        val buffer = ByteArrayOutputStream()
                        val temp = ByteArray(8192)
                        while (true) {
                            val read = upstreamSource.read(temp, 0, temp.size)
                            if (read == C.RESULT_END_OF_INPUT) break
                            if (read > 0) buffer.write(temp, 0, read)
                        }
                        runCatching { upstreamSource.close() }

                        val rawText = buffer.toString(Charsets.UTF_8.name())
                        val rewrittenText = HlsManifestValidator.rewriteManifestUrisForProxy(
                            rawManifest = rawText,
                            baseProxyUrl = resolvedProxyUri.toString()
                        )
                        val rewrittenBytes = rewrittenText.toByteArray(Charsets.UTF_8)
                        val byteSource = ByteArrayDataSource(rewrittenBytes)
                        transferListeners.forEach { byteSource.addTransferListener(it) }
                        val len = byteSource.open(
                            DataSpec.Builder()
                                .setUri(resolvedProxyUri)
                                .build()
                        )
                        activeDelegate = byteSource
                        return len
                    } else {
                        activeDelegate = upstreamSource
                        return openedLen
                    }
                } catch (e: HttpDataSource.InvalidResponseCodeException) {
                    runCatching { upstreamSource.close() }
                    lastException = e
                    if (e.responseCode != 429 && e.responseCode !in 500..599) {
                        throw e
                    }
                } catch (e: IOException) {
                    runCatching { upstreamSource.close() }
                    lastException = e
                }
            }

            throw lastException ?: IOException("Failed to download HLS segment after rate-limit retries.")
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val delegate = activeDelegate ?: return C.RESULT_END_OF_INPUT
            return delegate.read(buffer, offset, length)
        }

        override fun getUri(): Uri? = reportedUri ?: activeDelegate?.uri

        override fun getResponseHeaders(): Map<String, List<String>> {
            return activeDelegate?.responseHeaders ?: lastResponseHeaders
        }

        override fun close() {
            val delegate = activeDelegate
            activeDelegate = null
            reportedUri = null
            if (delegate != null) {
                runCatching { delegate.close() }
            }
        }
    }

    /**
     * Creates a read-only [CacheDataSource.Factory] backed by [downloadCache] (`SimpleCache`)
     * so `ExoPlayer` can play completed HLS downloads offline.
     */
    fun createOfflinePlaybackDataSourceFactory(
        refererUrl: String?,
        hlsProxyUrl: String?
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                createUpstreamHttpDataSourceFactory(
                    explicitRefererUrl = refererUrl,
                    explicitMasterProxyUrl = hlsProxyUrl
                )
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val downloadManager: DownloadManager = DownloadManager(
        appContext,
        databaseProvider,
        downloadCache,
        createUpstreamHttpDataSourceFactory(),
        Executors.newFixedThreadPool(2)
    ).apply {
        maxParallelDownloads = 1
        minRetryCount = 5
    }

    /**
     * Configures Media3 [DownloadManager] and [AniScrollHlsDownloadService] network requirements
     * for Wi-Fi-only (unmetered) vs any network.
     */
    fun setWifiOnlyRequirements(wifiOnly: Boolean) {
        val requirements = if (wifiOnly) {
            androidx.media3.exoplayer.scheduler.Requirements(
                androidx.media3.exoplayer.scheduler.Requirements.NETWORK_UNMETERED
            )
        } else {
            androidx.media3.exoplayer.scheduler.Requirements(
                androidx.media3.exoplayer.scheduler.Requirements.NETWORK
            )
        }
        runCatching {
            androidx.media3.exoplayer.offline.DownloadService.sendSetRequirements(
                appContext,
                AniScrollHlsDownloadService::class.java,
                requirements,
                /* foreground= */ false
            )
        }
        downloadManager.requirements = requirements
    }

    /**
     * Calculates the actual disk bytes used by Media3 HLS downloads (`SimpleCache`) and local subtitles.
     */
    fun calculateDownloadedBytesOnDisk(): Long {
        val cacheBytes = runCatching { downloadCache.cacheSpace }.getOrDefault(0L)
            .coerceAtLeast(getFolderSize(downloadDirectory))
        val subBytes = getFolderSize(subtitleDirectory)
        return cacheBytes + subBytes
    }

    fun calculateAvailableFreeBytesOnDisk(): Long {
        return runCatching { downloadDirectory.usableSpace }.getOrDefault(0L)
    }

    private fun getFolderSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { f ->
            total += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return total
    }

    /**
     * Uses Media3 [DownloadHelper] to prepare the HLS manifest from `hlsProxyUrl` and inspect
     * the actual video tracks in `MappedTrackInfo`, merging them with the master playlist variants.
     */
    suspend fun inspectVariantsWithDownloadHelper(
        hlsProxyUrl: String,
        refererUrl: String?,
        manifestVariants: List<HlsQualityVariant>
    ): Result<List<HlsQualityVariant>> {
        registerStreamAuthContext("inspect", hlsProxyUrl, refererUrl)

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(hlsProxyUrl))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        val dataSourceFactory = createUpstreamHttpDataSourceFactory(
            explicitRefererUrl = refererUrl,
            explicitMasterProxyUrl = hlsProxyUrl
        )

        val renderersFactory = DefaultRenderersFactory(appContext)
        val helper = DownloadHelper.forMediaItem(
            appContext,
            mediaItem,
            renderersFactory,
            dataSourceFactory
        )

        val prepResult = withTimeoutOrNull(14_000L) {
            suspendCancellableCoroutine<Result<List<HlsQualityVariant>>> { cont ->
                cont.invokeOnCancellation {
                    runCatching { helper.release() }
                }
                helper.prepare(object : DownloadHelper.Callback {
                    override fun onPrepared(preparedHelper: DownloadHelper) {
                        try {
                            val discoveredFromHelper = mutableListOf<HlsQualityVariant>()
                            for (periodIdx in 0 until preparedHelper.periodCount) {
                                val mappedTrackInfo = preparedHelper.getMappedTrackInfo(periodIdx)
                                for (rendererIdx in 0 until mappedTrackInfo.rendererCount) {
                                    if (mappedTrackInfo.getRendererType(rendererIdx) != C.TRACK_TYPE_VIDEO) continue
                                    val trackGroups = mappedTrackInfo.getTrackGroups(rendererIdx)
                                    for (groupIdx in 0 until trackGroups.length) {
                                        val group = trackGroups[groupIdx]
                                        for (trackIdx in 0 until group.length) {
                                            val format = group.getFormat(trackIdx)
                                            val tier = HlsManifestValidator.mapHeightToTier(format.height)
                                            if (tier != null) {
                                                discoveredFromHelper.add(
                                                    HlsQualityVariant(
                                                        qualityLabel = "${tier}p",
                                                        height = tier,
                                                        width = if (format.width > 0) format.width else (tier * 16) / 9,
                                                        bitrateBps = if (format.bitrate > 0) format.bitrate else 0,
                                                        codecs = format.codecs,
                                                        periodIndex = periodIdx,
                                                        rendererIndex = rendererIdx,
                                                        trackGroupIndex = groupIdx,
                                                        trackIndex = trackIdx,
                                                        isAvailable = true
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val merged = HlsManifestValidator.SUPPORTED_TIERS.map { tier ->
                                val helperMatch = discoveredFromHelper
                                    .filter { it.height == tier }
                                    .maxByOrNull { it.bitrateBps }
                                val manifestMatch = manifestVariants.firstOrNull {
                                    it.height == tier && it.isAvailable
                                }
                                when {
                                    helperMatch != null -> helperMatch.copy(
                                        bitrateBps = if (helperMatch.bitrateBps > 0) helperMatch.bitrateBps else (manifestMatch?.bitrateBps ?: 0),
                                        codecs = helperMatch.codecs ?: manifestMatch?.codecs
                                    )
                                    manifestMatch != null -> manifestMatch
                                    else -> HlsQualityVariant(
                                        qualityLabel = "${tier}p",
                                        height = tier,
                                        width = (tier * 16) / 9,
                                        isAvailable = false
                                    )
                                }
                            }
                            if (cont.isActive) cont.resume(Result.success(merged))
                        } catch (t: Throwable) {
                            if (cont.isActive) cont.resume(Result.failure(t))
                        } finally {
                            runCatching { preparedHelper.release() }
                        }
                    }

                    override fun onPrepareError(preparedHelper: DownloadHelper, e: IOException) {
                        runCatching { preparedHelper.release() }
                        if (cont.isActive) {
                            cont.resume(Result.failure(e))
                        }
                    }
                })
            }
        }

        if (prepResult == null) {
            runCatching { helper.release() }
            return if (manifestVariants.any { it.isAvailable }) {
                Result.success(manifestVariants)
            } else {
                Result.failure(IOException("Timed out while inspecting HLS tracks via Media3 DownloadHelper."))
            }
        }

        return prepResult
    }

    /**
     * Uses Media3 [DownloadHelper] to select the desired video quality variant (`360p`, `480p`, `720p`, `1080p`),
     * audio tracks, and permitted subtitle tracks, returning a [DownloadRequest] with populated `streamKeys`.
     */
    suspend fun buildDownloadRequestWithDownloadHelper(
        downloadId: String,
        hlsProxyUrl: String,
        refererUrl: String?,
        selectedVariant: HlsQualityVariant,
        customDataJson: String
    ): Result<DownloadRequest> {
        registerStreamAuthContext(downloadId, hlsProxyUrl, refererUrl)

        val mediaItem = MediaItem.Builder()
            .setMediaId(downloadId)
            .setUri(Uri.parse(hlsProxyUrl))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        val dataSourceFactory = createUpstreamHttpDataSourceFactory(
            explicitRefererUrl = refererUrl,
            explicitMasterProxyUrl = hlsProxyUrl
        )
        val renderersFactory = DefaultRenderersFactory(appContext)
        val helper = DownloadHelper.forMediaItem(
            appContext,
            mediaItem,
            renderersFactory,
            dataSourceFactory
        )

        val result = withTimeoutOrNull(18_000L) {
            suspendCancellableCoroutine<Result<DownloadRequest>> { cont ->
                cont.invokeOnCancellation {
                    runCatching { helper.release() }
                }
                helper.prepare(object : DownloadHelper.Callback {
                    override fun onPrepared(preparedHelper: DownloadHelper) {
                        try {
                            for (periodIdx in 0 until preparedHelper.periodCount) {
                                preparedHelper.clearTrackSelections(periodIdx)

                                val paramsBuilder = DownloadHelper.getDefaultTrackSelectorParameters(appContext)
                                    .buildUpon()
                                    .setMaxVideoSize(Int.MAX_VALUE, selectedVariant.height)
                                    .setMinVideoSize(0, (selectedVariant.height - 80).coerceAtLeast(0))
                                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)

                                // If MappedTrackInfo has an exact video track matching selectedVariant.height,
                                // add an explicit TrackSelectionOverride so DownloadHelper locks onto that variant.
                                val mapped = preparedHelper.getMappedTrackInfo(periodIdx)
                                for (rIdx in 0 until mapped.rendererCount) {
                                    if (mapped.getRendererType(rIdx) == C.TRACK_TYPE_VIDEO) {
                                        val groups = mapped.getTrackGroups(rIdx)
                                        for (gIdx in 0 until groups.length) {
                                            val group = groups[gIdx]
                                            val matchingIndices = (0 until group.length).filter { tIdx ->
                                                HlsManifestValidator.mapHeightToTier(group.getFormat(tIdx).height) == selectedVariant.height
                                            }
                                            if (matchingIndices.isNotEmpty()) {
                                                paramsBuilder.addOverride(
                                                    TrackSelectionOverride(group, listOf(matchingIndices.first()))
                                                )
                                            }
                                        }
                                    }
                                }

                                val trackSelectionParams: TrackSelectionParameters = paramsBuilder.build()
                                preparedHelper.addTrackSelection(periodIdx, trackSelectionParams)
                            }

                            val request = preparedHelper.getDownloadRequest(
                                downloadId,
                                Util.getUtf8Bytes(customDataJson)
                            )
                            if (cont.isActive) {
                                cont.resume(Result.success(request))
                            }
                        } catch (t: Throwable) {
                            if (cont.isActive) {
                                cont.resume(Result.failure(t))
                            }
                        } finally {
                            runCatching { preparedHelper.release() }
                        }
                    }

                    override fun onPrepareError(preparedHelper: DownloadHelper, e: IOException) {
                        runCatching { preparedHelper.release() }
                        if (cont.isActive) {
                            cont.resume(Result.failure(e))
                        }
                    }
                })
            }
        }

        if (result == null) {
            runCatching { helper.release() }
            return Result.failure(
                IOException("Media3 DownloadHelper timed out while preparing HLS variant tracks from hlsProxyUrl.")
            )
        }
        return result
    }

    /**
     * Queries Media3 [DownloadIndex] for a specific `downloadId`.
     */
    fun getMedia3Download(downloadId: String): Download? {
        return runCatching {
            downloadManager.downloadIndex.getDownload(downloadId)
        }.getOrNull()
    }

    /**
     * Queries Media3 [DownloadIndex] for all persisted downloads + active [DownloadManager.getCurrentDownloads].
     */
    fun getAllMedia3Downloads(): Map<String, Download> {
        val map = LinkedHashMap<String, Download>()
        runCatching {
            downloadManager.downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) {
                    val d = cursor.download
                    map[d.request.id] = d
                }
            }
        }
        runCatching {
            for (d in downloadManager.currentDownloads) {
                map[d.request.id] = d
            }
        }
        return map
    }
}
