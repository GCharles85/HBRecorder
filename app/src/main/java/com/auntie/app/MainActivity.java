package com.auntie.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String RAPIDAPI_KEY = BuildConfig.RAPIDAPI_KEY;
    private static final String RAPIDAPI_HOST = BuildConfig.RAPIDAPI_HOST;

    private TextView sharedLinkTextView;
    private TextView statusTextView;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        httpClient = new OkHttpClient();
        initViews();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void initViews() {
        sharedLinkTextView = findViewById(R.id.shared_link_text);
        statusTextView = findViewById(R.id.status_text);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSharedText(intent);
            }
        }
    }

    private void handleSharedText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            sharedLinkTextView.setText(sharedText);

            if (isTikTokUrl(sharedText)) {
                statusTextView.setText("TikTok link detected! Fetching video...");
                showToast("TikTok link received!");
                fetchTikTokVideo(sharedText);
            } else {
                statusTextView.setText("Not a TikTok link");
            }
        }
    }

    private boolean isTikTokUrl(String url) {
        return url.contains("tiktok.com") || url.contains("vm.tiktok.com");
    }

    private void fetchTikTokVideo(String tiktokUrl) {
        // First resolve shortened URLs (like /t/ links) to full URLs
        resolveAndFetchVideo(tiktokUrl.trim());
    }

    private void resolveAndFetchVideo(String tiktokUrl) {
        // Check if this is a shortened URL that needs resolving
        if (tiktokUrl.contains("/t/") || tiktokUrl.contains("vm.tiktok.com")) {
            Request resolveRequest = new Request.Builder()
                    .url(tiktokUrl)
                    .head()
                    .build();

            httpClient.newCall(resolveRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // If HEAD fails, try with original URL anyway
                    callTikTokApi(tiktokUrl);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    String resolvedUrl = response.request().url().toString();
                    response.close();
                    callTikTokApi(resolvedUrl);
                }
            });
        } else {
            callTikTokApi(tiktokUrl);
        }
    }

    private void callTikTokApi(String tiktokUrl) {
        runOnUiThread(() -> statusTextView.setText("Fetching video for:\n" + tiktokUrl));

        String encodedUrl = URLEncoder.encode(tiktokUrl, StandardCharsets.UTF_8);
        String apiUrl = "https://" + RAPIDAPI_HOST + "/media?videoUrl=" + encodedUrl;

        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("x-rapidapi-key", RAPIDAPI_KEY)
                .addHeader("x-rapidapi-host", RAPIDAPI_HOST)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    statusTextView.setText("Failed to fetch video: " + e.getMessage());
                    showToast("Network error");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String downloadUrl = json.optString("downloadUrl", null);

                        if (downloadUrl != null && !downloadUrl.isEmpty()) {
                            runOnUiThread(() -> {
                                statusTextView.setText("Video found! Starting download...");
                                downloadVideo(downloadUrl);
                            });
                        } else {
                            runOnUiThread(() -> {
                                statusTextView.setText("No download URL in response:\n" + responseBody);
                            });
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            statusTextView.setText("Failed to parse response: " + e.getMessage());
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        statusTextView.setText("API error " + response.code() + ":\n" + responseBody);
                    });
                }
            }
        });
    }

    private void downloadVideo(String videoUrl) {
        try {
            String fileName = "tiktok_" + System.currentTimeMillis() + ".mp4";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl))
                    .setTitle("TikTok Video")
                    .setDescription("Downloading video...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                statusTextView.setText("Download started!\nFile: " + fileName);
                showToast("Download started");
            } else {
                statusTextView.setText("Download manager not available");
            }
        } catch (Exception e) {
            statusTextView.setText("Download failed: " + e.getMessage());
            showToast("Download failed");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
