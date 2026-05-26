package com.example.twitterassistant;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;

import java.util.Random;

public class MyAccessibilityService extends AccessibilityService {

    private boolean isProcessing = false;
    private String currentTweetText = "";
    private SharedPreferences prefs;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    @Override
    protected void onServiceConnected() {
        prefs = getSharedPreferences("TwitterTaskPrefs", MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "OPEN_NEXT".equals(intent.getAction())) {
            openNextLink();
        }
        return START_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!prefs.getBoolean("isRunning", false)) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        int eventType = event.getEventType();

        // ১. স্ক্রিন রিড করা এবং রিপ্লাই বক্স খোঁজা
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (!isProcessing) {
                AccessibilityNodeInfo replyBox = findEditText(rootNode);
                if (replyBox != null) {
                    isProcessing = true;
                    broadcastStatus("Reading tweet...");
                    
                    // টুইটের মূল টেক্সট বের করার চেষ্টা
                    String tweet = findLongestText(rootNode);
                    if (tweet.isEmpty()) tweet = "General gaming/esports tweet"; 
                    
                    broadcastStatus("Generating AI reply...");
                    GeminiApiHelper.generateReply(tweet, new GeminiApiHelper.GeminiCallback() {
                        @Override
                        public void onSuccess(String reply) {
                            simulateHumanTyping(replyBox, reply);
                        }

                        @Override
                        public void onError(String error) {
                            broadcastStatus("AI Error: " + error);
                            isProcessing = false;
                        }
                    });
                }
            }
        }

        // ২. ইউজারের "Send" বা "Reply" ক্লিক ডিটেক্ট করা
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            CharSequence textDesc = event.getText() != null ? event.getText().toString() : "";
            CharSequence contentDesc = event.getContentDescription();
            String fullDesc = (textDesc.toString() + " " + (contentDesc != null ? contentDesc.toString() : "")).toLowerCase();

            if (fullDesc.contains("reply") || fullDesc.contains("post") || fullDesc.contains("send")) {
                broadcastStatus("Send clicked manually. Waiting for next link...");
                isProcessing = false;
                
                // ৫ থেকে ১০ সেকেন্ডের র্যান্ডম ডিলে নিয়ে নেক্সট লিংক ওপেন
                int delay = random.nextInt(5000) + 5000;
                handler.postDelayed(this::openNextLink, delay);
            }
        }
    }

    private void simulateHumanTyping(AccessibilityNodeInfo replyBox, String fullText) {
        broadcastStatus("Typing naturally...");
        new Thread(() -> {
            StringBuilder currentText = new StringBuilder();
            for (char c : fullText.toCharArray()) {
                currentText.append(c);
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, currentText.toString());
                replyBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                
                try {
                    // র্যান্ডম ডিলে (৫০-১০০ মিলি-সেকেন্ড)
                    Thread.sleep(random.nextInt(50) + 50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            broadcastStatus("Typing finished. Waiting for manual Send.");
            // **[CRITICAL RULE]:** কোনো অটো-সেন্ড হবে না। ইউজারের জন্য অপেক্ষা করবে।
        }).start();
    }

    private void openNextLink() {
        try {
            String jsonStr = prefs.getString("linksArray", "[]");
            JSONArray array = new JSONArray(jsonStr);
            int currentIndex = prefs.getInt("currentIndex", 0);

            if (currentIndex < array.length()) {
                String link = array.getString(currentIndex);
                
                // ইনডেক্স আপডেট
                prefs.edit().putInt("currentIndex", currentIndex + 1).apply();
                broadcastStatus("Opening link " + (currentIndex + 1));

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                prefs.edit().putBoolean("isRunning", false).apply();
                broadcastStatus("All tasks completed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper functions for Node Traversal
    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if ("android.widget.EditText".equals(node.getClassName())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findEditText(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private String findLongestText(AccessibilityNodeInfo node) {
        String longest = "";
        if (node == null) return longest;
        if (node.getText() != null) {
            longest = node.getText().toString();
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            String childText = findLongestText(node.getChild(i));
            if (childText.length() > longest.length()) {
                longest = childText;
            }
        }
        return longest;
    }

    private void broadcastStatus(String status) {
        Intent intent = new Intent("com.example.twitterassistant.UPDATE_UI");
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {}
}
