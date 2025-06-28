# Android System Overlay Module

Lets you draw on top of other apps. Good for things like ride-sharing alerts.

### Features

- Shows a small, draggable bubble that snaps to the screen edges.
- Can also show a big trip card with details and action buttons.
- **Auto-shows the bubble** when the app goes into the background.
- **Drag-to-dismiss**: Drag the bubble to a target at the bottom of the screen to close it.
- Stays on screen even when your app is in the background.

---

### How It Works

It's a bridge between your JavaScript code and the native Android code.

1.  **React Native (JS)**: You call a simple function, like `OverlayService.showBubble()`. Or, you just send the app to the background.
2.  **The Bridge**: React Native sends this command over to the native side.
3.  **Native Android (Java)**: The Java code gets the command. It handles the tricky Android stuff:
    *   Asks for `SYSTEM_ALERT_WINDOW` permission (to draw over other apps).
    *   Creates the bubble or card view from an XML layout.
    *   Adds that view directly to the phone's window manager.
4.  **Events**: When the user taps a button on the overlay (e.g., "Accept"), the native code sends an event back to your JavaScript.

### The Flow (Diagram)

```
 [ Your RN App ]                               [ Native Android ]
       |                                              |
 (OverlayService.ts)                          (OverlayCoreModule.java)
       |                                              |
  // User action or AppState change            // Listens for commands
       |                                              |
       v                                              v
 [ RN Bridge ] --- sends "showBubble" command --> [ Java Bridge ]
       |                                              |
       +----------------------------------------------+
                                                      |
                                             // 1. Checks permission
                                             // 2. Creates Android View
                                             // 3. Adds View to screen
                                                      |
                                                      v
                                             [ Overlay on Screen ]
                                                      |
                                             // User taps "Accept" or drags
                                                      |
                                                      v
 [ RN Bridge ] <-- sends "onTripAccepted" event -- [ Java Bridge ]
       |
       v
 [ Your RN App ] // Listens for the event and reacts

```

---

### Prerequisites

- A React Native project.
- Android platform setup.

### Setup

1.  **Copy Files**:
    *   Copy `android/app/src/main/java/com/awesomeproject/core` to your project's equivalent Java source directory.
    *   Copy `android/app/src/main/java/com/awesomeproject/permission` to the same place.
    *   Copy the `modules` folder into your JS source root (e.g., `src/`).
    *   Copy the layout files from `android/app/src/main/res/layout` (`overlay_bubble.xml`, `overlay_trip_card.xml`, `overlay_dismiss_button.xml`) to your project's layout folder.
    *   Copy the drawable file from `android/app/src/main/res/drawable` (`dismiss_background.xml`) to your project's drawable folder.

2.  **Register Packages**:
    *   In `MainApplication.kt` (or `.java` if you're not on Kotlin), add `OverlayCorePackage()` and `OverlayPermissionPackage()` to the packages list inside `getPackages()`.

    ```kotlin
    // MainApplication.kt
    override fun getPackages(): List<ReactPackage> =
        PackageList(this).packages.apply {
            // Packages that cannot be autolinked yet can be added manually here
            add(OverlayCorePackage()) // <-- Add this
            add(OverlayPermissionPackage()) // <-- And this
        }
    ```

3.  **Add Permission & String**:
    *   In `AndroidManifest.xml`, add `<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />`.
    *   In `res/values/strings.xml`, add `<string name="dismiss_overlay">Dismiss Overlay</string>`.

---

### How to Use

It's all handled by the `OverlayService` and the `OverlayDemoScreen`.

#### Automatic Behavior

-   **Backgrounding**: When the app moves to the background, the bubble will automatically appear. This is handled by an `AppState` listener in `OverlayDemoScreen.tsx`.
-   **Foregrounding**: When the app comes back to the front, the overlay is automatically hidden.
-   **Dismissing**: To close the bubble, just drag it. A dismiss area (a red 'X') will appear at the bottom. Drop the bubble on it to close the overlay.

#### Manual Control

You can still control it yourself if you need to.

```javascript
import OverlayService from './modules/OverlayService';

// Ask for permission first (or check if you have it).
// It's best to do this when the app starts.
OverlayService.checkPermission().then(hasPermission => {
  if (!hasPermission) {
    OverlayService.requestPermission();
  }
});

// Show the bubble manually.
OverlayService.showBubble({ badgeCount: 1 });

// Show the trip card directly.
OverlayService.showTripCard({
  destination: '123 Main St',
  totalFare: '₹450',
  distance: '5.2 km',
  eta: '12 mins'
});

// Hide whatever is showing.
OverlayService.hide();

// Listen for clicks.
OverlayService.addEventListener('onTripAccepted', (event) => {
  console.log('Trip accepted!', event.finalFare);
});

OverlayService.addEventListener('onBubbleClicked', () => {
  console.log('Bubble was clicked!');
  // The service handles showing the card automatically.
});
```

