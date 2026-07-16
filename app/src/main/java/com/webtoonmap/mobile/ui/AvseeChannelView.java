package com.webtoonmap.mobile.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.webtoonmap.mobile.MainActivity;
import com.webtoonmap.mobile.R;
import com.webtoonmap.mobile.download.AvseeDownloadService;
import com.webtoonmap.mobile.storage.AvseeSettings;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AvseeChannelView extends FrameLayout {
    private static final String MOBILE_UA = "Mozilla/5.0 (Linux; Android 14) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36";
    private static final String PAGE_SCRIPT =
            "(function(){try{" +
            "var q=function(s){return document.querySelector(s)};" +
            "var m=function(n){var e=q('meta[property=\\\"'+n+'\\\"]');return e?e.content:''};" +
            "var abs=function(x,b){if(!x)return'';try{return new URL(x,b||location.href).href}catch(e){return''}};" +
            "var docs=[document];for(var di=0;di<docs.length;di++){var fr=docs[di].querySelectorAll('iframe');" +
            "for(var fi=0;fi<fr.length;fi++){try{if(fr[fi].contentDocument)docs.push(fr[fi].contentDocument)}catch(e){}}}" +
            "var u='';for(var di=0;di<docs.length&&!u;di++){var doc=docs[di];var vs=doc.querySelectorAll('video');" +
            "for(var vi=0;vi<vs.length&&!u;vi++){var v=vs[vi];var cs=[v.currentSrc,v.src,v.getAttribute('data-src')];" +
            "for(var ci=0;ci<cs.length;ci++){var z=abs(cs[ci],doc.baseURI);if(z&&z.indexOf('blob:')!==0&&z.indexOf('data:')!==0&&!/\\.html?(\\?|$)/i.test(z)){u=z;break}}" +
            "if(!u){var ss=v.querySelectorAll('source');for(var si=0;si<ss.length;si++){var z=abs(ss[si].src,doc.baseURI);if(z&&z.indexOf('blob:')!==0){u=z;break}}}}" +
            "if(!u){var es=doc.querySelectorAll('source[src],a[href],video[data-src]');for(var ei=0;ei<es.length;ei++){var z=abs(es[ei].src||es[ei].href||es[ei].getAttribute('data-src'),doc.baseURI);if(/\\.(mp4|webm|m4v|mov)(\\?|$)/i.test(z)){u=z;break}}}}" +
            "if(!u&&performance&&performance.getEntriesByType){var a=performance.getEntriesByType('resource');" +
            "for(var i=a.length-1;i>=0;i--){if(/\\.(mp4|webm|m4v|mov|m3u8)(\\?|$)/i.test(a[i].name)){u=a[i].name;break}}}" +
            "var d=q('meta[name=description]');var k=q('meta[name=keywords]');" +
            "return JSON.stringify({title:m('og:title')||document.title||'AVSee 영상'," +
            "video:u,thumb:m('og:image'),description:d?d.content:'',tags:k?k.content:''," +
            "actors:'',pageText:(document.body?document.body.innerText:'').slice(0,5000)});" +
            "}catch(e){return JSON.stringify({error:String(e)})}})()";

    private final MainActivity activity;
    private final WebView webView;
    private final ProgressBar progress;
    private final TextView status;
    private final Button navigationButton;
    private final Button downloadButton;
    private final Button stopButton;
    private volatile String lastMediaUrl = "";
    private volatile Map<String, String> lastMediaHeaders = Collections.emptyMap();
    private String pageTitle = "";
    private String pageThumb = "";
    private String pageTags = "";
    private String pageActors = "";
    private String pageDescription = "";
    private boolean receiverRegistered;
    private boolean clearHistoryOnNextPage;
    private boolean cleanCaptureInProgress;
    private int lastMediaScore = Integer.MIN_VALUE;
    private String lockedContentHost = "";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(AvseeDownloadService.EXTRA_MESSAGE);
            if (message != null) status.setText(message);
            if (intent.getBooleanExtra(AvseeDownloadService.EXTRA_DONE, false)) {
                activity.refreshDownloads();
                Toast.makeText(activity, message == null ? "다운로드 완료" : message,
                        Toast.LENGTH_LONG).show();
            } else if (intent.getBooleanExtra(AvseeDownloadService.EXTRA_ERROR, false)) {
                Toast.makeText(activity, message == null ? "다운로드 실패" : message,
                        Toast.LENGTH_LONG).show();
            }
            updateButtons();
        }
    };

    public AvseeChannelView(MainActivity activity) {
        super(activity);
        this.activity = activity;
        setBackgroundColor(Color.WHITE);

        webView = new WebView(activity);
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        LayoutParams progressParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(3));
        progressParams.gravity = Gravity.TOP;
        addView(progress, progressParams);

        status = new TextView(activity);
        status.setText("영상 페이지에서 재생한 뒤 다운로드를 누르세요.");
        status.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
        status.setTextSize(12);
        status.setBackgroundColor(0xEFFFFFFF);
        status.setPadding(dp(12), dp(7), dp(12), dp(7));
        LayoutParams statusParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.BOTTOM;
        statusParams.setMargins(0, 0, 0, dp(70));
        addView(status, statusParams);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setElevation(dp(8));

        stopButton = new Button(activity);
        stopButton.setText("중단");
        stopButton.setTextColor(Color.WHITE);
        stopButton.setTextSize(14);
        stopButton.setAllCaps(false);
        stopButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(198, 40, 40)));
        actions.addView(stopButton, new LinearLayout.LayoutParams(dp(92), dp(52)));

        navigationButton = new Button(activity);
        navigationButton.setText("\uC774\uB3D9 \u25BE");
        navigationButton.setTextColor(Color.WHITE);
        navigationButton.setTextSize(14);
        navigationButton.setAllCaps(false);
        navigationButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(70, 78, 90)));
        LinearLayout.LayoutParams navigationParams = new LinearLayout.LayoutParams(dp(78), dp(52));
        navigationParams.setMarginStart(dp(8));
        actions.addView(navigationButton, navigationParams);


        downloadButton = new Button(activity);
        downloadButton.setText("영상 다운로드");
        downloadButton.setTextColor(Color.WHITE);
        downloadButton.setTextSize(14);
        downloadButton.setAllCaps(false);
        downloadButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(activity, R.color.green)));
        LinearLayout.LayoutParams downloadParams = new LinearLayout.LayoutParams(dp(142), dp(52));
        downloadParams.setMarginStart(dp(8));
        actions.addView(downloadButton, downloadParams);
        LayoutParams actionParams = new LayoutParams(LayoutParams.WRAP_CONTENT, dp(52));
        actionParams.gravity = Gravity.END | Gravity.BOTTOM;
        actionParams.setMargins(dp(16), dp(16), dp(16), dp(12));
        addView(actions, actionParams);

        configureWebView();
        navigationButton.setOnClickListener(v -> showNavigationMenu());
        downloadButton.setOnClickListener(v -> startCleanCapture());
        stopButton.setOnClickListener(v -> {
            AvseeDownloadService.cancelAll(activity);
            status.setText("다운로드를 중단했습니다.");
            updateButtons();
        });
        webView.loadUrl(AvseeSettings.getBaseUrl(activity));
        updateButtons();
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(MOBILE_UA);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                lastMediaUrl = "";
                lastMediaHeaders = Collections.emptyMap();
                lastMediaScore = Integer.MIN_VALUE;
                status.setText(cleanCaptureInProgress ?
                        "광고를 제외하고 본 영상 주소를 찾는 중…" : "페이지를 불러오는 중…");
                updateButtons();
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    if (request.isForMainFrame() && shouldBlockExternalNavigation(uri)) {
                        Toast.makeText(activity, "외부 광고 페이지 이동을 차단했습니다.",
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                }
                try { activity.startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (Exception ignored) { }
                return true;
            }

            @Override public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isMediaRequest(request)) rememberMedia(url, request.getRequestHeaders());
                return super.shouldInterceptRequest(view, request);
            }

            @Override public void onPageFinished(WebView view, String url) {
                if (clearHistoryOnNextPage) {
                    view.clearHistory();
                    clearHistoryOnNextPage = false;
                }
                rememberContentHost(url);
                if (cleanCaptureInProgress) {
                    view.getSettings().setJavaScriptEnabled(true);
                    status.setText("광고 제외 페이지에서 본 영상 주소를 확인하는 중…");
                    postDelayed(() -> capturePageInfo(() -> {
                        cleanCaptureInProgress = false;
                        updateButtons();
                        confirmDownload();
                    }), 350L);
                    return;
                }
                capturePageInfo(null);
                status.setText("영상을 재생하면 다운로드 주소를 자동으로 감지합니다.");
                updateButtons();
            }
        });
    }

    private void startCleanCapture() {
        String pageUrl = webView.getUrl();
        if (!isHttpsUrl(pageUrl) || cleanCaptureInProgress) return;
        cleanCaptureInProgress = true;
        lastMediaUrl = "";
        lastMediaHeaders = Collections.emptyMap();
        lastMediaScore = Integer.MIN_VALUE;
        status.setText("광고를 제외하고 본 영상 주소를 찾는 중…");
        webView.getSettings().setJavaScriptEnabled(false);
        webView.reload();
        updateButtons();
    }

    private void rememberMedia(String url, Map<String, String> headers) {
        String lower = url.toLowerCase(Locale.US);
        String current = lastMediaUrl.toLowerCase(Locale.US);
        int score = scoreMediaCandidate(url, headers);
        if (score < lastMediaScore) return;
        if (current.contains(".mp4") && lower.contains(".m3u8")) return;
        lastMediaScore = score;
        lastMediaUrl = url;
        lastMediaHeaders = headers == null ? Collections.emptyMap() : new HashMap<>(headers);
        post(() -> {
            status.setText(lower.contains(".m3u8") ?
                    "HLS(m3u8) 영상이 감지됐습니다." : "영상 주소 감지 완료 · 다운로드할 수 있습니다.");
            updateButtons();
        });
    }

    private void capturePageInfo(Runnable after) {
        webView.evaluateJavascript(PAGE_SCRIPT, value -> {
            try {
                Object decoded = new JSONTokener(value).nextValue();
                String json = decoded instanceof String ? (String) decoded : value;
                JSONObject object = new JSONObject(json);
                pageTitle = clean(object.optString("title", webView.getTitle()));
                pageThumb = clean(object.optString("thumb", ""));
                pageTags = clean(object.optString("tags", ""));
                pageActors = clean(object.optString("actors", ""));
                pageDescription = clean(object.optString("description", ""));
                String detected = clean(object.optString("video", ""));
                if (isHttpsUrl(detected) && !sameDocumentUrl(detected, webView.getUrl())) {
                    rememberMedia(detected, Collections.emptyMap());
                }
            } catch (Exception ignored) { }
            if (after != null) after.run();
        });
    }

    private void confirmDownload() {
        String pageUrl = webView.getUrl();
        if (!isHttpsUrl(pageUrl)) {
            Toast.makeText(activity, "설정한 AVSee 사이트의 영상 페이지에서 사용해 주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String videoUrl = sameDocumentUrl(lastMediaUrl, pageUrl) ? "" : lastMediaUrl;
        if (videoUrl == null || videoUrl.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("영상 주소를 찾지 못했습니다")
                    .setMessage("페이지의 영상 재생 버튼을 누르고 2~3초 뒤 다시 시도하세요.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }
        if (videoUrl.toLowerCase(Locale.US).contains(".m3u8")) {
            Toast.makeText(activity, "현재 버전은 MP4 직접 영상만 다운로드할 수 있습니다.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String title = pageTitle.isEmpty() ? clean(webView.getTitle()) : pageTitle;
        String cookie = mergeCookies(header(lastMediaHeaders, "Cookie"),
                CookieManager.getInstance().getCookie(videoUrl),
                CookieManager.getInstance().getCookie(pageUrl));
        String referer = firstNonEmpty(header(lastMediaHeaders, "Referer"), pageUrl);
        String userAgent = firstNonEmpty(header(lastMediaHeaders, "User-Agent"), MOBILE_UA);
        String finalTitle = title.isEmpty() ? "AVSee 영상" : title;
        new AlertDialog.Builder(activity)
                .setTitle("영상 다운로드")
                .setMessage("‘" + finalTitle + "’ 영상을 기기에 저장합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("시작", (dialog, which) -> {
                    AvseeDownloadService.enqueue(activity, finalTitle, videoUrl, pageThumb,
                            pageUrl, pageTags, pageActors, pageDescription,
                            referer, cookie, userAgent);
                    status.setText("다운로드 대기열에 추가했습니다.");
                    updateButtons();
                }).show();
    }

    private void updateButtons() {
        boolean running = AvseeDownloadService.isRunning();
        stopButton.setVisibility(running ? View.VISIBLE : View.GONE);
        downloadButton.setText(cleanCaptureInProgress ? "영상 찾는 중…" :
                (running ? "대기열 추가" : "영상 다운로드"));
        downloadButton.setEnabled(!cleanCaptureInProgress);
        String url = webView.getUrl();
        boolean guidePage = url != null && AvseeSettings.isConfiguredHost(activity, url);
        boolean mediaDetected = lastMediaUrl != null && !lastMediaUrl.isEmpty();
        downloadButton.setVisibility(isHttpsUrl(url) && (!guidePage || mediaDetected)
                ? View.VISIBLE : View.GONE);
    }

    private void showNavigationMenu() {
        PopupMenu popup = new PopupMenu(activity, navigationButton);
        popup.getMenu().add(0, 1, 0, "\uB4A4\uB85C \uAC00\uAE30")
                .setEnabled(webView.canGoBack());
        popup.getMenu().add(0, 2, 1, "\uC55E\uC73C\uB85C \uAC00\uAE30")
                .setEnabled(webView.canGoForward());
        popup.getMenu().add(0, 3, 2, "\uBA54\uC778\uC73C\uB85C \uAC00\uAE30");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                webView.goBack();
                return true;
            }
            if (item.getItemId() == 2) {
                webView.goForward();
                return true;
            }
            if (item.getItemId() == 3) {
                goHome();
                return true;
            }
            return false;
        });
        popup.show();
    }


    public boolean canGoBack() { return webView.canGoBack(); }
    public void goBack() { webView.goBack(); }
    public void goHome() {
        clearHistoryOnNextPage = true;
        cleanCaptureInProgress = false;
        lockedContentHost = "";
        webView.getSettings().setJavaScriptEnabled(true);
        webView.stopLoading();
        webView.loadUrl(AvseeSettings.getBaseUrl(activity));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(activity, receiver,
                    new IntentFilter(AvseeDownloadService.ACTION_PROGRESS),
                    ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (receiverRegistered) {
            activity.unregisterReceiver(receiver);
            receiverRegistered = false;
        }
        super.onDetachedFromWindow();
    }

    public void destroyWebView() {
        if (receiverRegistered) {
            activity.unregisterReceiver(receiver);
            receiverRegistered = false;
        }
        webView.stopLoading();
        webView.destroy();
    }

    private void rememberContentHost(String url) {
        if (!isBoardPostUrl(url)) return;
        try {
            String host = Uri.parse(url).getHost();
            if (host != null && !host.isEmpty()) lockedContentHost = host.toLowerCase(Locale.US);
        } catch (Exception ignored) { }
    }

    private boolean shouldBlockExternalNavigation(Uri uri) {
        if (lockedContentHost.isEmpty() || uri == null || uri.getHost() == null) return false;
        return !hostMatches(lockedContentHost, uri.getHost());
    }

    private static boolean hostMatches(String allowed, String actual) {
        String a = allowed == null ? "" : allowed.toLowerCase(Locale.US);
        String b = actual == null ? "" : actual.toLowerCase(Locale.US);
        return !a.isEmpty() && (b.equals(a) || b.endsWith("." + a) || a.endsWith("." + b));
    }

    private static boolean isBoardPostUrl(String url) {
        if (!isHttpsUrl(url)) return false;
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            return path != null && path.toLowerCase(Locale.US).endsWith("/board.php") &&
                    uri.getQueryParameter("wr_id") != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int scoreMediaCandidate(String url, Map<String, String> headers) {
        String lower = url == null ? "" : url.toLowerCase(Locale.US);
        String accept = header(headers, "Accept").toLowerCase(Locale.US);
        int score = 0;
        if (lower.matches(".*\\.mp4(\\?.*)?(#.*)?$")) score += 100;
        else if (lower.matches(".*\\.(webm|m4v|mov)(\\?.*)?(#.*)?$")) score += 60;
        else if (lower.contains(".m3u8")) score += 10;
        if (accept.contains("video/")) score += 80;
        if (!header(headers, "Range").isEmpty()) score += 30;
        if (lower.matches(".*(doubleclick|googlesyndication|advert|/ads?[/?._-]|vast|pre-?roll|popunder|preview|trailer|sample|thumb).*")) {
            score -= 120;
        }
        return score;
    }

    private static boolean isMediaUrl(String url) {
        if (url == null || url.isEmpty() || url.startsWith("blob:")) return false;
        String lower = url.toLowerCase(Locale.US);
        return lower.matches(".*\\.(mp4|webm|m4v|mov|m3u8)(\\?.*)?(#.*)?$");
    }

    private static boolean isMediaRequest(WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (!isHttpsUrl(url)) return false;
        if (isMediaUrl(url)) return true;
        if (request.isForMainFrame()) return false;
        Map<String, String> headers = request.getRequestHeaders();
        String accept = header(headers, "Accept").toLowerCase(Locale.US);
        String range = header(headers, "Range");
        return !range.isEmpty() || accept.contains("video/") ||
                accept.contains("application/vnd.apple.mpegurl") ||
                accept.contains("application/x-mpegurl");
    }

    private static boolean sameDocumentUrl(String first, String second) {
        if (first == null || second == null) return false;
        try {
            String a = Uri.parse(first).buildUpon().fragment(null).build().toString();
            String b = Uri.parse(second).buildUpon().fragment(null).build().toString();
            return a.equals(b);
        } catch (Exception ignored) {
            return first.equals(second);
        }
    }

    private static boolean isHttpsUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        try {
            Uri uri = Uri.parse(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String header(Map<String, String> headers, String name) {
        if (headers == null) return "";
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) return clean(entry.getValue());
        }
        return "";
    }

    private static String mergeCookies(String... cookieHeaders) {
        Map<String, String> jar = new HashMap<>();
        for (String cookieHeader : cookieHeaders) {
            if (cookieHeader == null) continue;
            for (String part : cookieHeader.split(";")) {
                String item = part.trim();
                int equals = item.indexOf('=');
                if (equals > 0) jar.put(item.substring(0, equals).trim(), item.substring(equals + 1).trim());
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : jar.entrySet()) {
            if (out.length() > 0) out.append("; ");
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
