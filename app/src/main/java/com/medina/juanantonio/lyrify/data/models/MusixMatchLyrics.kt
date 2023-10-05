package com.medina.juanantonio.lyrify.data.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

fun MusixMatchLyrics.toOpenSpotifyLyrics(): OpenSpotifyLyrics {
    val lines = subtitleBody?.map {
        val minutesMs = it.time.minutes * 60 * 1000
        val secondsMs = it.time.seconds * 1000
        val millisMs = it.time.hundredths * 10

        OpenSpotifyLyrics.Lines(
            words = it.text,
            startTimeMs = (minutesMs + secondsMs + millisMs).toInt().toString()
        )
    } ?: emptyList()

    return OpenSpotifyLyrics(
        error = lines.isEmpty(),
        syncType = "LINE_SYNCED",
        lines = ArrayList(lines)
    )
}

// TODO: WTF is this API
data class MusixMatchLyrics(
    @SerializedName(value = "message")
    val message: Message
) {
    data class Message(
        @SerializedName(value = "body")
        val body: MessageBody
    )

    val subtitleBody: List<SubtitleBodyItem>?
        get() {
            return message.body.macroCalls
                .trackSubtitlesGet.trackSubtitlesGetMessage.trackSubtitlesGetBody
                ?.subtitleList?.first()?.subtitle?.subtitleBody
        }
}

data class MessageBody(
    @SerializedName(value = "macro_calls")
    val macroCalls: MacroCalls
) {
    data class MacroCalls(
        @SerializedName(value = "track.subtitles.get")
        val trackSubtitlesGet: TrackSubtitlesGet
    )
}

data class TrackSubtitlesGet(
    @SerializedName(value = "message")
    val trackSubtitlesGetMessage: TrackSubtitlesGetMessage
) {
    data class TrackSubtitlesGetMessage(
        @SerializedName(value = "body")
        val trackSubtitlesGetBody: TrackSubtitlesGetBody?
    ) {
        data class TrackSubtitlesGetBody(
            @SerializedName(value = "subtitle_list")
            val subtitleList: List<SubtitleItem>
        )
    }
}

data class SubtitleItem(
    @SerializedName(value = "subtitle")
    val subtitle: Subtitle
) {
    data class Subtitle(
        @SerializedName(value = "subtitle_body")
        val _subtitleBody: String
    ) {
        val subtitleBody: List<SubtitleBodyItem>
            get() = Gson().fromJson(_subtitleBody, Array<SubtitleBodyItem>::class.java).toList()
    }
}

data class SubtitleBodyItem(
    val text: String,
    val time: SubtitleTime
) {
    data class SubtitleTime(
        val total: Float,
        val minutes: Float,
        val seconds: Float,
        val hundredths: Float
    )
}