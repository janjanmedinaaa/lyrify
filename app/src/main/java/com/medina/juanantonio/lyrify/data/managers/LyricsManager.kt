package com.medina.juanantonio.lyrify.data.managers

import android.net.Uri
import android.util.Log
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.medina.juanantonio.lyrify.common.extensions.toLyrics
import com.medina.juanantonio.lyrify.data.models.Lyrics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

object LyricsManager : ILyricsManager {
    private const val LYRICS_OVH_BASE_URL = "https://api.lyrics.ovh/v1"
    private const val AZ_LYRICS_BASE_URL = "https://www.azlyrics.com/lyrics"
    private const val AZ_LYRICS_COMMENT = "Usage of azlyrics.com content by any " +
        "third-party lyrics provider is prohibited by our licensing agreement." +
        " Sorry about that."

    private const val TAG = "LyricsManager"

    override suspend fun getSongLyrics(artist: String, title: String, index: Int): Lyrics {
        val multipleArtists = artist.split(",").map { it.trim() }
        val encodedArtist = formatAZParameter(multipleArtists[index])
        val encodedTitle = formatAZParameter(title)
        val lyricsAPI = "$AZ_LYRICS_BASE_URL/$encodedArtist/$encodedTitle.html"
        val resultData = CompletableDeferred<Lyrics>()

        Log.d(TAG, "$title, $artist $lyricsAPI - AZ Lyrics")

        try {
            val document = Jsoup.connect(lyricsAPI).get()
            val contentDivItemList =
                document.select("div[class='col-xs-12 col-lg-8 text-center'] > div")

            if (contentDivItemList.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    if (index + 1 > multipleArtists.size - 1) {
                        resultData.complete(
                            getSongLyricsFromOVH(artist, title, 0)
                        )
                    } else {
                        resultData.complete(
                            getSongLyrics(artist, title, index + 1)
                        )
                    }
                }
            } else {
                val filteredContentDivItemList =
                    contentDivItemList.filter { it.className().isEmpty() }
                val lyricsDiv = filteredContentDivItemList.firstOrNull()

                if (lyricsDiv != null) {
                    resultData.complete(
                        Lyrics(lyrics = "$lyricsDiv")
                    )
                } else {
                    resultData.complete(
                        getSongLyricsFromOVH(artist, title, 0)
                    )
                }
            }
        } catch (e: Exception) {
            resultData.complete(
                getSongLyricsFromOVH(artist, title, 0)
            )
        }

        return resultData.await()
    }

    private suspend fun getSongLyricsFromOVH(artist: String, title: String, index: Int): Lyrics {
        val multipleArtists = artist.split(",").map { it.trim() }
        val encodedArtist = Uri.encode(multipleArtists[index])
        val encodedTitle = Uri.encode(title)
        val lyricsAPI = "$LYRICS_OVH_BASE_URL/$encodedArtist/$encodedTitle"
        val resultData = CompletableDeferred<Lyrics>()
        val request = lyricsAPI.httpGet()

        Log.d(TAG, "$title, $artist $lyricsAPI - Lyrics OVH")

        request.responseString { _, _, result ->
            when (result) {
                is Result.Success -> {
                    resultData.complete(result.value.toLyrics())
                }
                is Result.Failure -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (index + 1 > multipleArtists.size - 1) {
                            resultData.complete(
                                Lyrics(error = "No lyrics found")
                            )
                        } else {
                            resultData.complete(
                                getSongLyricsFromOVH(artist, title, index + 1)
                            )
                        }
                    }
                }
            }
        }.join()

        return resultData.await()
    }

    override fun formatLyrics(lyrics: String): ArrayList<String> {
        val regexSingerPart1= """\[(.*?)]""".toRegex()
        val regexSingerPart2 = """<i>\[(.*?):]</i>""".toRegex()
        return ArrayList(
            lyrics
                .split("\n")
                .asSequence()
                .map {
                    it.replace("<br>", "")
                        .replace("<div>", "")
                        .replace("</div>", "")
                        .replace("<!-- $AZ_LYRICS_COMMENT -->", "")
                        .removeSurrounding("\\r")
                        .trim()
                }
                .filter { it.isNotEmpty() }
                .filterNot { regexSingerPart1.matches(it) }
                .filterNot { regexSingerPart2.matches(it) }
                .filterNot { it.startsWith("Paroles") }
                .toList()
        )
    }

    private fun formatAZParameter(parameter: String): String {
        val regexSpecialChars = """[<(\[{^@'\-=${'$'}!|\]}):&%_#;",?*+./\\> ]""".toRegex()
        return parameter
            .split("feat.")
            .first()
            .trim()
            .replace(regexSpecialChars, "")
            .lowercase()
    }
}

interface ILyricsManager {
    suspend fun getSongLyrics(artist: String, title: String, index: Int = 0): Lyrics
    fun formatLyrics(lyrics: String): ArrayList<String>
}