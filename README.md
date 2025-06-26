# Android Overlay Module for React Native

## 1. Introduction and Use Cases

This module provides a robust and modular system for displaying system-level overlays on Android within a React Native application. It is designed to be persistent, interactive, and lifecycle-aware, making it ideal for applications that need to display information to the user even when the app is in the background or the device is locked.

**Primary Use Cases:**

*   **Driver App Bubble:** A persistent, draggable bubble that indicates an active duty status or new ride requests. The bubble can display a badge count for new notifications.
*   **Incoming Trip Card:** When the bubble is tapped or a new high-priority event occurs, a full-width trip card overlay is displayed with details like fare, pickup, and drop-off locations, along with "Accept" and "Decline" buttons.

## 2. Installation

These instructions assume a bare React Native CLI project. The module consists of native Android code and a TypeScript bridge.

**Step 1: Copy Module Files**

*   Copy the entire `android/app/src/main/java/com/awesomeproject/core` directory into your project's corresponding folder.
*   Copy the `android/app/src/main/java/com/awesomeproject/permission` directory.
*   Copy the layout files (`overlay_bubble.xml`, `overlay_trip_card.xml`) into `android/app/src/main/res/layout`.
*   Copy the `modules` directory (containing `OverlayCore.ts`, `OverlayPermission.ts`, and `OverlayService.ts`) to your project root.

**Step 2: Register Native Packages**

In your `MainApplication.java` or `MainApplication.kt`, add the `OverlayCorePackage` and `OverlayPermissionPackage` to the list of packages.

```java
// MainApplication.java
import com.awesomeproject.core.OverlayCorePackage;
import com.awesomeproject.permission.OverlayPermissionPackage;

@Override
protected List<ReactPackage> getPackages() {
  @SuppressWarnings("UnnecessaryLocalVariable")
  List<ReactPackage> packages = new PackageList(this).getPackages();
  // ...
  packages.add(new OverlayCorePackage());
  packages.add(new OverlayPermissionPackage());
  return packages;
}
```

**Step 3: Add Android Permission**

Add the `SYSTEM_ALERT_WINDOW` permission to your `AndroidManifest.xml`:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <application ...>
      ...
    </application>
