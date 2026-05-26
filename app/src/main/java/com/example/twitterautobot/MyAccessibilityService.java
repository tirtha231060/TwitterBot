package com.example.twitterautobot;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MyAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // আপনার বট লজিক এখানে কাজ করবে
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
    }

    @Override
    public void onInterrupt() {
    }
}
