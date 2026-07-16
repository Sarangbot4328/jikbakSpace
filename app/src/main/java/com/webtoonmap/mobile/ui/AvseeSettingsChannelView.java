package com.webtoonmap.mobile.ui;

import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.webtoonmap.mobile.MainActivity;
import com.webtoonmap.mobile.R;
import com.webtoonmap.mobile.activation.ActivationStore;
import com.webtoonmap.mobile.storage.AvseeSettings;
import com.webtoonmap.mobile.storage.AvseeStorage;

public final class AvseeSettingsChannelView extends FrameLayout {
    private final MainActivity activity;
    private final EditText url;
    private final TextView storage;
    private final TextView activationUser;
    private final TextView version;

    public AvseeSettingsChannelView(MainActivity activity) {
        super(activity);
        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.channel_avsee_settings, this, true);
        url = findViewById(R.id.avsee_url);
        storage = findViewById(R.id.avsee_storage_path);
        activationUser = findViewById(R.id.activation_user);
        version = findViewById(R.id.avsee_app_version);
        findViewById(R.id.save_avsee_url).setOnClickListener(v -> saveUrl());
        refresh();
    }

    public void refresh() {
        url.setText(AvseeSettings.getBaseUrl(activity));
        storage.setText(AvseeStorage.root(activity).getAbsolutePath());
        activationUser.setText("등록 사용자: " + ActivationStore.getUserName(activity));
        try {
            String name = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
            version.setText("버전 " + name);
        } catch (Exception ignored) {
            version.setText("버전 1.0");
        }
    }

    private void saveUrl() {
        if (!AvseeSettings.setBaseUrl(activity, url.getText().toString())) {
            Toast.makeText(activity, "https://로 시작하는 올바른 주소를 입력해 주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        url.setText(AvseeSettings.getBaseUrl(activity));
        activity.applyChannelSettings();
        Toast.makeText(activity, "AVSee 주소를 저장했습니다.", Toast.LENGTH_SHORT).show();
    }
}
