package com.radiohyrule.android.player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.util.Util;
import com.radiohyrule.android.BuildConfig;
import com.radiohyrule.android.R;
import com.radiohyrule.android.activities.NewMainActivity;
import com.radiohyrule.android.api.NowPlayingService;
import com.radiohyrule.android.api.types.SongInfo;

import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ExoService extends Service {

    private AudioManager audioManager;

    public enum PlaybackStatus {
        PLAYING, PAUSED, BUFFERING
    }

    public static final String URL_HYRULE_STREAM = "http://listen.radiohyrule.com:8000/listen.aac";
    public static final String USER_AGENT_PREFIX = "RadioHyrule-ANA";

    private static final double BACKOFF_COEFFICIENT = 1.0;

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final String LOG_TAG = ExoService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 63556;

    public interface PlaybackStatusListener {
        void onPlaybackStatusChanged(PlaybackStatus status);
        void onError(Throwable throwable);
        void onNewMetadataAvailable(@NonNull SongInfo songInfo);
    }

    private ExoPlayer exoPlayer;
    private Handler eventHandler;
    private NowPlayingService nowPlayingService;

    private int retryCount;
    private SongInfo cachedSongInfo;
    private double timeOffset = 0; //seconds

    private int lastAudioFocusState = -999;

    @Nullable
    private Call<SongInfo> songInfoCall;
    private Set<PlaybackStatusListener> listeners;

    @Override
    public IBinder onBind(Intent intent) {
        return new PlaybackBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if(BuildConfig.DEBUG) {
            //gonna need to switch to injection if this gets any more complex
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(interceptor);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NowPlayingService.BASE_URL)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        nowPlayingService = retrofit.create(NowPlayingService.class);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        listeners = new HashSet<>(1);
        eventHandler = new Handler();
        exoPlayer = ExoPlayer.Factory.newInstance(1, 500, 500);

        exoPlayer.addListener(exoPlayerListener);
    }

    @Override
    public void onDestroy() {
        Log.v(LOG_TAG, "Destroyed");
        super.onDestroy();
        eventHandler.removeCallbacksAndMessages(null);
        if (songInfoCall != null) songInfoCall.cancel();
        exoPlayer.release();
    }

    public void startPlayback() {
        startSelf();
        if (exoPlayer.getPlaybackState() == ExoPlayer.STATE_IDLE) {
            exoPlayer.prepare(createTrackRenderer());
            Log.v(LOG_TAG, "exoPlayer preparing");
        } else if (!exoPlayer.getPlayWhenReady()) {
            exoPlayer.seekTo(0);
            //move to "live edge" of stream if we are un-pausing
        }
        audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        exoPlayer.setPlayWhenReady(true);
        fetchSongInfo(true);
        startForeground(NOTIFICATION_ID, createNotification(cachedSongInfo));
    }

    public void stopPlayback() {
        exoPlayer.setPlayWhenReady(false);
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        //todo consider leaving running or staying foreground if we want to keep notification around?
        onPlaybackStopped();
    }

    /**
     * Registers a new listener to receive event callbacks from the service.<br />
     * Executes an immediate callback with the current playback status as soon as it is added
     */
    public void registerNewPlaybackStatusListener(PlaybackStatusListener listener) {
        listeners.add(listener);
        listener.onPlaybackStatusChanged(getCurrentPlaybackStatus());
    }

    public void unregisterListener(PlaybackStatusListener listener) {
        listeners.remove(listener);
    }

    @CheckResult
    public PlaybackStatus getCurrentPlaybackStatus() {
        return mapPlaybackStatus(exoPlayer.getPlayWhenReady(), exoPlayer.getPlaybackState());
    }

    public SongInfo getCachedSongInfo() {
        return cachedSongInfo;
    }

    /**
     * Local - offset = server
     * offset = local - server
     */
    public double getTimeOffset() {
        return timeOffset;
    }

    //call this anywhere state changes to not playing or buffering
    private void onPlaybackStopped() {
        Log.v(LOG_TAG, "Stopped");
        stopSelf(); //we'll actually keep running if something is bound to us
        stopForeground(false);
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, createNotification(null));
    }

    private void onSongInfoFetched(SongInfo songInfo) {
        cachedSongInfo = songInfo;
        if (songInfo.duration != 0 && (System.currentTimeMillis() / 1000.0) > songInfo.getEndTime() - 10) {
            //the song has ended or will end in less than 10 seconds, try to get metadata again
            Log.d(LOG_TAG, "Fetched Stale Metadata, expires in: " + ((System.currentTimeMillis() / 1000.0) - songInfo.getEndTime()));
            fetchAfterDelay(true);
        } else if (songInfo.duration <= 0) { //probably a live event, queue up a re-fetch
            double waitTimeSeconds = 1000 * (50 + 50 * Math.random()); //50-100 seconds
            Runnable fetchNextSongRunnable = new Runnable() {
                @Override
                public void run() {
                    fetchSongInfo(false);
                }
            };
            eventHandler.postDelayed(fetchNextSongRunnable, (long) (waitTimeSeconds * 1000));
        }
        if (exoPlayer.getPlayWhenReady()) {
            startForeground(NOTIFICATION_ID, createNotification(songInfo));
        }
        notifyMetadata(songInfo);
    }

    private Notification createNotification(@Nullable final SongInfo songInfo) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_player_service_notification); //todo get new icons. Triforce?
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        builder.setLargeIcon(icon);

        if (songInfo == null || songInfo.title == null) {
            // Default title to "Radio Hyrule", text to Playing/Paused
            builder.setContentTitle(getString(R.string.radio_hyrule));
            builder.setContentText(exoPlayer.getPlayWhenReady() ? getString(R.string.playing) : getString(R.string.paused));
        } else {
            builder.setContentTitle(songInfo.title);
            builder.setContentText(TextUtils.join(", ", songInfo.artists));
        }

        Intent intent = NewMainActivity.createIntent(this, NewMainActivity.TAG_LISTEN);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);
        return builder.build();
    }

    @NonNull
    private MediaCodecAudioTrackRenderer createTrackRenderer() {
        //Just a bunch of magic to build the ExoPlay TrackRenderer
        final Uri uri = Uri.parse(URL_HYRULE_STREAM);
        final String userAgent = Util.getUserAgent(this, USER_AGENT_PREFIX);

        final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(eventHandler, null);
        final Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        final IcyHttpDataSource dataSource = new IcyHttpDataSource(userAgent, null, bandwidthMeter);
        dataSource.setRequestProperty("Icy-MetaData", "1");
        dataSource.setIcyMetaDataCallback(new IcyInputStream.IcyMetadataCallback() {
            @Override
            public void playerMetadata(String key, String value) {
                Log.d(LOG_TAG, "ExoPlayer Metadata: { " + key + " : \"" + value + "\" }");
                fetchAfterDelay(false);
            }
        });

        final ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE, new AdtsExtractor());

        return new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT, null, true, eventHandler, audioTrackRendererEventListener,
                AudioCapabilities.getCapabilities(this), AudioManager.STREAM_MUSIC);
    }

    private void notifyStatusChanged(PlaybackStatus status) {
        for (PlaybackStatusListener listener : listeners) {
            listener.onPlaybackStatusChanged(status);
        }
    }

    private void notifyError(Throwable throwable) {
        for (PlaybackStatusListener listener : listeners) {
            listener.onError(throwable);
        }
    }

    private void notifyMetadata(SongInfo songInfo) {
        for (PlaybackStatusListener listener : listeners) {
            listener.onNewMetadataAvailable(songInfo);
        }
    }

    private void startSelf() {
        Log.v(LOG_TAG, "Started");
        Intent intent = new Intent(this, ExoService.class);
        startService(intent);
    }

    private void fetchSongInfo(boolean cancelPending) {
        if (songInfoCall != null && !songInfoCall.isCanceled()) {
            //there is a request running
            if (cancelPending) {
                songInfoCall.cancel();
            } else {
                return;
            }
        }

        final Call<SongInfo> localCall = nowPlayingService.nowPlaying();
        songInfoCall = localCall;

        localCall.enqueue(new Callback<SongInfo>() {
            @Override
            public void onResponse(Response<SongInfo> response) {
                if (!localCall.isCanceled()) {
                    songInfoCall = null;
                    if (response.isSuccess()) {
                        retryCount = 0;
                        SongInfo songInfo = response.body();
                        Date date = response.headers().getDate("Date");
                        if (date != null) {
                            timeOffset = (System.currentTimeMillis() - date.getTime())/1000.0;
                            Log.v(LOG_TAG, "Time offset: " + timeOffset);
                        }

                        onSongInfoFetched(songInfo);
                    } else {
                        Log.e(LOG_TAG, "HTTP Error Fetching SongInfo: " + response.code() + " - " + response.message());
                        //if this is anything other than 5xx, this is probably unrecoverable.
                        if (response.code() >= 500 || response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            fetchAfterDelay(true);
                        } //Todo notify user of unrecoverable failure? log Runtime Exception?
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.w(LOG_TAG, "Error Fetching SongInfo: ", t);
                fetchAfterDelay(true);
            }
        });
    }


    //Calculate exp backoff and schedule a retry
    private void fetchAfterDelay(boolean backoff) {
        double backoffMagnitude = backoff ? Math.floor(Math.random() * ++retryCount) : 0;

        double waitTimeSeconds = Math.max(BACKOFF_COEFFICIENT, timeOffset) * Math.pow(2, backoffMagnitude);
        //coefficient is either 1.0 or time offset

        Runnable fetchNextSongRunnable = new Runnable() {
            @Override
            public void run() {
                fetchSongInfo(false);
            }
        };
        eventHandler.postDelayed(fetchNextSongRunnable, (long) (waitTimeSeconds * 1000));
        Log.v(LOG_TAG, "Fetching new song in " + waitTimeSeconds + " seconds");
    }

    ExoPlayer.Listener exoPlayerListener = new ExoPlayer.Listener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            PlaybackStatus status;

            if (playbackState == ExoPlayer.STATE_READY) {
                Log.v(LOG_TAG, "exoPlayer Ready");
            } else if (playbackState == ExoPlayer.STATE_ENDED) {
                //This shouldn't happen. If it does, hopefully we can stop & restart
                exoPlayer.stop();
                onPlaybackStopped();
                notifyError(new IllegalStateException("ExoPlayer ended playback unexpectedly"));
            } else {
                Log.v(LOG_TAG, "exoPlayer waiting: " + playbackState);
            }

            status = mapPlaybackStatus(playWhenReady, playbackState);
            notifyStatusChanged(status);
        }

        @Override
        public void onPlayWhenReadyCommitted() { }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.w(LOG_TAG, error);
            onPlaybackStopped();
            notifyError(error);
        }
    };

    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_GAIN:
                    //todo this