</manifest>
```

## 3. API Reference

All interaction with the overlay system should go through the `OverlayService`.

### `OverlayService.ts`

*   `showBubble(badgeCount: number, tripData: object): Promise<void>`
    *   Shows the draggable bubble overlay. `badgeCount` updates the badge visibility and text. `tripData` is stored and used if the bubble is clicked to show the trip card.

*   `showTripCard(data: object): Promise<void>`
    *   Shows the trip card overlay with the provided data (`fare`, `pickup`, `dropoff`).

*   `hideOverlay(): Promise<void>`
    *   Hides any currently visible overlay (bubble or card).

*   `updateBubble(badgeCount: number): Promise<void>`
    *   Updates the badge count on an already visible bubble without hiding and re-showing it.

### Native Events

Listen for these events from the `OverlayCoreModule`:

*   `onBubbleClicked`: Emitted when the bubble is tapped.
*   `onTripAccepted`: Emitted when the "Accept" button on the trip card is clicked.
*   `onTripDeclined`: Emitted when the "Decline" button is clicked.

## 4. Permission Flow

The `SYSTEM_ALERT_WINDOW` permission is required for displaying overlays. The `OverlayService` handles this automatically, but it's important to understand the flow.

1.  When `showOverlay` is called, the service first checks for the permission using `OverlayPermission.checkOverlayPermission()`.
2.  If permission is not granted, it calls `OverlayPermission.requestOverlayPermission()`, which opens the system settings screen for the user to manually grant the permission.

**Screenshots (Illustrative):**

*   *(Imagine a screenshot of the system permission screen for drawing over other apps)*

**Common Pitfalls:**

*   **User Denies Permission:** If the user denies the permission, subsequent calls to show an overlay will fail until the permission is granted manually in the app's settings.
*   **MIUI/ColorOS/etc.:** Some Android manufacturers have additional security layers that may block overlays. Users on these devices might need to grant a second permission from the device's security app.

## 5. XML Layout Reference

You can customize the appearance of the overlays by editing the XML layout files in `android/app/src/main/res/layout`.

*   `overlay_bubble.xml`: Defines the circular bubble, its icon, and the badge `TextView`.
*   `overlay_trip_card.xml`: Defines the trip card, including `TextViews` for fare/pickup/dropoff and the "Accept"/"Decline" buttons.

## 6. Troubleshooting

*   **Permission Denied:** Ensure the permission is in `AndroidManifest.xml` and that the user has granted it. Guide the user to the system settings if they deny it.
*   **View Not Showing:**
    *   Check `logcat` for errors from `OverlayCoreModule`. Common errors include `E_ADD_VIEW_FAILED`.
    *   Ensure the app has the overlay permission.
    *   Verify you are calling the `OverlayService` methods on the UI thread (the service handles this, but be mindful if you modify it).
*   **Memory Leaks:** The module is designed to clean up views in `onHostDestroy`. Ensure you are not holding strong references to the overlay views elsewhere in your code.

## 7. Next Steps

With the module integrated, the next step is to connect it to your application's state management logic (e.g., Redux, MobX).

*   Call `OverlayService.showBubble()` when your driver goes on duty.
*   Listen for new ride request events from your backend and use `OverlayService.updateBubble()` to show a badge.
*   If a high-priority request comes in, call `OverlayService.showTripCard()` directly.
*   When the user accepts or declines a trip via the overlay, update your app's state accordingly.

For more information, please visit [CocoaPods Getting Started guide](https://guides.cocoapods.org/using/getting-started.html).

```sh
# Using npm
npm run ios

# OR using Yarn
yarn ios
```

If everything is set up correctly, you should see your new app running in the Android Emulator, iOS Simulator, or your connected device.

This is one way to run your app — you can also build it directly from Android Studio or Xcode.

## Step 3: Modify your app

Now that you have successfully run the app, let's make changes!

Open `App.tsx` in your text editor of choice and make some changes. When you save, your app will automatically update and reflect these changes — this is powered by [Fast Refresh](https://reactnative.dev/docs/fast-refresh).

When you want to forcefully reload, for example to reset the state of your app, you can perform a full reload:

- **Android**: Press the <kbd>R</kbd> key twice or select **"Reload"** from the **Dev Menu**, accessed via <kbd>Ctrl</kbd> + <kbd>M</kbd> (Windows/Linux) or <kbd>Cmd ⌘</kbd> + <kbd>M</kbd> (macOS).
- **iOS**: Press <kbd>R</kbd> in iOS Simulator.

## Congratulations! :tada:

You've successfully run and modified your React Native App. :partying_face:

### Now what?

- If you want to add this new React Native code to an existing application, check out the [Integration guide](https://reactnative.dev/docs/integration-with-existing-apps).
- If you're curious to learn more about React Native, check out the [docs](https://reactnative.dev/docs/getting-started).

# Troubleshooting

If you're having issues getting the above steps to work, see the [Troubleshooting](https://reactnative.dev/docs/troubleshooting) page.

# Learn More

To learn more about React Native, take a look at the following resources:

- [React Native Website](https://reactnative.dev) - learn more about React Native.
- [Getting Started](https://reactnative.dev/docs/environment-setup) - an **overview** of React Native and how setup your environment.
- [Learn the Basics](https://reactnative.dev/docs/getting-started) - a **guided tour** of the React Native **basics**.
- [Blog](https://reactnative.dev/blog) - read the latest official React Native **Blog** posts.
- [`@facebook/react-native`](https://github.com/facebook/react-native) - the Open Source; GitHub **repository** for React Native.
