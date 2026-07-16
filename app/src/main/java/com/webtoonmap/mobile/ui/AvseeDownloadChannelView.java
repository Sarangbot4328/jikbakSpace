package com.webtoonmap.mobile.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.webtoonmap.mobile.MainActivity;
import com.webtoonmap.mobile.R;
import com.webtoonmap.mobile.data.AvseeLibraryDatabase;
import com.webtoonmap.mobile.data.AvseeVideo;
import com.webtoonmap.mobile.download.AvseeDownloadService;
import com.webtoonmap.mobile.storage.AvseeStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class AvseeDownloadChannelView extends FrameLayout {
    private final MainActivity activity;
    private final AvseeLibraryDatabase database;
    private final VideoAdapter adapter;
    private final TextView empty;
    private final TextView status;
    private final SwipeRefreshLayout swipe;
    private boolean receiverRegistered;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(AvseeDownloadService.EXTRA_DONE, false)) refresh();
        }
    };

    public AvseeDownloadChannelView(MainActivity activity) {
        super(activity);
        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.channel_avsee_downloads, this, true);
        database = new AvseeLibraryDatabase(activity);
        status = findViewById(R.id.video_status);
        empty = findViewById(R.id.video_empty);
        swipe = findViewById(R.id.video_swipe);
        RecyclerView list = findViewById(R.id.video_list);
        list.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new VideoAdapter();
        list.setAdapter(adapter);
        findViewById(R.id.refresh_videos).setOnClickListener(v -> refresh());
        swipe.setOnRefreshListener(this::refresh);
        refresh();
    }

    public void refresh() {
        List<AvseeVideo> videos = database.listAll();
        adapter.replace(videos);
        empty.setVisibility(videos.isEmpty() ? VISIBLE : GONE);
        status.setText(videos.isEmpty() ? "저장한 AVSee 영상을 볼 수 있습니다" :
                "저장된 영상 " + videos.size() + "개");
        swipe.setRefreshing(false);
    }

    private void play(AvseeVideo video) {
        File file = new File(video.filePath);
        if (!file.isFile()) {
            Toast.makeText(activity, "영상 파일을 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(activity, VideoPlayerActivity.class)
                .putExtra(VideoPlayerActivity.EXTRA_TITLE, video.title)
                .putExtra(VideoPlayerActivity.EXTRA_PATH, video.filePath);
        activity.startActivity(intent);
    }

    private void confirmDelete(AvseeVideo video) {
        new AlertDialog.Builder(activity)
                .setTitle("영상 삭제")
                .setMessage("‘" + video.title + "’ 영상과 썸네일을 삭제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    AvseeStorage.deleteVideoFiles(video.filePath);
                    database.delete(video.id);
                    refresh();
                    Toast.makeText(activity, "삭제했습니다.", Toast.LENGTH_SHORT).show();
                }).show();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(activity, receiver,
                    new IntentFilter(AvseeDownloadService.ACTION_PROGRESS),
                    ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
        refresh();
    }

    @Override protected void onDetachedFromWindow() {
        if (receiverRegistered) {
            activity.unregisterReceiver(receiver);
            receiverRegistered = false;
        }
        super.onDetachedFromWindow();
    }

    private final class VideoAdapter extends RecyclerView.Adapter<VideoHolder> {
        private final List<AvseeVideo> items = new ArrayList<>();

        void replace(List<AvseeVideo> videos) {
            items.clear();
            items.addAll(videos);
            notifyDataSetChanged();
        }

        @Override public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_avsee_video, parent, false);
            return new VideoHolder(view);
        }

        @Override public void onBindViewHolder(VideoHolder holder, int position) {
            AvseeVideo video = items.get(position);
            holder.title.setText(video.title);
            holder.meta.setText(AvseeStorage.formatBytes(video.sizeBytes) + " · " + shortDate(video.createdAt));
            String detail = joinDetail(video.actors, video.tags);
            holder.detail.setText(detail.isEmpty() ? "AVSee 다운로드 영상" : detail);
            holder.thumbnail.setImageDrawable(null);
            if (video.thumbnailPath != null && new File(video.thumbnailPath).isFile()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                holder.thumbnail.setImageBitmap(BitmapFactory.decodeFile(video.thumbnailPath, options));
            }
            holder.itemView.setOnClickListener(v -> play(video));
            holder.play.setOnClickListener(v -> play(video));
            holder.delete.setOnClickListener(v -> confirmDelete(video));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static final class VideoHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final TextView title;
        final TextView meta;
        final TextView detail;
        final Button play;
        final Button delete;

        VideoHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.video_thumbnail);
            title = view.findViewById(R.id.video_title);
            meta = view.findViewById(R.id.video_meta);
            detail = view.findViewById(R.id.video_detail);
            play = view.findViewById(R.id.play_video);
            delete = view.findViewById(R.id.delete_video);
        }
    }

    private static String shortDate(String value) {
        if (value == null) return "";
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    private static String joinDetail(String actors, String tags) {
        String a = actors == null ? "" : actors.trim();
        String t = tags == null ? "" : tags.trim();
        if (a.isEmpty()) return t;
        if (t.isEmpty()) return a;
        return a + " · " + t;
    }
}
