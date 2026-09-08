package com.yourapp.exoplayer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

public class MainActivity extends AppCompatActivity {

    // Identify ourselves to servers/CDNs - some reject requests with no/blank User-Agent
    private static final String USER_AGENT = "MyExoPlayerApp/1.0 (Android)";

    private PlayerView playerView;
    private ExoPlayer player;
    private EditText urlInput;
    private Button playButton;
    private Button drmButton;
    private ProgressBar bufferingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find all UI elements
        playerView = findViewById(R.id.player_view);
        urlInput = findViewById(R.id.url_input);
        playButton = findViewById(R.id.play_button);
        drmButton = findViewById(R.id.drm_button);
        bufferingProgress = findViewById(R.id.buffering_progress);

        // Create ExoPlayer instance
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);

        // Play button - for normal M3U8/MPD/MP4 streams
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlInput.getText().toString().trim();
                if (!url.isEmpty()) {
                    playVideo(url, null);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a URL", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // DRM button - for protected streams (Widevine). Prompts for a real license URL,
        // since a hardcoded placeholder license server can never actually work.
        drmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlInput.getText().toString().trim();
                if (url.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a stream URL first", Toast.LENGTH_SHORT).show();
                    return;
                }
                promptForLicenseUrl(url);
            }
        });

        // Error listener for playback issues + buffering state
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                bufferingProgress.setVisibility(View.GONE);
                String reason = error.getMessage() != null ? error.getMessage() : "Unknown error";
                Toast.makeText(MainActivity.this,
                        "Playback error [" + error.errorCode + "]: " + reason,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_BUFFERING:
                        bufferingProgress.setVisibility(View.VISIBLE);
                        break;
                    case Player.STATE_READY:
                    case Player.STATE_ENDED:
                    case Player.STATE_IDLE:
                        bufferingProgress.setVisibility(View.GONE);
                        break;
                }
            }
        });
    }

    private void promptForLicenseUrl(final String streamUrl) {
        final EditText input = new EditText(this);
        input.setHint("https://your-license-server.com/license");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        new AlertDialog.Builder(this)
                .setTitle("Widevine License URL")
                .setMessage("Enter the DRM license server URL for this stream:")
                .setView(input)
                .setPositiveButton("Play", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String licenseUrl = input.getText().toString().trim();
                        if (licenseUrl.isEmpty()) {
                            Toast.makeText(MainActivity.this, "License URL cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        playVideo(streamUrl, licenseUrl);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void playVideo(String url, String licenseUrl) {
        // Stop and clear any previous playback before loading a new source
        player.stop();
        player.clearMediaItems();

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000);

        MediaItem mediaItem;

        // Check if DRM license URL is provided
        if (licenseUrl != null && !licenseUrl.isEmpty()) {
            // Build DRM MediaItem for protected content
            mediaItem = new MediaItem.Builder()
                    .setUri(url)
                    .setDrmConfiguration(
                            new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                                    .setLicenseUri(licenseUrl)
                                    .setMultiSession(false)
                                    .build()
                    )
                    .build();
        } else {
            // Build regular MediaItem
            mediaItem = MediaItem.fromUri(url);
        }

        MediaSource mediaSource;

        // Auto-detect stream type by file extension (falls back to extractors for anything else)
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".mpd")) {
            // DASH stream
            mediaSource = new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else if (lowerUrl.contains(".m3u8")) {
            // HLS stream
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else {
            // MP4 or other direct/progressive media - let ExoPlayer pick the right source
            player.setMediaItem(mediaItem);
            player.prepare();
            player.setPlayWhenReady(true);
            Toast.makeText(this, "Playing: " + url, Toast.LENGTH_SHORT).show();
            return;
        }

        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
        Toast.makeText(this, "Playing: " + url, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
        }
    }
}
