package com.hagag.mahafez;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.graphics.Color;
import android.print.PrintAttributes;
import android.print.PrintManager;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE = 41;
    private WebView web;
    private ValueCallback<Uri[]> pendingChooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, false);
        }

        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (pendingChooser != null) pendingChooser.onReceiveValue(null);
                pendingChooser = cb;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, PICK_FILE);
                return true;
            }
        });

        if (savedInstanceState == null) web.loadUrl("file:///android_asset/index.html");
        else web.restoreState(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // close an open sheet first, then leave the app
                web.evaluateJavascript(
                    "(function(){var h=document.getElementById('sheetHost');if(h&&h.innerHTML){h.innerHTML='';return true}return false})()",
                    v -> { if (!"true".equals(v)) { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); } });
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req != PICK_FILE || pendingChooser == null) return;
        Uri[] r = (res == Activity.RESULT_OK && d != null && d.getData() != null) ? new Uri[]{d.getData()} : null;
        pendingChooser.onReceiveValue(r);
        pendingChooser = null;
    }

    class Bridge {
        /** Save a text file to the Downloads folder. Returns true on success. */
        @JavascriptInterface
        public boolean saveFile(String name, String content, String mime) {
            try {
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    v.put(MediaStore.Downloads.MIME_TYPE, mime);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MahafezCash");
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) return false;
                    try (OutputStream o = getContentResolver().openOutputStream(uri)) { o.write(bytes); }
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MahafezCash");
                    dir.mkdirs();
                    try (FileOutputStream o = new FileOutputStream(new File(dir, name))) { o.write(bytes); }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /** Print the current page: the system dialog offers "Save as PDF". */
        @JavascriptInterface
        public void print(String jobName) {
            runOnUiThread(() -> {
                try {
                    PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                    pm.print(jobName, web.createPrintDocumentAdapter(jobName),
                        new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "تعذّر فتح الطباعة", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /** Match the status and navigation bars to the page's theme. */
        @JavascriptInterface
        public void setBars(String hex, boolean light) {
            runOnUiThread(() -> {
                try {
                    int c = Color.parseColor(hex);
                    getWindow().setStatusBarColor(c);
                    getWindow().setNavigationBarColor(c);
                    web.setBackgroundColor(c);
                    WindowInsetsControllerCompat ic = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                    ic.setAppearanceLightStatusBars(light);
                    ic.setAppearanceLightNavigationBars(light);
                } catch (Exception ignored) {}
            });
        }

        /** Share a text file through the system share sheet. */
        @JavascriptInterface
        public void shareFile(String name, String content, String mime) {
            try {
                File dir = new File(getCacheDir(), "share");
                dir.mkdirs();
                File f = new File(dir, name);
                try (FileOutputStream o = new FileOutputStream(f)) { o.write(content.getBytes(StandardCharsets.UTF_8)); }
                Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType(mime);
                i.putExtra(Intent.EXTRA_STREAM, uri);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(Intent.createChooser(i, name)));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "تعذّرت المشاركة", Toast.LENGTH_SHORT).show());
            }
        }
    }
}
