package com.awesomeproject.core;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.awesomeproject.R;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Calendar;

public class OverlayCoreModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private static final String TAG = "OverlayCoreModule";
    private final ReactApplicationContext mReactContext;
    private WindowManager mWindowManager;
    private View mOverlayView;
    private WindowManager.LayoutParams mOverlayParams;

    private String mCurrentOverlayType = null;
    private int mBubbleLastX = 0;
    private int mBubbleLastY = 100;

    public OverlayCoreModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        mReactContext.addLifecycleEventListener(this);
    }

    @NonNull
    @Override
    public String getName() {
        return "OverlayCoreModule";
    }

    @ReactMethod
    public void showOverlay(ReadableMap data, Promise promise) {
        UiThreadUtil.runOnUiThread(() -> {
            if (mOverlayView != null) {
                Log.w(TAG, "An overlay is already visible. Hide it before showing a new one.");
                promise.reject("E_OVERLAY_EXISTS", "An overlay is already visible.");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(mReactContext)) {
                promise.reject("E_PERMISSION_DENIED", "Overlay permission is not granted.");
                return;
            }

            mWindowManager = (WindowManager) mReactContext.getSystemService(Context.WINDOW_SERVICE);
            LayoutInflater inflater = (LayoutInflater) mReactContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String type = data.getString("type");

            if (type == null) {
                promise.reject("E_INVALID_TYPE", "Overlay type must be specified.");
                return;
            }

            mCurrentOverlayType = type;

            switch (type) {
                case "bubble":
                    mOverlayView = inflater.inflate(R.layout.overlay_bubble, null);
                    setupBubbleView(data);
                    break;
                case "card":
                    mOverlayView = inflater.inflate(R.layout.overlay_trip_card, null);
                    setupCardView(data);
                    break;
                default:
                    promise.reject("E_INVALID_TYPE", "Invalid overlay type: " + type);
                    return;
            }

            try {
                mWindowManager.addView(mOverlayView, mOverlayParams);
                promise.resolve(null);
            } catch (Exception e) {
                Log.e(TAG, "Failed to add overlay view.", e);
                promise.reject("E_ADD_VIEW_FAILED", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void hideOverlay(Promise promise) {
        UiThreadUtil.runOnUiThread(() -> {
            if (mOverlayView != null && mWindowManager != null) {
                try {
                    mWindowManager.removeView(mOverlayView);
                    mOverlayView = null;
                    mCurrentOverlayType = null;
                    mOverlayParams = null;
                    promise.resolve(null);
                } catch (Exception e) {
                    promise.reject("E_REMOVE_VIEW_FAILED", e.getMessage());
                }
            } else {
                promise.resolve(null); // No-op if not visible
            }
        });
    }

    @ReactMethod
    public void updateOverlay(ReadableMap data, Promise promise) {
        UiThreadUtil.runOnUiThread(() -> {
            if (mOverlayView == null || mCurrentOverlayType == null) {
                promise.reject("E_NO_OVERLAY", "No overlay is visible to update.");
                return;
            }

            switch (mCurrentOverlayType) {
                case "bubble":
                    updateBubbleView(data);
                    break;
                case "card":
                    updateCardView(data);
                    break;
            }
            promise.resolve(null);
        });
    }

    private void setupBubbleView(ReadableMap data) {
        updateBubbleView(data);
        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        mOverlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mOverlayParams.gravity = Gravity.TOP | Gravity.START;
        mOverlayParams.x = mBubbleLastX;
        mOverlayParams.y = mBubbleLastY;

        mOverlayView.setOnTouchListener(new View.OnTouchListener() {
            private long startClickTime;
            private float initialX, initialY, initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        initialX = mOverlayParams.x;
                        initialY = mOverlayParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        mOverlayParams.x = (int) (initialX + (event.getRawX() - initialTouchX));
                        mOverlayParams.y = (int) (initialY + (event.getRawY() - initialTouchY));
                        mWindowManager.updateViewLayout(mOverlayView, mOverlayParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        float clickDistance = (float) Math.hypot(event.getRawX() - initialTouchX, event.getRawY() - initialTouchY);

                        if (clickDuration < 200 && clickDistance < 15) {
                            sendEvent("onBubbleClicked", null);
                        } else {
                            // Snap to edge and save position
                            int screenWidth = mReactContext.getResources().getDisplayMetrics().widthPixels;
                            if (mOverlayParams.x < (screenWidth - v.getWidth()) / 2) {
                                mOverlayParams.x = 0;
                            } else {
                                mOverlayParams.x = screenWidth - v.getWidth();
                            }
                            mWindowManager.updateViewLayout(mOverlayView, mOverlayParams);
                            mBubbleLastX = mOverlayParams.x;
                            mBubbleLastY = mOverlayParams.y;
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void updateBubbleView(ReadableMap data) {
        if (data == null) return;
        TextView badge = mOverlayView.findViewById(R.id.bubble_badge);
        if (data.hasKey("badgeCount")) {
            int count = data.getInt("badgeCount");
            badge.setText(String.valueOf(count));
            badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void setupCardView(ReadableMap data) {
        updateCardView(data);
        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        mOverlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        mOverlayParams.gravity = Gravity.TOP;

        Button acceptButton = mOverlayView.findViewById(R.id.accept_button);
        Button declineButton = mOverlayView.findViewById(R.id.decline_button);

        acceptButton.setOnClickListener(v -> sendEvent("onTripAccepted", null));
        declineButton.setOnClickListener(v -> sendEvent("onTripDeclined", null));
    }

    private void updateCardView(ReadableMap data) {
        if (data == null) return;
        TextView fareText = mOverlayView.findViewById(R.id.fare_text);
        TextView pickupText = mOverlayView.findViewById(R.id.pickup_text);
        TextView dropoffText = mOverlayView.findViewById(R.id.dropoff_text);

        if (data.hasKey("fare")) fareText.setText("Fare: " + data.getString("fare"));
        if (data.hasKey("pickup")) pickupText.setText("Pickup: " + data.getString("pickup"));
        if (data.hasKey("dropoff")) dropoffText.setText("Dropoff: " + data.getString("dropoff"));
    }

    @Override
    public void onHostResume() {}

    @Override
    public void onHostPause() {}

    @Override
    public void onHostDestroy() {
        // Clean up the view when the host activity is destroyed
        if (mOverlayView != null) {
            mWindowManager.removeView(mOverlayView);
            mOverlayView = null;
        }
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        mReactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }
}
