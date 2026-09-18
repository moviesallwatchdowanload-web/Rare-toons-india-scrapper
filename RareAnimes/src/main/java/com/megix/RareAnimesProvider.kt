package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

open class RareAnimesProvider : MainAPI() {
    override var mainUrl = "https://www.rareanimes.mov"
    override var name = "RareAnimes"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)

    override val mainPage = mainPageOf(
        "$mainUrl/hindi/page/%d/" to "Hindi",
        "$mainUrl/anime/page/%d/" to "Anime",
        "$mainUrl/cartoon/page/%d/" to "Cartoon"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page)).document
        val home = document.select("article, div.post")
            .mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = select("h1.entry-title, h2, h3").first()
            ?.text()
            ?.replace("Download ", "")
            ?.trim()
            ?: return null

        val href = select("a[href]").firstOrNull()?.attr("href")
            ?: return null

        if (href.isBlank()) return null

        val posterUrl = select("img").firstOrNull()?.let {
            it.attr("src").ifBlank { it.attr("data-src") }
        }

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val document = app.get(
            "$mainUrl/?s=${query.replace(" ", "+")}"
        ).document

        return document.select("article, div.post")
            .mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document
            .select("h1.entry-title, h1.post-title")
            .text()
            .replace("Download ", "")
            .trim()
            .substringBefore(" - ")

        val posterUrl = document
            .select("div.entry-content img, article img")
            .firstOrNull()
            ?.attr("src")

        val description = document
            .select("div.entry-content p")
            .text()
            .substringBefore("Anime Series Info")
            .trim()

        val episodes = mutableListOf<Episode>()

        // Current RareAnimes pages use:
        // <p>Episode 01 – Title</p>
        // <p>Hindi – [WatchMultQuality] ...</p>
        val paragraphs = document.select("div.entry-content p")

        for (index in paragraphs.indices) {
            val paragraph = paragraphs[index]
            val text = paragraph.text().trim()

            val match = Regex(
                """(?i)^Episode\s+(\d+)\s*[–—-]\s*(.+)$"""
            ).find(text) ?: continue

            val episodeNumber = match.groupValues[1].toIntOrNull() ?: continue
            val episodeTitle = match.groupValues[2].trim()

            val sourceParagraph = paragraphs
                .drop(index + 1)
                .firstOrNull { next ->
                    val nextText = next.text().trim()
                    nextText.contains("WatchMultQuality", ignoreCase = true) ||
                    nextText.contains("WatchMultiQuality", ignoreCase = true) ||
                    nextText.contains(".m3u8", ignoreCase = true) ||
                    nextText.contains(".mp4", ignoreCase = true)
                }

            val sourceUrl = sourceParagraph
                ?.select("a[href]")
                ?.firstOrNull { link ->
                    val href = link.attr("href")
                    val textValue = link.text().trim()

                    isUsableSource(href, textValue)
                }
                ?.attr("href")

            if (!sourceUrl.isNullOrBlank()) {
                episodes.add(
                    newEpisode(EpisodeLink(sourceUrl)) {
                        this.name = episodeTitle
                        this.episode = episodeNumber
                    }
                )
            }
        }

        if (episodes.isNotEmpty()) {
            return newTvSeriesLoadResponse(
                title,
                url,
                TvType.TvSeries,
                episodes
            ) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }

        // Movie/direct-source fallback
        val directSource = document
            .select("a[href]")
            .firstOrNull { link ->
                isUsableSource(
                    link.attr("href"),
                    link.text()
                )
            }
            ?.attr("href")

        if (directSource.isNullOrBlank()) {
            return null
        }

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            EpisodeLink(directSource)
        ) {
            this.posterUrl = posterUrl
            this.plot = description
        }
    }

    private fun isUsableSource(
        href: String,
        linkText: String
    ): Boolean {
        if (href.isBlank()) return false

        val lowerHref = href.lowercase()
        val lowerText = linkText.lowercase()

        // Direct media
        if (
            lowerHref.contains(".m3u8") ||
            lowerHref.contains(".mp4") ||
            lowerHref.contains(".mkv") ||
            lowerHref.contains(".webm")
        ) {
            return true
        }

        // Do not send Codedew encrypted links to CloudStream.
        if (false) {
            return false
        }

        // Known playback wording; CloudStream can resolve supported hosts.
        return lowerText.contains("watchmultquality") ||
            lowerText.contains("watchmultiquality") ||
            lowerText.contains("streambeta")
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val sources =
            AppUtils.tryParseJson<ArrayList<EpisodeLink>>(data)
                ?: return false

        var loaded = false

        for (source in sources) {
            val sourceUrl = source.source

            if (sourceUrl.isBlank()) continue
            if (sourceUrl.lowercase().contains("codedew.com")) continue

            loadExtractor(
                sourceUrl,
                subtitleCallback,
                callback
            )

            loaded = true
        }

        return loaded
    }

    data class EpisodeLink(
        val source: String
    )
}
