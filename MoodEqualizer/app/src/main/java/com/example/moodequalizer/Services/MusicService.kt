package com.example.moodequalizer.Services

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.widget.SeekBar
import android.widget.TextView
import com.example.moodequalizer.Interfaces.OnMusicComplete
import com.example.moodequalizer.Models.MusicModel
import java.util.concurrent.TimeUnit

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    lateinit var player : MediaPlayer
    lateinit var musics : MusicModel
    lateinit var seekBar : SeekBar
    lateinit var start_point : TextView
    lateinit var end_point : TextView
    lateinit var onMusicComplete: OnMusicComplete

    private val musicBind = MusicBinder()
    private val interval : Long = 1000

    var playerState = STOPPED

    override fun onBind(intent: Intent?): IBinder? {
        return musicBind
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()

        player = MediaPlayer()
        initMusic()
    }

    fun setListener(onMusicComplete: OnMusicComplete) {
        this.onMusicComplete = onMusicComplete
    }

    fun initMusic() {
        player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        player.setAudioStreamType(AudioManager.STREAM_MUSIC)
        player.setOnPreparedListener(this)
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        player.stop()
        player.reset()
        player.release()
        return false
    }

    companion object {
        const val STOPPED = 0
        const val PAUSED = 1
        const val PLAYING = 2
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp!!.start()
        val duration = mp.duration
        seekBar.max = duration
        seekBar.postDelayed(progressRunner, interval)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration.toLong())
        end_point.text = String.format(
            "%d : %02d",
            minutes,
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) - TimeUnit.MINUTES.toSeconds(minutes))
    }

    private fun playMusic() {
        player.reset()

        val playMusic = musics
        val currentMusicId = playMusic.musicId
        val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentMusicId)

        player.setDataSource(applicationContext, trackUri)
        player.prepareAsync()
        progressRunner.run()
    }

    public fun pauseMusic() {
        player.pause()
        playerState = PAUSED
        seekBar.removeCallbacks(progressRunner)
    }

    public fun resumeMusic() {
        player.start()
        playerState = PLAYING
        progressRunner.run()
    }

    fun setUI(seekBar: SeekBar, start_int: TextView, end_int: TextView) {
        this.seekBar = seekBar
        start_point = start_int
        end_point = end_int
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    player.seekTo(progress)
                }
                val minutes = TimeUnit.MILLISECONDS.toMinutes(progress.toLong())
                start_point.text = String.format(
                    "%d : %02d",
                    minutes,
                    TimeUnit.MILLISECONDS.toSeconds(progress.toLong()) - TimeUnit.MINUTES.toSeconds(minutes))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
    }

    fun setMusic(musicModel: MusicModel) {
        musics = musicModel
        playerState = PLAYING
        playMusic()
    }

    private val progressRunner = object: Runnable {
        override fun run() {
            if(seekBar != null) {
                seekBar.progress = player.currentPosition
                if(player.isPlaying) {
                    seekBar.postDelayed(this, interval)
                }
            }
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        onMusicComplete.onMusicComplete()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return false
    }
}