package com.example.moodequalizer.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moodequalizer.Interfaces.OnMusicSelect
import com.example.moodequalizer.Models.MusicModel
import com.example.moodequalizer.R
import com.example.moodequalizer.currentMusic
import kotlinx.android.synthetic.main.music_list.view.*

class MusicAdapter(var musicList: ArrayList<MusicModel>, var context: Context, var OnMusicSelect: OnMusicSelect) : RecyclerView.Adapter<MusicAdapter.ViewHolder>(){

    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        var music_title = itemView.music_title
        var music_artist = itemView.music_artist
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.music_list, parent, false))
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.music_title.text = musicList[position].musicTitle
        holder.music_artist.text = musicList[position].musicArtist

        holder.itemView.setOnClickListener{
            currentMusic = position
            OnMusicSelect.onSelect(musicList[position])
        }
    }
}