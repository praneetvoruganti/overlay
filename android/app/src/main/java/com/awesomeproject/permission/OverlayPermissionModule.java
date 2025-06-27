package com.awesomeproject.permission;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class OverlayPermissionModule extends ReactContextBaseJavaModule {

    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1234;
    private Promise mPromise;
    private final ReactApplicationContext mReactContext;

    public OverlayPermissionModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        // listen for settings screen result
        mReactContext.addActivityEventListener(mActivityEventListener);
    }

    @NonNull
    @Override
    public String getName() {
        return "OverlayPermissionModule";
    }

    // check if we can draw overlays
    @ReactMethod
    public void checkOverlayPermission(Promise promise) {
        // Android M+ needs explicit permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            promise.resolve(Settings.canDrawOverlays(mReactContext));
        } else {
            // older Android, always okay
            promise.resolve(true);
        }
    }

    // ask user for overlay permission
    @ReactMethod
    public void requestOverlayPermission(Promise promise) {
        mPromise = promise; // hold promise for later

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // check if permission already given
            if (Settings.canDrawOverlays(mReactContext)) {
                mPromise.resolve(null);
                mPromise = null; // clean up promise
                return;
            }

            Activity currentActivity = getCurrentActivity();
            if (currentActivity == null) {
                mPromise.reject("E_ACTIVITY_DOES_NOT_EXIST", "Activity doesn't exist");
                mPromise = null;
                return;
            }

            // create intent to open settings screen
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + mReactContext.getPackageName()));
            currentActivity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        } else {
            // older Android, always okay
            mPromise.resolve(null);
            mPromise = null;
        }
    }

    // listens for user returning from settings
    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            // is this our request?
            if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
                if (mPromise != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // check again after user returns
                        if (Settings.canDrawOverlays(mReactContext)) {
                            mPromise.resolve(null); // permission granted
                        } else {
                            mPromise.reject("E_PERMISSION_DENIED", "User denied overlay permission"); // permission denied
                        }
                    } else {
                         mPromise.resolve(null); // older Android, always okay
                    }
                    mPromise = null; // clean up promise
                }
            }
        }
    };
}
