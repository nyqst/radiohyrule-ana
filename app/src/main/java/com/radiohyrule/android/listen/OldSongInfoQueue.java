package com.radiohyrule.android.listen;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.*;

public class OldSongInfoQueue implements SongInfoQueue {

    protected static final String LOG_TAG = OldSongInfoQueue.class.getCanonicalName();

    protected static final int maxShortDelayRetries = 3;
    protected static final int shortRetryDelay = 2000; // milliseconds
    protected static final int longRetryDelay = 10000; // milliseconds

    protected QueueObserver observer;

    protected ScheduledExecutorService queryService;
    protected Future<?> queryServiceTask, scheduledQueryServiceTask;
    protected int queryRetryCount;
    protected boolean initialQuery;

    protected java.util.Queue<SongInfo> songInfoQueue = new java.util.LinkedList<>();

    public OldSongInfoQueue() {
        queryService = Executors.newSingleThreadScheduledExecutor();
        reset();
    }

    @Override
    public void setObserver(QueueObserver observer) {
        this.observer = observer;
    }

    @Override
    public synchronized SongInfo getCurrentSong() {
        return songInfoQueue.peek();
    }

    @Override
    public synchronized SongInfo moveToNextSong() {
        songInfoQueue.poll();
        return getCurrentSong();
    }

    @Override
    public synchronized void onPlayerConnectingToStream(boolean resetConnection) {
        // reset, but only if this is a new connection attempt (not a reconnection attempt due to network issues)
        initialQuery = true;
        if (resetConnection) {
            reset();
            queryNextSongInfo();
        }
    }

    @Override
    public void onPlayerStopRequested() {
        reset();
    }

    protected synchronized void reset() {
        Log.d(LOG_TAG, "reset");

        cancelQuery();
        songInfoQueue.clear();
        queryRetryCount = 0;
        initialQuery = true;
    }

    protected synchronized void cancelQuery() {
        // current loading operation
        if (queryServiceTask != null) {
            queryServiceTask.cancel(true);
            queryServiceTask = null;
        }
        // scheduled loading operation
        if (scheduledQueryServiceTask != null) {
            scheduledQueryServiceTask.cancel(false);
            scheduledQueryServiceTask = null;
        }
    }

    protected void queryNextSongInfo() {
        queryNextSongInfo(false);
    }

    protected synchronized void queryNextSongInfo(boolean sporadic) {
        if (queryServiceTask != null) return; // only one fetch operation at all times
        Log.d(LOG_TAG, "queryNextSongInfo");

        queryServiceTask = queryService.submit(new QueryRunnable(sporadic));
    }

