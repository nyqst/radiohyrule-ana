package com.radiohyrule.android.listen;

import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.*;

public class Queue {
    protected static final String LOG_TAG = "com.radiohyrule.android.listen.Queue";

    protected static final int maxShortDelayRetries = 3;
    protected static final int shortRetryDelay = 2000; // milliseconds
    protected static final int longRetryDelay = 10000; // milliseconds


    protected QueueObserver observer;

    protected ScheduledExecutorService queryService;
    protected Future<?> queryServiceTask, scheduledQueryServiceTask;
    protected int queryRetryCount;

    protected java.util.Queue<NowPlaying.SongInfo> songInfoQueue = new java.util.LinkedList<NowPlaying.SongInfo>();


    public Queue() {
        queryService = Executors.newSingleThreadScheduledExecutor();
        reset();
    }

    public void setObserver(QueueObserver observer) {
        this.observer = observer;
    }

    public synchronized NowPlaying.SongInfo getCurrentSong() {
        return songInfoQueue.peek();
    }


    public void onPlayerConnectingToStream(boolean resetConnection) {
        // reset, but only if this is a new connection attempt (not a reconnection attempt due to network issues)
        if(resetConnection) {
            reset();
            queryNextSongInfo();
        }
    }

    public void onPlayerStopRequested() {
        reset();
    }


    protected synchronized void reset() {
        Log.d(LOG_TAG, "reset");

        cancelQuery();
        songInfoQueue.clear();
//        [self.metadataChangedEvents removeAllObjects];
        queryRetryCount = 0;
    }

    protected synchronized void cancelQuery() {
        // current loading operation
        if(queryServiceTask != null) {
            queryServiceTask.cancel(true);
            queryServiceTask = null;
        }
        // scheduled loading operation
        if(scheduledQueryServiceTask != null) {
            scheduledQueryServiceTask.cancel(false);
            scheduledQueryServiceTask = null;
        }
    }

    protected void queryNextSongInfo() { queryNextSongInfo(false); }
    protected synchronized void queryNextSongInfo(boolean sporadic) {
        if(queryServiceTask != null) return; // only one fetch operation at all times
        Log.d(LOG_TAG, "queryNextSongInfo");

        queryServiceTask = queryService.submit(new QueryRunnable(sporadic));
    }

    protected synchronized void retryQuery(final boolean sporadic) {
        Log.v(LOG_TAG, "retryQuery");

        // did we already retry many times with a short interval (and use the long interval instead now?)
        // (if we had metadatachanged in our player, we would not do retries with long delays and wait for
        // metadatachanged instead)
        long delay = longRetryDelay;
        if(!sporadic && queryRetryCount < maxShortDelayRetries) {
            delay = shortRetryDelay;
            queryRetryCount++;
        }

        // randomize the delay so that not all users of this app query the server at the same time
        delay += 1000 + new Random().nextInt(1000); // + 1..2

        // schedule next query
        scheduledQueryServiceTask = queryService.schedule(new Runnable() {
            @Override
            public void run() { queryNextSongInfo(sporadic); }
        }, delay, TimeUnit.MILLISECONDS);
    }

    protected synchronized void onQueryFinished(NowPlaying nowPlayingOrNull, Calendar queryStartTime, boolean sporadic) {
        queryServiceTask = null;
        Log.v(LOG_TAG, "onQueryFinished");

        if(nowPlayingOrNull == null) {
            retryQuery(sporadic);
            return;
        }
        NowPlaying.SongInfo newSong = nowPlayingOrNull.getSong();

        // is this a new song?
        boolean songExists = false;
        for(NowPlaying.SongInfo queueSong : songInfoQueue) {
            Long newSongTimeStarted = newSong.getTimeStarted();
            if(newSongTimeStarted != null) {
                Long queueSongTimeStarted = queueSong.getTimeStarted();
                if(queueSongTimeStarted != null) {
                    if(newSongTimeStarted.compareTo(queueSongTimeStarted) <= 0) {
                        songExists = true;
                        break;
                    }
                }
            }
        }
        if(songExists) {
            // this is still an old song, we are waiting for the new one
            retryQuery(sporadic);
            return;
        }

        // it's a new song

        queryRetryCount = 0; // reset

//        newSong.initialProgress = 0; // assume we start at the beginning of the song by default

        // start timer for querying next song info
        long timeElasped = 0;
        if(newSong.getDuration() != null) {
            timeElasped = nowPlayingOrNull.getTimeValue() - newSong.getTimeStartedValue();
            if(timeElasped < 0) timeElasped = 0;

            double timeLeft = 0;
            if(newSong.getDuration() != null) timeLeft = newSong.getDuration() - timeElasped; // playback time left on server

            // Add a small additional delay to reduce the probability that we arrive too early at the server.
            // We also randomize the delay so that not all users of this app query the server at the same time.
            timeLeft += 1 + new Random().nextDouble(); // + 1..2

            Calendar expectedPlaybackEndOnServer = queryStartTime; queryStartTime = null;
            expectedPlaybackEndOnServer.add(Calendar.MILLISECOND, (int)(timeLeft * 1000.0));
            long delay = expectedPlaybackEndOnServer.getTimeInMillis() - Calendar.getInstance(expectedPlaybackEndOnServer.getTimeZone()).getTimeInMillis();
            scheduledQueryServiceTask = queryService.schedule(new Runnable() {
                @Override
                public void run() { queryNextSongInfo(); }
            }, delay, TimeUnit.MILLISECONDS);

        } else {
            // unknown duration --> we query periodically (but sporadically) until we get a song with duration
            // if we had metadatachanged with our player we would use it instead
            retryQuery(true);
        }

        // append to songInfoQueue
        songInfoQueue.add(newSong);

        // notify about pending song info
        if(observer != null) observer.onNewPendingSong(newSong);

        if (songInfoQueue.size() == 1) {
            // if this is the first song in the songInfoQueue
            // inform the observer that this is the current song item

            // TODO
//            // if this is the first song in the songInfoQueue and the
//            // metadataChangedEvents queue is not empty, this is the current item
//            // and we are late delivering the it.
//            // (It wasn't there on metadataChanged yet.)
//            if(self.metadataChangedEvents.count > 0) {
//                NSNumber* audioPlayerStartTime = self.metadataChangedEvents[0];
//                newSong.audioPlayerStartTime = audioPlayerStartTime.doubleValue;
//                [self.metadataChangedEvents removeObjectAtIndex:0];
//            } else {
//                // It is the current song in the songInfoQueue and metadataChanged
//                // has not been fired yet. This means this is the very first song played.
//
//                // save time that has already elapsed
//                newSong.initialProgress = timeElapsed;
//
//                // We must add this song twice, because we expect a metadataChanged
//                // event and would move to the next item by removing the first song from
//                // the songInfoQueue. So we would actually remove this item too early.
//                // Adding it twice circumvents this issue.
//                newSong.duplicateCount++;
//                NSLogD(@"%@ will be a duplicate", newSong.title);
//            }

            notifyAboutCurrentSongChange();
        }
    }

