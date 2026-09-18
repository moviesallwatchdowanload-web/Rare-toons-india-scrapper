package com.megix

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class RareAnimesPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(RareAnimesProvider())
        registerExtractorAPI(Codedew())   // ← yeh line add karo
    }
}