    protected synchronized void retryQuery(final boolean sporadic) {
        Log.v(LOG_TAG, "retryQuery");

        // did we already retry many times with a short interval (and use the long interval instead now?)
        // (if we had metadatachanged in our player, we would not do retries with long delays and wait for
        // metadatachanged instead)
        long delay = longRetryDelay;
        if (!sporadic && queryRetryCount < maxShortDelayRetries) {
            delay = shortRetryDelay;
            queryRetryCount++;
        }

        // randomize the delay so that not all users of this app query the server at the same time
        delay += 1000 + new Random().nextInt(1000); // + 1..2

        // schedule next query
        scheduledQueryServiceTask = queryService.schedule(new Runnable() {
            @Override
            public void run() {
                queryNextSongInfo(sporadic);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    protected synchronized void onQueryFinished(SongInfo songInfo, Calendar queryStartTime, boolean sporadic) {
        queryServiceTask = null;
        Log.v(LOG_TAG, "onQueryFinished");

        if (songInfo == null) {
            retryQuery(sporadic);
            return;
        }

        // is this a new song?
        boolean songExists = false;
        for (SongInfo queueSong : songInfoQueue) {
            Long newSongTimeStarted = songInfo.timeStarted;
            Long queueSongTimeStarted = queueSong.timeStarted;
            if (newSongTimeStarted.compareTo(queueSongTimeStarted) <= 0) {
                //newSong started at or before a song in queue, it can't be new
                songExists = true;
                break;
            }
        }
        if (songExists) {
            // this is still an old song, we are waiting for the new one
            retryQuery(sporadic);
            return;
        }

        // it's a new song

        // reset query state
        initialQuery = false;
        queryRetryCount = 0;

        // save information about elapsed time
        long timeElapsed = songInfo.timeStamp - songInfo.timeStarted;
        if (timeElapsed < 0) timeElapsed = 0;
        songInfo.setTimeElapsedAtStart(timeElapsed);

        // start timer for querying next song info
        if (songInfo.duration != null && songInfo.duration != 0) {
            double timeLeft = songInfo.duration - timeElapsed;

            // Add a small additional delay to reduce the probability that we arrive too early at the server.
            // We also randomize the delay so that not all users of this app query the server at the same time.
            timeLeft += 1 + Math.random(); // + 1..2

            //noinspection UnnecessaryLocalVariable
            Calendar expectedPlaybackEndOnServer = queryStartTime; //todo switch all this calendar nonsense to longs in epoch time
            expectedPlaybackEndOnServer.add(Calendar.MILLISECOND, (int) (timeLeft * 1000.0));
            long delay = expectedPlaybackEndOnServer.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
            scheduledQueryServiceTask = queryService.schedule(new Runnable() {
                @Override
                public void run() {
                    queryNextSongInfo();
                }
            }, delay, TimeUnit.MILLISECONDS);

        } else {
            // unknown duration --> we query periodically (but sporadically) until we get a song with duration
            // if we had metadatachanged with our player we would use it instead
            retryQuery(true);
        }

        // append to songInfoQueue
        songInfoQueue.add(songInfo);

        // notify about pending song info
        if (observer != null) observer.onNewPendingSong(songInfo);
    }

    class QueryRunnable implements Runnable {
        //todo rip literally all of this out, replace with retrofit
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
            } catch (Throwable e) {
                // TODO: better error handling
                Log.e(LOG_TAG, "Querying new song info failed with error", e);
            } finally {
                onQueryFinished(nowPlaying != null ? nowPlaying.getSong() : null, startTime, sporadic);
            }
        }

        private NowPlaying fetchNowPlaying() throws IOException, JSONException {
            NowPlaying nowPlaying = null;

            URL url = new URL("https://radiohyrule.com/sites/radiohyrule.com/www/nowplaying.json.php"); //todo looks like this url can be shortened?
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if (connection != null) {
                InputStream instream = connection.getInputStream();
                try {
                    String responseBody = convertStreamToString(instream);
                    JSONObject nowPlayingJson = new JSONObject(responseBody);

                    // parse JSON
                    nowPlaying = new NowPlaying();
                    if (!parseNowPlaying(nowPlayingJson, nowPlaying)) {
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
            //todo we probably don't need the 'time' field in json; the date header in the response works just fine, which means we can switch to the shorter url

            try {
                return parseSongInfo(json.getJSONObject("nowplaying"), result.getSong());
            } catch (JSONException e) {
                return false;
            }
        }

        private boolean parseSongInfo(JSONObject json, SongInfo result) {
            result.timeStarted = (parseJsonLong(json, "started"));
            result.numListeners = (int) parseJsonLong(json, "listeners");

            result.requestUsername = (parseJsonString(json, "request_user"));
            result.requestUrl = (parseJsonString(json, "request_user_url"));

            result.title = (parseJsonString(json, "title"));
            result.album = (parseJsonString(json, "album"));
            result.albumCover = (parseJsonString(json, "albumcover"));
            result.songUrl = (parseJsonString(json, "song_url"));
            result.duration = (parseJsonDouble(json, "duration"));

            result.artists = new ArrayList<>(1);
            try {
                JSONArray artistArrayJson = json.getJSONArray("artist");
                for (int i = 0; i < artistArrayJson.length(); ++i) {
                    try {
                        result.artists.add(artistArrayJson.getString(i));
                    } catch (JSONException e) { /* ignore */ }
                }
            } catch (JSONException e) { /* ignore */ }
            return true;
        }

        private long parseJsonLong(JSONObject json, String name) {
            long result = 0;
            try {
                result = json.getLong(name);
            } catch (JSONException e) { /* ignore */ }
            return result;
        }

        private Double parseJsonDouble(JSONObject json, String name) {
            Double result = null;
            try {
                result = json.getDouble(name);
            } catch (JSONException e) { /* ignore */ }
            return result;
        }

        private String parseJsonString(JSONObject json, String name) {
            String result = null;
            try {
                result = json.getString(name);
            } catch (JSONException e) { /* ignore */ }
            return result;
        }

        private String convertStreamToString(InputStream is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            try {
                char[] buffer = new char[512];
                int charsRead;
                while ((charsRead = reader.read(buffer, 0, 512)) != -1) {
                    sb.append(buffer, 0, charsRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    }

}
