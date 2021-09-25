package com.example.moodequalizer

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
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
import java.io.*
import java.net.Socket

class MainActivity : AppCompatActivity(), OnMusicSelect, View.OnClickListener, OnMusicComplete {
    lateinit var list: ArrayList<MusicModel>
    lateinit var adapter: MusicAdapter
    lateinit var musicService: MusicService
    lateinit var musicModel: MusicModel
    lateinit var newMusicModel: MusicModel
    lateinit var sheetBehavior: BottomSheetBehavior<View>
    lateinit var selectedMusicUri: Uri
    var playIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        play_pause_btn.setOnClickListener(this)
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
            val musicDate = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

            while(cursor.moveToNext()) {
                val currentId = cursor.getLong(musicId)
                val currentTitle = cursor.getString(musicTitle)
                val currentArtist = cursor.getString(musicArtist)
                val currentGenre = "Unknown_genre"
                val currentDate = cursor.getLong(musicDate)

                list.add(MusicModel(currentId, currentTitle, currentArtist, currentGenre, currentDate))

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

    private fun updateUI(musicModel: MusicModel) {
        bottom_layout.music_title_bar.text = musicModel.musicTitle
        bottom_layout.music_genre_bar.text = musicModel.musicGenre
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
        val audioUri : Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        selectedMusicUri = ContentUris.withAppendedId(audioUri, music.musicId)
        musicModel = music

        //new_music.wav is selected
        if(musicModel.musicTitle.startsWith("new")){
            musicService.setMusic(musicModel)
            updateUI(musicModel)
        }
        else {
            var checkNewMusic = 0
            val musicName = musicModel.musicTitle
            while(
                currentMusic < list.size) {
                if(list[currentMusic].musicTitle == "new_$musicName") {
                    checkNewMusic = 1
                }
                currentMusic++
            }
            //music.wav is selected while new_music.wav is exist
            if(checkNewMusic == 1) {
                musicService.setMusic(musicModel)
                updateUI(musicModel)
            }
            //music.wav is selected while new_music.wav isn't exist
            else {
                var thread = NetworkThread()
                thread.start()
            }
        }
        musicService.setListener(this)
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
        play_pause_btn.setImageResource(R.drawable.play_circle)
        musicService.pauseMusic()
    }

    fun findNewMusic(genre: String) {
        val musicName = musicModel.musicTitle
        while(currentMusic < list.size) {
            if(list[currentMusic].musicTitle == "new_$musicName") {
                newMusicModel = list[currentMusic]
                newMusicModel.musicGenre = genre
            }
            currentMusic++
        }
    }

    inner class NetworkThread : Thread() {
        override fun run() {
            try {
                Log.d("check","thread is started.")

                //File read
                var fileDescriptor: ParcelFileDescriptor? = null
                try {
                    fileDescriptor = applicationContext.contentResolver.openFile(selectedMusicUri, "r", null)
                }
                catch (e: FileNotFoundException){
                    e.printStackTrace()
                }

                val fileInput = FileInputStream(fileDescriptor?.fileDescriptor)
                val sendBuffer = ByteArray(1024)
                var receiveBuffer = ByteArray(1024)
                var readBytes = fileInput.read(sendBuffer)

                val HOST = "192.168.0.6"
                val PORT = 8888

                var socket = Socket(HOST, PORT)

                var outputStream = socket.getOutputStream()
                var totalReadBytes = 0L
                var totalWriteBytes = 0L

                //Send music title
                val sendNameData = musicModel.musicTitle
                val sendFileName = sendNameData.toByteArray()
                outputStream.write(sendFileName)

                //Send music file
                while(readBytes > 0) {
                    outputStream.write(sendBuffer, 0, readBytes)
                    totalReadBytes += readBytes

                    readBytes = fileInput.read(sendBuffer)
                }

                Log.d("transfer done", "Done. Total read bytes: $totalReadBytes")

                //Reopen socket
                outputStream.close()
                socket.close()

                sleep(10000)

                socket = Socket(HOST, PORT)
                val inputStream = socket.getInputStream()

                //Receive music title
                inputStream.read(receiveBuffer)
                var str = String(receiveBuffer)
                var splitPoint = str.indexOf("wav") + 3
                val fileName = str.substring(0 until splitPoint)
                Log.d("fileName", "file name is $fileName")

                //Receive music genre
                val genreBuffer = ByteArray(10)
                inputStream.read(genreBuffer)
                str = String(genreBuffer)
                splitPoint = str.lastIndexOf("'")
                val genre = str.substring(2 until splitPoint)
                Log.d("fileGenre", "music genre is $genre")


                //File write
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val item = contentResolver.insert(collection, values)!!

                //Receive music file
                contentResolver.openFileDescriptor(item, "w", null).use {
                    FileOutputStream(it!!.fileDescriptor).use{ fileOutputStream ->
                        var writeBytes = inputStream.read(receiveBuffer)
                        while(writeBytes > 0) {
                            fileOutputStream.write(receiveBuffer, 0, writeBytes)
                            totalWriteBytes += writeBytes

                            writeBytes = inputStream.read(receiveBuffer)
                        }
                        fileOutputStream.close()
                    }
                }

                Log.d("transfer done", "Done. Total write bytes: $totalWriteBytes")

                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                contentResolver.update(item, values, null, null)

                fileInput.close()
                inputStream.close()
                socket.close()

                runOnUiThread{
                    getMusic()
                    findNewMusic(genre)
                    musicService.setMusic(newMusicModel)
                    updateUI(newMusicModel)
                }

            }
            catch (e: Exception){
                e.printStackTrace()
            }

        }
    }
}
