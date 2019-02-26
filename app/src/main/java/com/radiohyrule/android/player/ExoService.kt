package com.radiohyrule.android.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.annotation.CheckResult
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import com.crashlytics.android.Crashlytics
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.radiohyrule.android.R
import com.radiohyrule.android.activities.NewMainActivity
import com.radiohyrule.android.api.NowPlayingApi
import com.radiohyrule.android.api.types.SongInfo
import com.radiohyrule.android.injection.Injector
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import saschpe.exoplayer2.ext.icy.IcyHttpDataSourceFactory
import java.net.HttpURLConnection
import java.util.HashSet
import javax.inject.Inject

class ExoService : Service() {

    @Inject internal lateinit var nowPlayingApi: NowPlayingApi

    private lateinit var audioManager: AudioManager
    private lateinit var exoPlayer: SimpleExoPlayer
    
    private var eventHandler: Handler = Handler()

    private var retryCount: Int = 0
    var cachedSongInfo: SongInfo? = null
        private set
    /**
     * Local - offset = server
     * offset = local - server
     */
    var timeOffset = 0.0
        private set //seconds

    private var lastAudioFocusState = -999

    private var songInfoCall: Call<SongInfo>? = null
    private var listeners: MutableSet<PlaybackStatusListener> = HashSet(1)

