package com.szatube;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.MediaStore;
import android.Manifest;
import android.os.Build;
import java.io.*;

public class MainActivity extends Activity {
    WebView web;
    private static final int REQ_PICK = 100;
    private static final int REQ_PERM = 200;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQ_PERM);
        }

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient());
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    public class Bridge {
        @JavascriptInterface
        public String listSzaVideos() {
            StringBuilder sb = new StringBuilder("[");
            File dir = new File("/sdcard/sza_tube/media/videos/");
            if (!dir.exists()) dir = new File("/storage/emulated/0/sza_tube/media/videos/");
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    boolean first = true;
                    for (File f : files) {
                        String n = f.getName().toLowerCase();
                        if (n.endsWith(".mp4")) {
                            if (!first) sb.append(",");
                            String base = f.getName().substring(0, f.getName().length()-4);
                            sb.append("{");
                            sb.append("\"id\":\"").append(esc(base)).append("\",");
                            sb.append("\"name\":\"").append(esc(f.getName())).append("\",");
                            sb.append("\"path\":\"file://").append(f.getAbsolutePath()).append("\"");
                            sb.append("}");
                            first = false;
                        }
                    }
                }
            }
            sb.append("]");
            return sb.toString();
        }

        @JavascriptInterface
        public String listMyVideos() {
            StringBuilder sb = new StringBuilder("[");
            File dir = new File("/sdcard/SzaTube/");
            if (!dir.exists()) dir.mkdirs();
            File[] files = dir.listFiles();
            if (files != null) {
                boolean first = true;
                for (File f : files) {
                    String n = f.getName().toLowerCase();
                    if (n.endsWith(".mp4")) {
                        if (!first) sb.append(",");
                        String base = f.getName().substring(0, f.getName().length()-4);
                        sb.append("{");
                        sb.append("\"id\":\"").append(esc(base)).append("\",");
                        sb.append("\"name\":\"").append(esc(f.getName())).append("\",");
                        sb.append("\"path\":\"file://").append(f.getAbsolutePath()).append("\"");
                        sb.append("}");
                        first = false;
                    }
                }
            }
            sb.append("]");
            return sb.toString();
        }

        @JavascriptInterface
        public void pickVideo() {
            Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            i.setType("video/*");
            startActivityForResult(i, REQ_PICK);
        }

        @JavascriptInterface
        public boolean deleteVideo(String name) {
            File f = new File("/sdcard/SzaTube/" + name);
            return f.delete();
        }

        private String esc(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String[] proj = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME};
            Cursor c = getContentResolver().query(uri, proj, null, null, null);
            if (c != null && c.moveToFirst()) {
                String path = c.getString(0);
                String name = c.getString(1);
                c.close();
                File dstDir = new File("/sdcard/SzaTube/");
                if (!dstDir.exists()) dstDir.mkdirs();
                File dst = new File(dstDir, name);
                try {
                    copy(new File(path), dst);
                    final String finalName = name;
                    web.post(new Runnable() {
                        public void run() {
                            web.evaluateJavascript("onUploaded('" + finalName.replace("'", "\\'") + "')", null);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void copy(File s, File d) throws IOException {
        try (InputStream in = new FileInputStream(s);
             OutputStream out = new FileOutputStream(d)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
    }
