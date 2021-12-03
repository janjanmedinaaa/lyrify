package com.medina.juanantonio.lyrify.data.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medina.juanantonio.lyrify.databinding.ItemLyricsBinding

class LyricsAdapter : RecyclerView.Adapter<LyricsAdapter.LyricsItemViewHolder>() {
    private val lyricsList = arrayListOf<String>()

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
    fun setLyrics(lyricsList: ArrayList<String>) {
        this.lyricsList.clear()
        this.lyricsList.addAll(lyricsList)
        notifyDataSetChanged()
    }

    inner class LyricsItemViewHolder(
        private val binding: ItemLyricsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(line: String) {
            binding.textViewLine.text = line
        }
    }
}