package com.example.twitterautobot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etApiKey = findViewById(R.id.etApiKey);
        EditText etLinks = findViewById(R.id.etLinks);
        Button btnStartBot = findViewById(R.id.btnStartBot);

        btnStartBot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String apiKey = etApiKey.getText().toString().trim();
                String links = etLinks.getText().toString().trim();

                if (apiKey.isEmpty() || links.isEmpty()) {
                    Toast.makeText(MainActivity.this, "API Key এবং Links দিন", Toast.LENGTH_SHORT).show();
                    return;
                }

                // চেক করা হচ্ছে পারমিশন আগে থেকে দেওয়া আছে কিনা
                if (isAccessibilityServiceEnabled(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "বট সফলভাবে চালু হয়েছে এবং ব্যাকগ্রাউন্ডে কাজ করছে!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Bot চালু করতে Accessibility পারমিশন দিন", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                }
            }
        });
    }

    // Accessibility পারমিশন চেক করার মেথড
    private boolean isAccessibilityServiceEnabled(Context context) {
        int accessibilityEnabled = 0;
        final String service = context.getPackageName() + "/" + MyAccessibilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // Error handling
        }
        
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