//                    switch(lastAudioFocusState){
//                        case AudioManager.AUDIOFOCUS_LOSS:
//
//                            break;
//                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
//
//                            break;
//                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//
//                            break;
//                    }

                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    //stop indefinitely
                    stopPlayback();

                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    stopPlayback();

                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    stopPlayback();
                    //todo: player.sendMessage(audioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, 0.2f);
                    break;
            }

            lastAudioFocusState = focusChange;
        }
    };

    //Convert 2D ExoPlayer status into 1D status for clients
    private static PlaybackStatus mapPlaybackStatus(boolean playWhenReady, int playbackState) {
        PlaybackStatus status;
        switch (playbackState) {
            case ExoPlayer.STATE_READY:
                if (playWhenReady) {
                    //playing
                    status = PlaybackStatus.PLAYING;
                } else {
                    //paused
                    status = PlaybackStatus.PAUSED;
                }
                break;
            case ExoPlayer.STATE_IDLE:
                status = PlaybackStatus.PAUSED;
                break;
            default:
                status = PlaybackStatus.BUFFERING;

        }
        return status;
    }

    private MediaCodecAudioTrackRenderer.EventListener audioTrackRendererEventListener = new MediaCodecAudioTrackRenderer.EventListener() {

        @Override
        public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) { }

        @Override
        public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) { }

        @Override
        public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
            notifyError(e);
            Log.d(LOG_TAG, "MediaCodecAudioTrackRenderer onAudioTrackInitializationError Error: ", e);
        }

        @Override
        public void onAudioTrackWriteError(AudioTrack.WriteException e) {
            notifyError(e);
            Log.d(LOG_TAG, "MediaCodecAudioTrackRenderer onAudioTrackWriteError Error: ", e);
        }

        @Override
        public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
            notifyError(e);
            Log.d(LOG_TAG, "MediaCodecAudioTrackRenderer onDecoderInitializationError Error: ", e);
        }

        @Override
        public void onCryptoError(MediaCodec.CryptoException e) {
            notifyError(e);
            Log.d(LOG_TAG, "MediaCodecAudioTrackRenderer onCryptoError Error: ", e);
        }
    };

    public class PlaybackBinder extends Binder {
        private final ExoService exoService;

        public PlaybackBinder(ExoService service) {
            exoService = service;
        }

        public ExoService getExoService() {
            return exoService;
        }
    }
}
