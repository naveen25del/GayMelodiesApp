package com.gaymelodies.app;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private View progressBar;
    private String startUrl;
    private boolean musicPlaying = false;
    private PauseAudioReceiver pauseReceiver;

    private class PlaybackBridge {
        @JavascriptInterface
        public void onPlay() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startMusicService();
                }
            });
        }

        @JavascriptInterface
        public void onMediaPause() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopMusicService();
                }
            });
        }
    }

    private class PauseAudioReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (webView != null) {
                webView.evaluateJavascript(
                        "document.querySelectorAll('audio,video').forEach(function(m){try{m.pause()}catch(e){}});",
                        null);
            }
            musicPlaying = false;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startUrl = getString(R.string.start_url);

        webView = findViewById(R.id.webview);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);

        pauseReceiver = new PauseAudioReceiver();
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(pauseReceiver,
                    new IntentFilter(MusicService.ACTION_PAUSE_AUDIO),
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(pauseReceiver,
                    new IntentFilter(MusicService.ACTION_PAUSE_AUDIO));
        }

        webView.addJavascriptInterface(new PlaybackBridge(), "GMPlayer");

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        // Standard WebView UA - custom suffixes can break site API calls
        settings.setUserAgentString(settings.getUserAgentString());

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                return host == null || !host.contains("gaymelodies.best");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
                hookMediaEvents(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(View.VISIBLE);
                ((android.widget.ProgressBar) progressBar).setProgress(newProgress);
                if (newProgress >= 100) {
                    progressBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                        }
                    }, 300);
                }
            }
        });

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(startUrl);
        }
    }

    /** Detect play/pause/ended of any media element inside the site and update the service. */
    private void hookMediaEvents(WebView view) {
        view.evaluateJavascript(
                "(function(){"
                        + "if(window.__gmHooked)return;window.__gmHooked=true;"
                        + "document.addEventListener('play',function(){try{GMPlayer.onPlay()}catch(e){}},true);"
                        + "document.addEventListener('pause',function(){try{GMPlayer.onMediaPause()}catch(e){}},true);"
                        + "document.addEventListener('ended',function(){try{GMPlayer.onMediaPause()}catch(e){}},true);"
                        + "})();", null);
    }

    private void startMusicService() {
        musicPlaying = true;
        Intent svc = new Intent(this, MusicService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }

    private void stopMusicService() {
        if (!musicPlaying) return;
        musicPlaying = false;
        stopService(new Intent(this, MusicService.class));
    }

    private boolean isServiceRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        for (ActivityManager.RunningServiceInfo info : am.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if (musicPlaying) {
                // Let the service keep the audio going; app moves to background
                moveTaskToBack(true);
                return;
            }
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (pauseReceiver != null) {
            unregisterReceiver(pauseReceiver);
        }
        if (!isServiceRunning()) {
            if (webView != null) {
                webView.loadUrl("about:blank");
                ((ViewGroup) webView.getParent()).removeView(webView);
                webView.destroy();
            }
        }
        super.onDestroy();
    }
}
