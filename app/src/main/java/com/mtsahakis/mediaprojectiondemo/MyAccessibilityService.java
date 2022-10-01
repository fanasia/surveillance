package com.mtsahakis.mediaprojectiondemo;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Patterns;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.text.TextUtils.isEmpty;

import org.apache.commons.codec.binary.StringUtils;

public class MyAccessibilityService extends AccessibilityService {
    private String allUrls = "";
    List<String> allUrlsList = new ArrayList<>();
    private String allContents = "";
    List<String> allContentsList = new ArrayList<>();
    private int mDebugDepth = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        PackageManager pm = getApplicationContext().getPackageManager();

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                // get app name
                String applicationName = getApplicationName(event, pm);
                ((OSLog) this.getApplication()).setAppname(applicationName);

                // get all content & urls
                mDebugDepth = 0;
                allContents = "";
                allContentsList = new ArrayList<String>();
                allUrls = "";
                allUrlsList = new ArrayList<String>();

                AccessibilityNodeInfo mNodeInfo = event.getSource();
                getAllContent(mNodeInfo);

                listToStringContent(allContentsList);
                listToStringUrls(allUrlsList);

//                Log.i(TAG, "final allContent:" + allContents);
                ((OSLog) this.getApplication()).setContent(allContents);
//                Log.i(TAG, "final allUrls:" + allUrls);
                ((OSLog) this.getApplication()).setUrl(allUrls);
            }
        }

    }

    public String getApplicationName(AccessibilityEvent event, PackageManager pm) {
        String applicationName = event.getPackageName().toString();
//        Log.e("PackageName App", applicationName);

        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(applicationName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            ai = null;
        }

        if (ai != null) {
            applicationName = (String) pm.getApplicationLabel(ai);
//            Log.e("ApplicationName App", applicationName);
        }

        return applicationName;
    }

    public void captureUrl(AccessibilityEvent event, AccessibilityNodeInfo mNodeInfo) {
        String chrome = "com.android.chrome";
        String chromeAddressBarId = "com.android.chrome:id/url_bar";

        if (chrome.contentEquals(event.getPackageName())) {
            List<AccessibilityNodeInfo> nodes = mNodeInfo.findAccessibilityNodeInfosByViewId(chromeAddressBarId);
            if (nodes == null || nodes.size() <= 0) {
                return;
            }

            AccessibilityNodeInfo addressBarNodeInfo = nodes.get(0);
            String url = null;
            if (addressBarNodeInfo.getText() != null) {
                url = addressBarNodeInfo.getText().toString();

            }
            addressBarNodeInfo.recycle();
            allUrlsList.add(url);
        }
    }

    public void getAllContent(AccessibilityNodeInfo mNodeInfo) {
        String log = "";

        if (mNodeInfo == null) {
            return;
        }

//        for (int i = 0; i < mDebugDepth; i++) {
//            log += ".";
//        }

        if (mNodeInfo.getText() != null && mNodeInfo.getText() != "") {
            String text = mNodeInfo.getText().toString();
            if (isUrl(text)) {
                allUrlsList.add(text);
            }
            log += text ;
            allContentsList.add(text);
//            Log.e(TAG, "mDebugDepth: " + mDebugDepth + ", Content: " +  log + "viewIdResourceName: " + mNodeInfo.getViewIdResourceName());
        }

        if (mNodeInfo.getChildCount() < 1) {
            return;
        }

        mDebugDepth++;

        for (int i = 0; i < mNodeInfo.getChildCount(); i++) {
            getAllContent(mNodeInfo.getChild(i));
        }

        mDebugDepth--;
    }

    private void listToStringContent(List<String> allContentsList) {
        for (String content: allContentsList) {
            allContents += content + "\n";
        }
    }

    private void listToStringUrls(List<String> allUrlsList) {
        for (String url: allUrlsList) {
            allUrls += url + ";";
        }
    }

    private boolean isUrl(String capturedText) {
        return Patterns.WEB_URL.matcher(capturedText).matches();
    }

    /**
     * Method to loop through all the views and try to find a URL.
     * @param info
     */
    public void getUrlsFromViews(AccessibilityNodeInfo info) {

        try {
            if (info == null)
                return;

            if (info.getText() != null && info.getText().length() > 0) {
                String capturedText = info.getText().toString();
                if (capturedText.contains("https://")
                        || capturedText.contains("http://")
                        || capturedText.contains("www.")
                        || capturedText.contains(".com")
                        || capturedText.contains(".fi")
                        || capturedText.contains(".net") ) {
                    allUrls = allUrls + " <newline> " + capturedText;
                }
            }
            for (int i = 0; i < info.getChildCount(); i++) {
                AccessibilityNodeInfo child = info.getChild(i);
                getUrlsFromViews(child);
                if(child != null){
                    child.recycle();
                }
            }
        } catch(StackOverflowError ex){
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void getContentFromViews(AccessibilityNodeInfo info) {

        try {
            if (info == null)
                return;

            if (info.getText() != null && info.getText().length() > 0) {
                String capturedText = info.getText().toString();
                    allContents = allContents + " <newline> " + capturedText;
            }
            for (int i = 0; i < info.getChildCount(); i++) {
                AccessibilityNodeInfo child = info.getChild(i);
                getContentFromViews(child);
                if(child != null){
                    child.recycle();
                }
            }
        } catch(StackOverflowError ex){
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {

    }
}
