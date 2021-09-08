package com.example.moodequalizer

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodequalizer.Adapters.MusicAdapter
import com.example.moodequalizer.Interfaces.OnMusicComplete
import com.example.moodequalizer.Interfaces.OnMusicSelect
import com.example.moodequalizer.Models.MusicModel
import com.example.moodequalizer.Services.MusicService
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_music_player.*
import kotlinx.android.synthetic.main.activity_music_player.view.*

class MainActivity : AppCompatActivity(), OnMusicSelect, View.OnClickListener, OnMusicComplete {
    lateinit var list: ArrayList<MusicModel>
    lateinit var adapter: MusicAdapter
    lateinit var musicService: MusicService
    lateinit var musicModel: MusicModel
    lateinit var sheetBehavior: BottomSheetBehavior<View>
    var playIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        play_pause_btn.setOnClickListener(this)
        previous_btn.setOnClickListener(this)
        next_btn.setOnClickListener(this)
        to_music_list_btn.setOnClickListener(this)

        val bottomSheet = findViewById<View>(R.id.bottom_layout)
        sheetBehavior = BottomSheetBehavior.from(bottomSheet)

        list = ArrayList()

        var manager = LinearLayoutManager(applicationContext)
        recyclerview.layoutManager = manager
        adapter = MusicAdapter(list, applicationContext, this)
        recyclerview.adapter = adapter

        getMusic()
    }

    private fun getMusic() {
        list.clear()
        val contentResolver: ContentResolver = this.contentResolver
        val musicUri : Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor : Cursor? = contentResolver.query(musicUri, null, null, null, null)
        if(cursor != null && cursor.moveToFirst()) {
            val musicId = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val musicTitle = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val musicArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val musicData = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val musicDate = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

            while(cursor.moveToNext()) {
                val currentId = cursor.getLong(musicId)
                val currentTitle = cursor.getString(musicTitle)
                val currentArtist = cursor.getString(musicArtist)
                val currentData = cursor.getString(musicData)
                val currentDate = cursor.getLong(musicDate)

                list.add(MusicModel(currentId, currentTitle, currentArtist, currentData, currentDate))

            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onStart() {
        super.onStart()
        if(playIntent == null) {
            playIntent = Intent(this, MusicService::class.java)
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    override fun onDestroy() {
        stopService(playIntent)
        unbindService(musicConnection)
        super.onDestroy()
    }

    private fun updateUI() {
        bottom_layout.music_title_bar.text = musicModel.musicTitle
        bottom_layout.music_artist_bar.text = musicModel.musicArtist
    }

    private var musicConnection : ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder : MusicService.MusicBinder = service as MusicService.MusicBinder
            musicService = binder.service
            musicService.setUI(bottom_layout.seek_bar_design, bottom_layout.start_point, bottom_layout.end_point)

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }

    }

    override fun onSelect(music: MusicModel) {
        musicService.setMusic(music)
        musicModel = music
        musicService.setListener(this)

        updateUI()
    }

    override fun onClick(v: View?) {
        when(v) {
            play_pause_btn -> {
                if(musicService.playerState == 2) {
                    // Pause music
                    play_pause_btn.setImageResource(R.drawable.play_circle)
                    musicService.pauseMusic()
                }
                else if(musicService.playerState == 1) {
                    //Resume music
                    play_pause_btn.setImageResource(R.drawable.pause_circle)
                    musicService.resumeMusic()
                }
            }
            next_btn -> {
                if(list.size > 0) {
                    if(currentMusic != -1) {
                        if(list.size - 1 == currentMusic) {
                            currentMusic = 0
                            musicService.setMusic(list[currentMusic])
                            musicModel = list[currentMusic]

                            updateUI()
                        }
                        else {
                            ++currentMusic
                            musicService.setMusic(list[currentMusic])
                            musicModel = list[currentMusic]

                            updateUI()
                        }
                    }
                }
            }
            previous_btn -> {
                if(currentMusic != -1) {
                    if(currentMusic == 0) {
                        currentMusic = list.size - 1
                        musicService.setMusic(list[currentMusic])
                        musicModel = list[currentMusic]

                        updateUI()
                    }
                    else {
                        currentMusic--
                        musicService.setMusic(list[currentMusic])
                        musicModel = list[currentMusic]

                        updateUI()
                    }

                }
            }
            to_music_list_btn -> {
                if(sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED ) {
                    sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    to_music_list_btn.setImageResource(R.drawable.arrow_up)
                }
                else {
                    sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    to_music_list_btn.setImageResource(R.drawable.arrow_down)
                }
            }
        }
    }

    override fun onMusicComplete() {
        if(currentMusic != -1) {
            if(list.size - 1 == currentMusic) {
                currentMusic = 0
                musicService.setMusic(list[currentMusic])
                musicModel = list[currentMusic]

                updateUI()
            }
            else {
                ++currentMusic
                musicService.setMusic(list[currentMusic])
                musicModel = list[currentMusic]

                updateUI()
            }
        }
    }
}
