package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

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
        val codedewDoc = app.get(url).document
        var argonUrl = codedewDoc.select("iframe").attr("src")
            .ifBlank { codedewDoc.select("iframe").attr("data-src") }

        if (argonUrl.isBlank()) {
            // sometimes link is direct in page
            argonUrl = Regex("""https?://argon\.razorshell\.space/embed/[^"'\s]+""").find(codedewDoc.html())?.value ?: return
        }
        if (!argonUrl.startsWith("http")) argonUrl = "https:$argonUrl"

        val argonHtml = app.get(argonUrl).text

        // try multiple patterns for stream
        val patterns = listOf(
            Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']"""),
            Regex("""["'](https?://[^"']+\.mp4[^"']*)["']"""),
            Regex("""file\s*:\s*["']([^"']+)["']"""),
            Regex("""source\s*:\s*["']([^"']+)["']"""),
            Regex("""src\s*:\s*["']([^"']+\.m3u8[^"']*)["']""")
        )

        for (p in patterns) {
            val match = p.find(argonHtml)?.groupValues?.get(1)
            if (!match.isNullOrBlank() && (match.contains(".m3u8") || match.contains(".mp4"))) {
                callback.invoke(
                    newExtractorLink(
                        name,
                        name,
                        match,
                        type = if (match.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        this.referer = argonUrl
                        this.quality = Qualities.Unknown.value
                    }
                )
                return
            }
        }
    }
}
