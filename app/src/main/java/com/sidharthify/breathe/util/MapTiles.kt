// SPDX-License-Identifier: MIT
/*
 * MapTiles.kt - Tile downloads over OkHttp, so a dead IPv6 route can't stall the map
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.sidharthify.breathe.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.CantContinueException
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck
import org.osmdroid.tileprovider.modules.MapTileDownloader
import org.osmdroid.tileprovider.modules.TileDownloader
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.Inet6Address
import java.util.concurrent.TimeUnit

private const val TAG = "MapTiles"

// some home wifi (seen on a xiaomi pad 8) hands out an IPv6 address but never
// routes IPv6. osmdroid's own downloader sets no connect timeout, so each of
// its two threads sits on the dead IPv6 address of the tile server for
// minutes and the map stays grey while the api, being IPv4 only, works fine.
// trying IPv4 addresses first sidesteps that, and IPv6 only networks still
// get their IPv6 addresses since there's nothing else to try
private object Ipv4FirstDns : Dns {
    override fun lookup(hostname: String) = Dns.SYSTEM.lookup(hostname).sortedBy { it is Inet6Address }
}

private val tileClient: OkHttpClient by lazy {
    OkHttpClient
        .Builder()
        .dns(Ipv4FirstDns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}

// does what osmdroid's TileDownloader does (user agent, expiry from the cache
// headers, write to the tile cache) but through OkHttp with timeouts
private class OkHttpTileDownloader : TileDownloader() {
    override fun downloadTile(
        pMapTileIndex: Long,
        pRedirectCount: Int,
        pTargetUrl: String,
        pFilesystemCache: IFilesystemCache?,
        pTileSource: OnlineTileSourceBase,
    ): Drawable? {
        val config = Configuration.getInstance()
        val userAgent =
            if (pTileSource.tileSourcePolicy.normalizesUserAgent()) {
                config.normalizedUserAgent
            } else {
                config.userAgentValue
            }
        val request =
            Request
                .Builder()
                .url(pTargetUrl)
                .header(config.userAgentHttpHeader, userAgent)
                .apply {
                    config.additionalHttpRequestProperties.forEach { (name, value) ->
                        header(name, value)
                    }
                }.build()

        try {
            tileClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "tile ${response.code} for $pTargetUrl")
                    return null
                }
                val bytes = response.body?.bytes() ?: return null
                val expires =
                    computeExpirationTime(
                        response.header("Expires"),
                        response.header("Cache-Control"),
                        System.currentTimeMillis(),
                    )
                pFilesystemCache?.saveFile(pTileSource, pMapTileIndex, ByteArrayInputStream(bytes), expires)
                return pTileSource.getDrawable(ByteArrayInputStream(bytes))
            }
        } catch (e: BitmapTileSourceBase.LowMemoryException) {
            // osmdroid's loader backs off and retries on this one
            throw CantContinueException(e)
        } catch (e: IOException) {
            Log.w(TAG, "tile download failed for $pTargetUrl", e)
            return null
        }
    }
}

// the stock provider with only the download step swapped out
class BreatheTileProvider(
    context: Context,
) : MapTileProviderBasic(context) {
    override fun createDownloaderProvider(
        aNetworkAvailabilityChecker: INetworkAvailablityCheck?,
        pTileSource: ITileSource?,
    ): MapTileDownloader {
        val downloader = super.createDownloaderProvider(aNetworkAvailabilityChecker, pTileSource)
        downloader.setTileDownloader(OkHttpTileDownloader())
        return downloader
    }
}
