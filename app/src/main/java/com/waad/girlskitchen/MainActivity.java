package com.waad.girlskitchen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

public class MainActivity extends Activity {

    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final Locale ttsLocale = new Locale("ar");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(0xFFFFF7EF);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) { }
                    return true;
                }
                return false;
            }
        });
        web.addJavascriptInterface(new Bridge(), "Native");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS || tts == null) return;
            try {
                int avail = tts.isLanguageAvailable(ttsLocale);
                if (avail >= TextToSpeech.LANG_AVAILABLE) {
                    tts.setLanguage(ttsLocale);
                } else {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setSpeechRate(0.96f);
                tts.setPitch(1.0f);
                ttsReady = true;
            } catch (Exception ignored) { }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) {
                runOnUiThread(() -> js("window.onSpeechDone && onSpeechDone()"));
            }
            @Override public void onError(String id) { }
        });
    }

    private void js(String code) {
        if (web != null) web.evaluateJavascript(code, null);
    }

    public class Bridge {
        @JavascriptInterface
        public void speak(String text) {
            if (!ttsReady || tts == null || text == null) return;
            String t = text.trim();
            if (t.isEmpty()) return;
            try {
                tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "g" + System.nanoTime());
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void stopSpeaking() {
            if (tts != null) { try { tts.stop(); } catch (Exception ignored) { } }
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            try {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) v.vibrate(Math.max(10, Math.min(400, ms)));
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public boolean ttsAvailable() { return ttsReady; }
    }

    @Override
    public void onBackPressed() {
        if (web == null) { super.onBackPressed(); return; }
        web.evaluateJavascript("(window.gameBack ? gameBack() : false)", value -> {
            if (!"true".equals(value)) finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { try { tts.stop(); tts.shutdown(); } catch (Exception ignored) { } }
        if (web != null) { try { web.destroy(); } catch (Exception ignored) { } }
        super.onDestroy();
    }
}