    private var mediaActionIntentFilter: IntentFilter? = null
    private val mediaActionBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //handle media button and headphone unplug events
            if (intent.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                stopPlayback() //headphones or some such got unplugged.
            } else if (intent.action == Intent.ACTION_MEDIA_BUTTON) {
                val keyEvent = intent.extras?.get(Intent.EXTRA_KEY_EVENT) as KeyEvent?
                if (keyEvent == null || keyEvent.action != KeyEvent.ACTION_DOWN)
                    return
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> togglePlayback()
                    KeyEvent.KEYCODE_MEDIA_PLAY -> startPlayback() //NB this won't actually work right now, since we don't currently receive broadcasts while paused
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> stopPlayback()
                    KeyEvent.KEYCODE_MEDIA_STOP -> stopPlayback()
                }
            }
        }
    }

    private val currentPlaybackStatus
        get() = mapPlaybackStatus(exoPlayer.playWhenReady, exoPlayer.playbackState)

    private var exoPlayerListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            val status: PlaybackStatus = mapPlaybackStatus(playWhenReady, playbackState)

            if (playbackState == Player.STATE_READY) {
                Log.v(LOG_TAG, "exoPlayer Ready")
            } else if (playbackState == Player.STATE_ENDED) {
                //This shouldn't happen. If it does, hopefully we can stop & restart
                exoPlayer.stop()
                onPlaybackStopped()
                notifyError(IllegalStateException("ExoPlayer ended playback unexpectedly"))
            } else {
                Log.v(LOG_TAG, "exoPlayer waiting: $playbackState")
            }

            notifyStatusChanged(status)
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            Log.w(LOG_TAG, error)
            onPlaybackStopped()
            notifyError(error)
        }
    }

    private var audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN ->
                when (lastAudioFocusState) {
                    AudioManager.AUDIOFOCUS_LOSS -> { } // Why would this ever happen?
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> startPlayback()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        exoPlayer.volume = 1f
                    }
                }
            AudioManager.AUDIOFOCUS_LOSS ->
                //long term. eg phone call or other media player. stop indefinitely
                stopPlayback()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                // TODO pause or mute instead of stopping outright.
                stopPlayback()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                //stopPlayback();
                exoPlayer.volume = 0.2f
            }
        }
        lastAudioFocusState = focusChange
    }

    enum class PlaybackStatus {
        PLAYING, PAUSED, BUFFERING
    }

    interface PlaybackStatusListener {
        fun onPlaybackStatusChanged(status: PlaybackStatus)

        fun onError(throwable: Throwable?)

        fun onNewMetadataAvailable(songInfo: SongInfo)
    }

    override fun onBind(intent: Intent): IBinder? {
        return PlaybackBinder(this)
    }

    override fun onCreate() {
        super.onCreate()

        Injector.getComponent().inject(this)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this)
        
        exoPlayer.addListener(exoPlayerListener)

        mediaActionIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        mediaActionIntentFilter?.addAction(Intent.ACTION_MEDIA_BUTTON)

    }

    override fun onDestroy() {
        Log.v(LOG_TAG, "Destroyed")
        super.onDestroy()
        eventHandler.removeCallbacksAndMessages(null)
        songInfoCall?.cancel()
        exoPlayer.release()
    }

    fun startPlayback() {
        startSelf()
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare(createStreamMediaSource())
            Log.v(LOG_TAG, "exoPlayer preparing")
        } else if (!exoPlayer.playWhenReady) {
            exoPlayer.seekTo(0)
            //move to "live edge" of stream if we are un-pausing
        }

        val audioFocusResult = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        if (audioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            //bail out!
            stopPlayback()
            return
        } else {
            registerReceiver(mediaActionBroadcastReceiver, mediaActionIntentFilter)
        }

        exoPlayer.playWhenReady = true
        fetchSongInfo(true)
        startForeground(NOTIFICATION_ID, createNotification(cachedSongInfo))
    }

    fun stopPlayback() {
        unregisterReceiver(mediaActionBroadcastReceiver)
        exoPlayer.playWhenReady = false
        audioManager.abandonAudioFocus(audioFocusChangeListener)
        //todo consider leaving running or staying foreground if we want to keep notification around?
        onPlaybackStopped()
    }

    fun togglePlayback() {
        //BUFFERING & PLAYING -> PAUSED -> PLAYING
        val playbackStatus = currentPlaybackStatus
        when (playbackStatus) {
            ExoService.PlaybackStatus.PAUSED -> startPlayback()
            ExoService.PlaybackStatus.PLAYING, ExoService.PlaybackStatus.BUFFERING -> stopPlayback()
        }
    }

    /**
     * Registers a new listener to receive event callbacks from the service.<br></br>
     * Executes an immediate callback with the current playback status as soon as it is added
     */
    fun registerNewPlaybackStatusListener(listener: PlaybackStatusListener) {
        listeners.add(listener)
        listener.onPlaybackStatusChanged(currentPlaybackStatus)
    }

    fun unregisterListener(listener: PlaybackStatusListener) {
        listeners.remove(listener)
    }

    //call this anywhere state changes to not playing or buffering
    private fun onPlaybackStopped() {
        Log.v(LOG_TAG, "Stopped")
        stopSelf() //we'll actually keep running if something is bound to us
        stopForeground(false)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, createNotification(null))
    }

    private fun onSongInfoFetched(songInfo: SongInfo) {
        cachedSongInfo = songInfo
        if (songInfo.duration != 0.0 && System.currentTimeMillis() / 1000.0 > songInfo.endTime - 2) {
            //the song has ended or will end in less than 10 seconds, try to get metadata again
            val timeToEnd = System.currentTimeMillis() / 1000.0 - songInfo.endTime
            if(timeToEnd > 0) {
                Log.d(LOG_TAG, "Fetched stale metadata, already expired %.2f seconds ago".format(timeToEnd))
            } else {
                Log.d(LOG_TAG, "Fetched metadata, expires in %.2f seconds".format(-1*timeToEnd))
            }
            fetchAfterDelay(true)
        } else if (songInfo.duration <= 0) { //probably a live event, queue up a re-fetch
            val waitTimeSeconds = 1000 * (50 + 50 * Math.random()) //50-100 seconds
            val fetchNextSongRunnable = Runnable { fetchSongInfo(false) }
            eventHandler.postDelayed(fetchNextSongRunnable, (waitTimeSeconds * 1000).toLong())
        }
        if (exoPlayer.playWhenReady) {
            startForeground(NOTIFICATION_ID, createNotification(songInfo))
        }
        notifyMetadata(songInfo)
    }

    private fun createNotification(songInfo: SongInfo?): Notification {

        createNotificationChannel()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_player_service_notification) //todo get new icons. Triforce?
        val icon = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)
        builder.setLargeIcon(icon)

        if (songInfo?.title == null) {
            // Default title to "Radio Hyrule", text to Playing/Paused
            builder.setContentTitle(getString(R.string.radio_hyrule))
            builder.setContentText(getString(R.string.artist_unknown))
        } else {
            builder.setContentTitle(songInfo.title)
            builder.setContentText(TextUtils.join(", ", songInfo.artists))
        }

        val intent = NewMainActivity.createIntent(this, NewMainActivity.TAG_LISTEN)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        builder.setContentIntent(pendingIntent)
        return builder.build()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService<NotificationManager>(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createStreamMediaSource(): MediaSource {
        val uri = Uri.parse(URL_HYRULE_STREAM)
        val userAgent = Util.getUserAgent(this, USER_AGENT_PREFIX)
        // Custom HTTP data source factory which requests Icy metadata and parses it if
        // the stream server supports it
        val client = OkHttpClient.Builder().build()
        val icyHttpDataSourceFactory = IcyHttpDataSourceFactory.Builder(client)
                .setUserAgent(userAgent)
                .setIcyHeadersListener { icyHeaders ->
                    Log.d(LOG_TAG, "onIcyHeaders: %s".format(icyHeaders.toString()))
                }
                .setIcyMetadataChangeListener { icyMetadata ->
                    //metadata change just triggers a full re-fetch from the API, we don't actually need to read/parse it
                    Log.d(LOG_TAG, "onIcyMetaData: %s".format(icyMetadata.toString()))
                    fetchAfterDelay(false)
                }
                .build()

        // Produces DataSource instances through which media data is loaded
        val dataSourceFactory = DefaultDataSourceFactory(applicationContext, null, icyHttpDataSourceFactory)

        // The MediaSource represents the media to be played

        return ExtractorMediaSource.Factory(dataSourceFactory)
                .setExtractorsFactory(DefaultExtractorsFactory())
                .createMediaSource(uri)
    }

    private fun notifyStatusChanged(status: PlaybackStatus) {
        for (listener in listeners) {
            listener.onPlaybackStatusChanged(status)
        }
    }

    private fun notifyError(throwable: Throwable?) {
        for (listener in listeners) {
            listener.onError(throwable)
        }
        Crashlytics.getInstance().core.logException(throwable)
    }

    private fun notifyMetadata(songInfo: SongInfo) {
        for (listener in listeners) {
            listener.onNewMetadataAvailable(songInfo)
        }
    }

    private fun startSelf() {
        Log.v(LOG_TAG, "Started")
        val intent = Intent(this, ExoService::class.java)
        startService(intent)
    }

    private fun fetchSongInfo(cancelPending: Boolean) {
        if (songInfoCall != null && !songInfoCall!!.isCanceled) {
            //there is a request running
            if (cancelPending) {
                songInfoCall?.cancel()
            } else {
                return
            }
        }

        val localCall = nowPlayingApi.nowPlaying()
        songInfoCall = localCall

        localCall.enqueue(object : Callback<SongInfo> {
            override fun onResponse(call: Call<SongInfo>, response: Response<SongInfo>) {
                if (!localCall.isCanceled) {
                    songInfoCall = null
                    if (response.isSuccessful) {
                        Log.v(LOG_TAG, "Reset retryCount from $retryCount")
                        // TODO only reset this when the document changes
                        retryCount = 0
                        val songInfo = response.body()
                        val date = response.headers().getDate("Date")
                        if (date != null) {
                            timeOffset = (System.currentTimeMillis() - date.time) / 1000.0
                            Log.v(LOG_TAG, "Time offset: $timeOffset")
                        }

                        onSongInfoFetched(songInfo!!)
                    } else {
                        Log.e(LOG_TAG, "HTTP Error Fetching SongInfo: " + response.code() + " - " + response.message())
                        //if this is anything other than 5xx, this is probably unrecoverable.
                        if (response.code() >= 500 || response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            fetchAfterDelay(true)
                        } //Todo notify user of unrecoverable failure? log Runtime Exception?
                    }
                }
            }

            override fun onFailure(call: Call<SongInfo>, t: Throwable) {
                //todo check call for manual cancellation and swallow errors
                Log.w(LOG_TAG, "Error Fetching SongInfo: ", t)
                Crashlytics.getInstance().core.logException(t)
                fetchAfterDelay(true)
            }
        })
    }


    //Calculate exp backoff and schedule a retry
    private fun fetchAfterDelay(backoff: Boolean) {
        val backoffMagnitude = if (backoff) Math.floor(Math.random() * ++retryCount) else 0.toDouble()

        val waitTimeSeconds = Math.max(BACKOFF_COEFFICIENT, timeOffset) * Math.pow(2.0, backoffMagnitude)
        //coefficient is either 1.0 or time offset

        val fetchNextSongRunnable = Runnable { fetchSongInfo(false) }
        eventHandler.postDelayed(fetchNextSongRunnable, (waitTimeSeconds * 1000).toLong())
        Log.v(LOG_TAG, "Fetching new song in $waitTimeSeconds seconds ($retryCount, $backoffMagnitude)")
    }

    inner class PlaybackBinder(val exoService: ExoService) : Binder()

    companion object {

        const val URL_HYRULE_STREAM = "http://listen.radiohyrule.com:8000/listen.aac"
        const val USER_AGENT_PREFIX = "RadioHyrule-ANA"

        const val CHANNEL_ID = "radiohyrule.notification.foreground"

        private const val BACKOFF_COEFFICIENT = 1.0

        private const val LOG_TAG = "HyrulePlayer"
        private const val NOTIFICATION_ID = 63556

        //Convert 2D ExoPlayer status into 1D status for clients
        @CheckResult
        private fun mapPlaybackStatus(playWhenReady: Boolean, playbackState: Int): PlaybackStatus {
            return when (playbackState) {
                Player.STATE_READY -> if (playWhenReady) {
                    //playing
                    PlaybackStatus.PLAYING
                } else {
                    //paused
                    PlaybackStatus.PAUSED
                }
                Player.STATE_IDLE -> PlaybackStatus.PAUSED
                else -> PlaybackStatus.BUFFERING
            }
        }
    }
}
