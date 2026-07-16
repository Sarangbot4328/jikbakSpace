package com.webtoonmap.mobile.ui;

import android.os.Bundle;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.webtoonmap.mobile.R;

import java.io.File;

public final class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PATH = "path";
    private static final String STATE_POSITION = "position";
    private VideoView player;
    private int resumePosition;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_video_player);
        SystemBarInsets.apply(this, findViewById(R.id.player_root), true);
        findViewById(R.id.player_back).setOnClickListener(v -> finish());
        TextView title = findViewById(R.id.player_title);
        title.setText(getIntent().getStringExtra(EXTRA_TITLE));
        player = findViewById(R.id.video_player);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null || !new File(path).isFile()) {
            Toast.makeText(this, "영상 파일을 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (state != null) resumePosition = state.getInt(STATE_POSITION, 0);
        MediaController controls = new MediaController(this);
        controls.setAnchorView(player);
        player.setMediaController(controls);
        player.setVideoPath(path);
        player.setOnPreparedListener(mediaPlayer -> {
            if (resumePosition > 0) player.seekTo(resumePosition);
            player.start();
        });
        player.setOnErrorListener((mediaPlayer, what, extra) -> {
            Toast.makeText(this, "이 영상 형식은 기기 기본 플레이어에서 재생할 수 없습니다.",
                    Toast.LENGTH_LONG).show();
            return true;
        });
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_POSITION, player == null ? 0 : player.getCurrentPosition());
        super.onSaveInstanceState(outState);
    }

    @Override protected void onPause() {
        if (player != null) {
            resumePosition = player.getCurrentPosition();
            player.pause();
        }
        super.onPause();
    }
}
