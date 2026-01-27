package com.hbisoft.hbrecorderexample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView sharedLinkTextView;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                String videoId = extractTikTokVideoId(sharedText);
                if (videoId != null) {
                    statusTextView.setText("TikTok video detected!\nVideo ID: " + videoId);
                    showToast("TikTok link received!");
                } else {
                    statusTextView.setText("TikTok link detected, but couldn't extract video ID");
                }
            } else {
                statusTextView.setText("Not a TikTok link");
            }
        }
    }

    private boolean isTikTokUrl(String url) {
        return url.contains("tiktok.com") || url.contains("vm.tiktok.com");
    }

    private String extractTikTokVideoId(String url) {
        // TikTok URLs can be in several formats:
        // https://www.tiktok.com/@username/video/1234567890123456789
        // https://vm.tiktok.com/ABC123xyz/
        // https://www.tiktok.com/t/ABC123xyz/

        // Try to extract video ID from full URL format
        Pattern fullUrlPattern = Pattern.compile("/video/(\\d+)");
        Matcher fullMatcher = fullUrlPattern.matcher(url);
        if (fullMatcher.find()) {
            return fullMatcher.group(1);
        }

        // For short URLs (vm.tiktok.com), we'd need to follow the redirect
        // For now, just return the short code
        Pattern shortUrlPattern = Pattern.compile("tiktok\\.com/(?:t/)?([A-Za-z0-9]+)/?$");
        Matcher shortMatcher = shortUrlPattern.matcher(url);
        if (shortMatcher.find()) {
            return "Short URL: " + shortMatcher.group(1);
        }

        return null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
