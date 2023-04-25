package com.medina.juanantonio.lyrify.data.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medina.juanantonio.lyrify.data.models.OpenSpotifyLyrics
import com.medina.juanantonio.lyrify.databinding.ItemLyricsBinding

class LyricsAdapter : RecyclerView.Adapter<LyricsAdapter.LyricsItemViewHolder>() {

    companion object {
        fun toLyricList(openSpotifyLyrics: OpenSpotifyLyrics?): ArrayList<Lyric> {
            return ArrayList(
                openSpotifyLyrics?.lines?.map {
                    Lyric(
                        line = it.words,
                        startTimeMs = it.startTimeMs.toInt()
                    )
                }?.filter { it.line.isNotBlank() } ?: emptyList()
            )
        }
    }

    private val lyricsList = arrayListOf<Lyric>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LyricsItemViewHolder {
        val binding = ItemLyricsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LyricsItemViewHolder(binding)
    }

    override fun getItemCount() = lyricsList.size

    override fun onBindViewHolder(holder: LyricsItemViewHolder, position: Int) {
        holder.bind(lyricsList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setLyrics(lyricsList: ArrayList<Lyric>) {
        this.lyricsList.clear()
        this.lyricsList.addAll(lyricsList)
        notifyDataSetChanged()
    }

    fun getNearestLineFromStartTime(startTimeMs: Int): Int {
        return lyricsList.indexOfLast { it.startTimeMs < startTimeMs }
    }

    inner class LyricsItemViewHolder(
        private val binding: ItemLyricsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(lyric: Lyric) {
            binding.textViewLine.text = lyric.line
        }
    }
}

data class Lyric(
    val line: String,
    val startTimeMs: Int
)