package com.webtoonmap.mobile.ui;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.webtoonmap.mobile.R;

import java.io.File;

public final class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_PATH = "path";
    private static final String STATE_POSITION = "position";
    private VideoView player;
    private View playerRoot;
    private View toolbar;
    private View exitFullscreenButton;
    private int resumePosition;
    private boolean fullscreen;
    private boolean unlockAfterPortrait;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_video_player);
        playerRoot = findViewById(R.id.player_root);
        toolbar = findViewById(R.id.player_toolbar);
        exitFullscreenButton = findViewById(R.id.player_exit_fullscreen);
        SystemBarInsets.apply(this, playerRoot, true);
        findViewById(R.id.player_back).setOnClickListener(v -> finish());
        findViewById(R.id.player_fullscreen).setOnClickListener(v -> enterLandscapeFullscreen());
        exitFullscreenButton.setOnClickListener(v -> exitLandscapeFullscreen());
        TextView title = findViewById(R.id.player_title);
        title.setText(getIntent().getStringExtra(EXTRA_TITLE));
        player = findViewById(R.id.video_player);
        player.setKeepScreenOn(true);
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
        applyFullscreenUi(getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (fullscreen) exitLandscapeFullscreen();
                else finish();
            }
        });
    }

    private void enterLandscapeFullscreen() {
        unlockAfterPortrait = false;
        applyFullscreenUi(true);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private void exitLandscapeFullscreen() {
        unlockAfterPortrait = true;
        applyFullscreenUi(false);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void applyFullscreenUi(boolean enabled) {
        fullscreen = enabled;
        toolbar.setVisibility(enabled ? View.GONE : View.VISIBLE);
        exitFullscreenButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        if (enabled) {
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }
        ViewCompat.requestApplyInsets(playerRoot);
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        boolean landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
        applyFullscreenUi(landscape);
        if (!landscape && unlockAfterPortrait) {
            unlockAfterPortrait = false;
            player.postDelayed(() -> setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED), 300L);
        }
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
