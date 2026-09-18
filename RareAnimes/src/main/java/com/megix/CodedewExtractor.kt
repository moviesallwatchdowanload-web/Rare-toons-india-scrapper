package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.helper.AesHelper
import org.jsoup.Jsoup

class Codedew : ExtractorApi() {
    override val name = "Codedew"
    override val mainUrl = "https://codedew.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // Step 1: Get Codedew page → extract Argon embed
        val codedewDoc = app.get(url).document
        val argonUrl = codedewDoc.select("iframe").attr("src")
            .ifBlank { codedewDoc.select("iframe").attr("data-src") }

        if (argonUrl.isBlank()) return

        // Step 2: Get Argon page → extract juicyData
        val argonHtml = app.get(argonUrl).text
        val token = Regex("\"token\"\\s*:\\s*\"([^\"]+)\"").find(argonHtml)?.groupValues?.get(1)
        val videoId = Regex("\"video\"\\s*:\\s*\"([^\"]+)\"").find(argonHtml)?.groupValues?.get(1)
        val pingPath = Regex("\"ping\"\\s*:\\s*\"([^\"]+)\"").find(argonHtml)?.groupValues?.get(1)

        if (token.isNullOrBlank() || pingPath.isNullOrBlank()) return

        val pingUrl = if (pingPath.startsWith("http")) pingPath else "https://argon.razorshell.space$pingPath"

        // Step 3: Call the public ping endpoint with token
        val response = app.post(
            pingUrl,
            headers = mapOf(
                "Referer" to argonUrl,
                "Origin" to "https://argon.razorshell.space",
                "Content-Type" to "application/json",
                "X-Token" to token
            ),
            data = mapOf("token" to token)
        ).text

        // Look for m3u8 / mp4 in response
        val m3u8 = Regex("(https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*)").find(response)?.value
            ?: Regex("(https?://[^\"'\\s]+\\.mp4[^\"'\\s]*)").find(response)?.value

        if (!m3u8.isNullOrBlank()) {
            callback.invoke(
                newExtractorLink(
                    name,
                    name,
                    m3u8,
                    type = if (m3u8.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                ) {
                    this.referer = argonUrl
                    this.quality = Qualities.Unknown.value
                }
            )
        }
    }
}
