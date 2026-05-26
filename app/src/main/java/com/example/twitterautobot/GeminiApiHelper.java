package com.example.twitterassistant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiApiHelper {

    // আপনার জেমিনি এপিআই কি এখানে দিন
    private static final String API_KEY = "YOUR_GEMINI_API_KEY_HERE";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface GeminiCallback {
        void onSuccess(String reply);
        void onError(String error);
    }

    public static void generateReply(String tweetText, GeminiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // প্রম্পট বিল্ড করা (JSON Format)
                String prompt = "Analyze this tweet and write a short, highly contextual, conversational, and natural reply in English (or the language of the tweet). Do not use AI buzzwords. Make it sound like a real human responding to a friend. Keep it under 150 characters. Tweet: " + tweetText;
                
                JSONObject parts = new JSONObject();
                parts.put("text", prompt);
                
                JSONArray partsArray = new JSONArray();
                partsArray.put(parts);
                
                JSONObject contents = new JSONObject();
                contents.put("parts", partsArray);
                
                JSONArray contentsArray = new JSONArray();
                contentsArray.put(contents);
                
                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contentsArray);

                // ডেটা পাঠানো
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    // রেসপন্স পার্স করা
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String generatedText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                            
                    // UI থ্রেডের মাধ্যমে কলব্যাক
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(generatedText.trim().replaceAll("\"", ""));
                    });
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Server Error: " + responseCode);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError(e.getMessage());
                });
            }
        });
    }
}

