package com.yourapp.exoplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

public class MainActivity extends AppCompatActivity {
    
    private PlayerView playerView;
    private ExoPlayer player;
    private EditText urlInput;
    private Button playButton;
    private Button drmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Find all UI elements
        playerView = findViewById(R.id.player_view);
        urlInput = findViewById(R.id.url_input);
        playButton = findViewById(R.id.play_button);
        drmButton = findViewById(R.id.drm_button);
        
        // Create ExoPlayer instance
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        
        // Play button - for normal M3U8/MPD streams
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
        
        // DRM button - for protected streams (Widevine)
        drmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlInput.getText().toString().trim();
                if (!url.isEmpty()) {
                    // IMPORTANT: User must provide their own license URL
                    String licenseUrl = "https://license.yourservice.com/license"; // CHANGE THIS
                    playVideo(url, licenseUrl);
                } else {
                    Toast.makeText(MainActivity.this, "Enter MPD URL first", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Error listener for playback issues
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void playVideo(String url, String licenseUrl) {
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        
        MediaItem mediaItem;
        
        // Check if DRM license URL is provided
        if (licenseUrl != null && !licenseUrl.isEmpty()) {
            // Build DRM MediaItem for protected content
            mediaItem = new MediaItem.Builder()
                .setUri(url)
                .setDrmConfiguration(
                    new MediaItem.DrmConfiguration.Builder(MediaItem.DrmConfiguration.WIDEVINE_UUID)
                        .setLicenseUri(licenseUrl)
                        .build()
                )
                .build();
        } else {
            // Build regular MediaItem
            mediaItem = MediaItem.fromUri(url);
        }
        
        // Auto-detect stream type by file extension
        if (url.endsWith(".mpd")) {
            // DASH stream
            DashMediaSource dashSource = new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
            player.setMediaSource(dashSource);
        } else if (url.endsWith(".m3u8")) {
            // HLS stream
            HlsMediaSource hlsSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
            player.setMediaSource(hlsSource);
        } else {
            // MP4 or other direct video
            player.setMediaItem(mediaItem);
        }
        
        // Start playback
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