    protected void notifyAboutCurrentSongChange() {
        if(observer != null) {
            observer.onCurrentSongChanged(getCurrentSong());
        }
    }


    class QueryRunnable implements Runnable {
        protected boolean sporadic;

        public QueryRunnable(boolean sporadic) {
            this.sporadic = sporadic;
        }

        @Override
        public void run() {
            Calendar startTime = Calendar.getInstance();
            NowPlaying nowPlaying = null;
            try {
                nowPlaying = fetchNowPlaying();
            } catch(Throwable e) {
                // TODO: better error handling
                Log.e(LOG_TAG, "Querying new song info failed with error: " + e.getMessage());
            } finally {
                onQueryFinished(nowPlaying, startTime, sporadic);
            }
        }

        private NowPlaying fetchNowPlaying() throws IOException, JSONException {
            NowPlaying nowPlaying = null;

            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet("http://radiohyrule.com/sites/radiohyrule.com/www/nowplaying.json.php");
            HttpResponse response = client.execute(request);

            Log.v(LOG_TAG, response.getStatusLine().toString());
            HttpEntity entity = response.getEntity();
            if(entity != null) {
                InputStream instream = entity.getContent();
                try {
                    String responseBody = convertStreamToString(instream);
                    JSONObject nowPlayingJson = new JSONObject(responseBody);

                    // parse JSON
                    nowPlaying = new NowPlaying();
                    if(!parseNowPlaying(nowPlayingJson, nowPlaying)) {
                        Log.e(LOG_TAG, "Failed to parse nowplaying.json from\n" + responseBody);
                        nowPlaying = null;
                    }

                } finally {
                    instream.close();
                }
            }

            return nowPlaying;
        }

        private boolean parseNowPlaying(JSONObject json, NowPlaying result) {
            result.setTime(parseJsonLong(json, "time"));

            try {
                return parseSongInfo(json.getJSONObject("nowplaying"), result.getSong());
            } catch(JSONException e) {
                return false;
            }
        }
        private boolean parseSongInfo(JSONObject json, NowPlaying.SongInfo result) {
            result.setTimeStarted(parseJsonLong(json, "started"));
            result.setNumListeners(parseJsonLong(json, "listeners"));

            result.setRequestUsername(parseJsonString(json, "request_user"));
            result.setRequestUrl(parseJsonString(json, "request_user_url"));

            result.setTitle(parseJsonString(json, "title"));
            result.setAlbum(parseJsonString(json, "album"));
            result.setAlbumCover(parseJsonString(json, "albumcover"));
            result.setSongUrl(parseJsonString(json, "song_url"));
            result.setDuration(parseJsonDouble(json, "duration"));

            result.clearArtists();
            try {
                JSONArray artistArrayJson = json.getJSONArray("artist");
                for(int i = 0; i < artistArrayJson.length(); ++i) {
                    try {
                        result.addArtist(artistArrayJson.getString(i));
                    } catch(JSONException e) { /* ignore */ }
                }
            } catch(JSONException e) { /* ignore */ }

            return true;
        }

        private Long parseJsonLong(JSONObject json, String name) {
            Long result = null;
            try { result = json.getLong(name); } catch(JSONException e) { /* ignore */ }
            return result;
        }
        private Double parseJsonDouble(JSONObject json, String name) {
            Double result = null;
            try { result = json.getDouble(name); } catch(JSONException e) { /* ignore */ }
            return result;
        }
        private String parseJsonString(JSONObject json, String name) {
            String result = null;
            try { result = json.getString(name); } catch(JSONException e) { /* ignore */ }
            return result;
        }

        private String convertStreamToString(InputStream is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            try {
                char[] buffer = new char[512];
                int charsRead;
                while((charsRead = reader.read(buffer, 0, 512)) != -1) {
                    sb.append(buffer, 0, charsRead);
                }
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    }

    public static interface QueueObserver {
        public void onNewPendingSong(NowPlaying.SongInfo song);
        public void onCurrentSongChanged(NowPlaying.SongInfo song);
    }
}
