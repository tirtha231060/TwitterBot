package com.example.twitterassistant;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView tvTotal, tvCompleted, tvPending, tvStatus;
    private SharedPreferences prefs;
    private ArrayList<String> linksList;
    private BroadcastReceiver updateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("TwitterTaskPrefs", MODE_PRIVATE);
        linksList = new ArrayList<>();

        tvTotal = findViewById(R.id.tvTotal);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvPending = findViewById(R.id.tvPending);
        tvStatus = findViewById(R.id.tvStatus);

        Button btnUpload = findViewById(R.id.btnUpload);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnPause = findViewById(R.id.btnPause);
        Button btnReset = findViewById(R.id.btnReset);

        loadSavedData();

        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        processSelectedFile(result.getData().getData());
                    }
                }
        );

        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            filePickerLauncher.launch(intent);
        });

        btnStart.setOnClickListener(v -> {
            prefs.edit().putBoolean("isRunning", true).apply();
            updateUI("Running...");
            triggerNextLink();
        });

        btnPause.setOnClickListener(v -> {
            prefs.edit().putBoolean("isRunning", false).apply();
            updateUI("Paused");
        });

        btnReset.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            linksList.clear();
            updateUI("Reset completed");
        });

        // Listen for updates from AccessibilityService
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.twitterassistant.UPDATE_UI".equals(intent.getAction())) {
                    loadSavedData();
                    String status = intent.getStringExtra("status");
                    if (status != null) tvStatus.setText("Current Action: " + status);
                }
            }
        };
        registerReceiver(updateReceiver, new IntentFilter("com.example.twitterassistant.UPDATE_UI"));
    }

    private void processSelectedFile(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            linksList.clear();
            JSONArray jsonArray = new JSONArray();
            while ((line = reader.readLine()) != null) {
                if (line.contains("twitter.com") || line.contains("x.com")) {
                    linksList.add(line.trim());
                    jsonArray.put(line.trim());
                }
            }
            reader.close();
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("linksArray", jsonArray.toString());
            editor.putInt("currentIndex", 0);
            editor.putBoolean("isRunning", false);
            editor.apply();

            Toast.makeText(this, "Loaded " + linksList.size() + " links", Toast.LENGTH_SHORT).show();
            loadSavedData();

        } catch (Exception e) {
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedData() {
        try {
            String jsonStr = prefs.getString("linksArray", "[]");
            JSONArray jsonArray = new JSONArray(jsonStr);
            int total = jsonArray.length();
            int current = prefs.getInt("currentIndex", 0);
            
            tvTotal.setText("Total Links: " + total);
            tvCompleted.setText("Completed: " + current);
            tvPending.setText("Pending: " + (total - current));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void triggerNextLink() {
        Intent serviceIntent = new Intent(this, MyAccessibilityService.class);
        serviceIntent.setAction("OPEN_NEXT");
        startService(serviceIntent);
    }

    private void updateUI(String statusText) {
        tvStatus.setText("Current Action: " + statusText);
        loadSavedData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateReceiver != null) {
            unregisterReceiver(updateReceiver);
        }
    }
}
