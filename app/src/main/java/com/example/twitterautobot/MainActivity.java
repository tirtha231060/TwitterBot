package com.example.twitterautobot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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
                
                Toast.makeText(MainActivity.this, "Bot চালু করতে Accessibility পারমিশন দিন", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
    }
}
