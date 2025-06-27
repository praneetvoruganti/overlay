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
import android.content.Intent;
import android.app.PendingIntent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.awesomeproject.MainActivity;
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

// Manages system overlays (bubble, card) and communicates with JS.
public class OverlayCoreModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private static final String TAG = "OverlayCoreModule";
    private final ReactApplicationContext mReactContext;
    private WindowManager mWindowManager;
    private View mOverlayView;
    private View mDismissView; // view for the 'X' dismiss button
    private WindowManager.LayoutParams mOverlayParams;
    private WindowManager.LayoutParams mDismissParams;
    private boolean mIsBubbleOverlappingDismiss = false; // for drag-to-dismiss

    // --- State ---
    private String mCurrentOverlayType = null; // "bubble", "card", or null
    private int mBubbleLastX = 0; // remember bubble position
    private int mBubbleLastY = 100;

    public OverlayCoreModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        mReactContext.addLifecycleEventListener(this); // handle app resume/pause
    }

    @NonNull
    @Override
    public String getName() {
        return "OverlayCoreModule";
    }

    // Show overlay from JS
    @ReactMethod
    public void showOverlay(ReadableMap data, Promise promise) {
        UiThreadUtil.runOnUiThread(() -> {
            if (mOverlayView != null) {
                promise.reject("E_OVERLAY_EXISTS", "Overlay already visible.");
                return;
            }

            // check permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(mReactContext)) {
                promise.reject("E_PERMISSION_DENIED", "Overlay permission denied.");
                return;
            }

            mWindowManager = (WindowManager) mReactContext.getSystemService(Context.WINDOW_SERVICE);
            LayoutInflater inflater = (LayoutInflater) mReactContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String type = data.getString("type");

            if (type == null) {
                promise.reject("E_INVALID_TYPE", "Overlay type missing.");
                return;
            }

            mCurrentOverlayType = type;

            // inflate correct layout
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

    // Hide overlay from JS
    @ReactMethod
    public void hideOverlay(Promise promise) {
        UiThreadUtil.runOnUiThread(() -> {
            try {
                hideOverlayInternal();
                if (promise != null) {
                    promise.resolve(null);
                }
            } catch (Exception e) {
                if (promise != null) {
                    promise.reject("E_REMOVE_VIEW_FAILED", e.getMessage());
                }
            }
        });
    }

    // remove view and reset state
    private void hideOverlayInternal() {
        // remove main overlay
        if (mOverlayView != null && mWindowManager != null) {
            try {
                mWindowManager.removeView(mOverlayView);
            } catch (Exception e) { /* ignore */ }
            mOverlayView = null;
            mCurrentOverlayType = null;
            mOverlayParams = null;
        }
        // also remove dismiss view if it exists
        hideDismissView();
    }

    // Update overlay content from JS
    @ReactMethod
    public void updateOverlay(ReadableMap data, Promise promise) {
        UiThreadUtil.runOnUiThread(() -> {
            if (mOverlayView == null || mCurrentOverlayType == null) {
                promise.reject("E_NO_OVERLAY", "No overlay to update.");
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

    // config bubble view, params, and listeners
    private void setupBubbleView(ReadableMap data) {
        updateBubbleView(data); // set initial content

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE; // fallback for old android

        mOverlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // allow touch passthrough
                PixelFormat.TRANSLUCENT);

        mOverlayParams.gravity = Gravity.TOP | Gravity.START; // top-left corner
        mOverlayParams.x = mBubbleLastX; // restore position
        mOverlayParams.y = mBubbleLastY;

        // handle drag, click, and dismiss
        mOverlayView.setOnTouchListener(new View.OnTouchListener() {
            private long startClickTime;
            private float initialX, initialY, initialTouchX, initialTouchY;
            private final int screenHeight = mReactContext.getResources().getDisplayMetrics().heightPixels;
            private final int dismissZoneHeight = 300; // px from bottom for dismiss zone

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        initialX = mOverlayParams.x;
                        initialY = mOverlayParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        showDismissView(); // show 'X' on drag start
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // update position on drag
                        mOverlayParams.x = (int) (initialX + (event.getRawX() - initialTouchX));
                        mOverlayParams.y = (int) (initialY + (event.getRawY() - initialTouchY));
                        mWindowManager.updateViewLayout(mOverlayView, mOverlayParams);

                        // check if over dismiss zone and give visual feedback
                        mIsBubbleOverlappingDismiss = event.getRawY() > screenHeight - dismissZoneHeight;
                        if (mDismissView != null) {
                            mDismissView.setAlpha(mIsBubbleOverlappingDismiss ? 1.0f : 0.5f);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        hideDismissView(); // always hide 'X' on drag end

                        if (mIsBubbleOverlappingDismiss) {
                            hideOverlayInternal(); // bye bye bubble
                            return true;
                        }

                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        float clickDistance = (float) Math.hypot(event.getRawX() - initialTouchX, event.getRawY() - initialTouchY);

                        // short, stationary touch = click
                        if (clickDuration < 200 && clickDistance < 15) {
                            sendEvent("onBubbleClicked", null);
                        } else {
                            // otherwise, it's a drag, so snap to edge
                            int screenWidth = mReactContext.getResources().getDisplayMetrics().widthPixels;
                            if (mOverlayParams.x < (screenWidth - v.getWidth()) / 2) {
                                mOverlayParams.x = 0; // snap left
                            } else {
                                mOverlayParams.x = screenWidth - v.getWidth(); // snap right
                            }
                            mWindowManager.updateViewLayout(mOverlayView, mOverlayParams);
                            // save position
                            mBubbleLastX = mOverlayParams.x;
                            mBubbleLastY = mOverlayParams.y;
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // update bubble badge count
        // show the 'X' dismiss button at the bottom
    private void showDismissView() {
        if (mDismissView != null) return;

        UiThreadUtil.runOnUiThread(() -> {
            LayoutInflater inflater = (LayoutInflater) mReactContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mDismissView = inflater.inflate(R.layout.overlay_dismiss_button, null);
            mDismissView.setAlpha(0.5f); // start semi-transparent

            int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            mDismissParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutFlag,
                    // not focusable or touchable, it's just a target
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);

            mDismissParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            mDismissParams.y = 50;

            mWindowManager.addView(mDismissView, mDismissParams);
        });
    }

    // remove the 'X' dismiss button
    private void hideDismissView() {
        UiThreadUtil.runOnUiThread(() -> {
            if (mDismissView != null && mWindowManager != null) {
                try {
                    mWindowManager.removeView(mDismissView);
                } catch (Exception e) {
                    Log.w(TAG, "Could not remove dismiss view, maybe it was already gone.");
                }
                mDismissView = null;
            }
        });
    }

    private void updateBubbleView(ReadableMap data) {
        if (data == null || mOverlayView == null) return;
        TextView badge = mOverlayView.findViewById(R.id.bubble_badge);
        if (data.hasKey("badgeCount")) {
            int count = data.getInt("badgeCount");
            badge.setText(String.valueOf(count));
            badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }

    // --- Card State ---
    private double baseFare = 0;
    private double currentFare = 0;

    // config card view, params, and listeners
    private void setupCardView(ReadableMap data) {
        updateCardView(data); // set initial text fields

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        mOverlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // allow touch passthrough
                PixelFormat.TRANSLUCENT);
        mOverlayParams.gravity = Gravity.CENTER;

        // find views
        final Button acceptButton = mOverlayView.findViewById(R.id.accept_button);
        final Button ignoreButton = mOverlayView.findViewById(R.id.ignore_button);
        final TextView totalFareTextView = mOverlayView.findViewById(R.id.total_fare_text);
        final TextView increment1 = mOverlayView.findViewById(R.id.increment_1);
        final TextView increment2 = mOverlayView.findViewById(R.id.increment_2);
        final TextView increment3 = mOverlayView.findViewById(R.id.increment_3);

        // init fare state
        try {
            if (data.hasKey("totalFare")) {
                String fareString = data.getString("totalFare").replaceAll("[^\\d.]", "");
                baseFare = Double.parseDouble(fareString);
            } else {
                baseFare = 0;
            }
            currentFare = baseFare;
            updateFareDisplay(totalFareTextView, acceptButton);
        } catch (Exception e) {
            Log.e(TAG, "Could not parse base fare from: " + (data.hasKey("totalFare") ? data.getString("totalFare") : "null"), e);
            baseFare = 0;
            currentFare = 0;
            updateFareDisplay(totalFareTextView, acceptButton);
        }

        // listener for fare increments
        View.OnClickListener incrementListener = v -> {
            String text = ((TextView) v).getText().toString();
            try {
                double increment = Double.parseDouble(text.replaceAll("[^\\d.]", ""));
                currentFare = baseFare + increment; // always add to base
                updateFareDisplay(totalFareTextView, acceptButton);
            } catch (NumberFormatException ex) {
                Log.e(TAG, "Could not parse increment from: " + text, ex);
            }
        };

        increment1.setOnClickListener(incrementListener);
        increment2.setOnClickListener(incrementListener);
        increment3.setOnClickListener(incrementListener);

        // accept button listener
        acceptButton.setOnClickListener(v -> {
            WritableMap params = Arguments.createMap();
            params.putDouble("finalFare", currentFare);
            sendEvent("onTripAccepted", params);
            hideOverlayInternal();

            // bring app to foreground
            Context context = getReactApplicationContext();
            String packageName = context.getPackageName();
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(launchIntent);
            }
        });

        // ignore button listener
        ignoreButton.setOnClickListener(v -> {
            sendEvent("onTripIgnored", null);
            hideOverlayInternal();
        });
    }

    // update fare text on card and button
    private void updateFareDisplay(TextView totalFareView, Button acceptButton) {
        String fareText = String.format(java.util.Locale.US, "₹%.0f", currentFare);
        totalFareView.setText(fareText);
        acceptButton.setText(String.format("Accept for %s", fareText));
    }

    /**
     * Updates the non-interactive text fields on the card.
     * @param cardData The new data from JavaScript.
     */
    private void updateCardView(ReadableMap cardData) {
        if (mOverlayView == null || cardData == null) return;

        // Find the TextViews by their ID.
        TextView destinationText = mOverlayView.findViewById(R.id.destination_text);
        TextView distanceText = mOverlayView.findViewById(R.id.distance_text);
        TextView etaText = mOverlayView.findViewById(R.id.eta_text);

        // Update their text if the data exists in the map.
        if (cardData.hasKey("destination")) destinationText.setText(cardData.getString("destination"));
        if (cardData.hasKey("distance")) distanceText.setText(cardData.getString("distance"));
        if (cardData.hasKey("eta")) etaText.setText(cardData.getString("eta"));

        // Set the text for the increment buttons. This could also come from JS data.
        TextView increment1 = mOverlayView.findViewById(R.id.increment_1);
        TextView increment2 = mOverlayView.findViewById(R.id.increment_2);
        TextView increment3 = mOverlayView.findViewById(R.id.increment_3);
        increment1.setText("+ ₹5");
        increment2.setText("+ ₹10");
        increment3.setText("+ ₹20");
    }

    // --- LifecycleEventListener Methods ---

    @Override
    public void onHostResume() {
        // This is called when the app comes to the foreground. Not used here.
    }

    @Override
    public void onHostPause() {
        // This is called when the app goes to the background. Not used here.
    }

    @Override
    public void onHostDestroy() {
        // This is called when the main React Native activity is destroyed.
        // It's crucial to clean up our overlay to prevent a 'window leak'.
        if (mOverlayView != null) {
            mWindowManager.removeView(mOverlayView);
            mOverlayView = null;
        }
    }

    /**
     * A helper method to send an event from native code to JavaScript.
     * @param eventName The name of the event (e.g., "onTripAccepted").
     * @param params Optional data to send with the event.
     */
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        mReactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }
}
