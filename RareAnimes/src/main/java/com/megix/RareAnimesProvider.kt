package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import org.jsoup.nodes.Element

@CloudstreamPlugin
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

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data.format(page)).document
        val home = document.select("article, div.post").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select("h2, h3").first()?.text()?.replace("Download ", "") ?: return null
        val href = this.select("a").attr("href")
        val posterUrl = this.select("img").attr("src").ifBlank { this.select("img").attr("data-src") }
        if (href.isBlank()) return null
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article, div.post").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.select("h1.entry-title, h1.post-title").text()
            .replace("Download ", "").substringBefore(" - ")
        val posterUrl = document.select("div.entry-content img, article img").first()?.attr("src")
        val description = document.select("div.entry-content p").text().substringBefore("Anime Series Info")

        val episodes = mutableListOf<Episode>()

        document.select("h3:contains(Episode), h4:contains(Episode), p:contains(Episode)").forEach { heading ->
            val epTitle = heading.text()
            val watchLink = heading.nextElementSiblings().select("a:contains(WatchMultQuality)").firstOrNull()
            val epUrl = watchLink?.attr("href")

            if (!epUrl.isNullOrBlank()) {
                episodes.add(
                    newEpisode(EpisodeLink(epUrl)) {
                        this.name = epTitle
                        this.episode = Regex("""(\d+)""").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()
                    }
                )
            }
        }

        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        } else {
            val watchLink = document.select("a:contains(WatchMultQuality)").firstOrNull()?.attr("href")
            if (watchLink.isNullOrBlank()) return null
            newMovieLoadResponse(title, url, TvType.Movie, EpisodeLink(watchLink)) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val sources = AppUtils.tryParseJson<ArrayList<EpisodeLink>>(data) ?: return false
        for (source in sources) {
            loadExtractor(
                source.source,
                subtitleCallback,
                callback
            )
        }
        return true
    }

    data class EpisodeLink(val source: String)
}
