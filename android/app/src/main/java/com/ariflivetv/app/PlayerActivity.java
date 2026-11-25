package com.ariflivetv.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.view.Gravity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("stream_url");
        String title = getIntent().getStringExtra("stream_title");
        if (url == null || url.trim().isEmpty()) { finish(); return; }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        TextView heading = new TextView(this);
        heading.setText(title == null ? "ARIF LIVE TV" : title);
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(18);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(16, 12, 16, 12);
        root.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        playerView = new PlayerView(this);
        playerView.setUseController(true);
        root.addView(playerView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(androidx.media3.common.PlaybackException error) {
                heading.setText("Playback error: " + error.errorCodeName);
            }
        });
    }

    @Override protected void onStop() {
        super.onStop();
        if (player != null) { player.release(); player = null; }
    }
}
