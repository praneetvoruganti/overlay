# Android System Overlay Module

Lets you draw on top of other apps. Good for things like ride-sharing alerts.

- Shows a small, draggable bubble.
- Can also show a big trip card with details.
- Stays on screen even when your app is in the background.

---

### How It Works

It's a bridge between your JavaScript code and the native Android code.

1.  **React Native (JS)**: You call a simple function, like `OverlayService.showBubble()`.
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
  // User action                               // Listens for commands
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
                                             // User taps "Accept"
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
    *   Copy `android/app/src/main/java/com/awesomeproject/core` to your project.
    *   Copy `android/app/src/main/java/com/awesomeproject/permission` to your project.
    *   Copy the `modules` folder into your JS source root.
2.  **Register Packages**:
    *   In `MainApplication.java`, add `new OverlayCorePackage()` and `new OverlayPermissionPackage()` to the packages list.
3.  **Add Permission**:
    *   In `AndroidManifest.xml`, add `<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />`.

### How to Use

It's all handled by the `OverlayService`.

```javascript
import OverlayService from './modules/OverlayService';

// Ask for permission first (or check if you have it).
// OverlayService.checkPermission() / .requestPermission()

// Show the bubble.
OverlayService.showBubble(1, { destination: '...' });

// Show the card directly.
OverlayService.showTripCard({ destination: '...', totalFare: '...' });

// Listen for clicks.
OverlayService.addEventListener('onTripAccepted', (event) => {
  console.log('Trip accepted!', event.finalFare);
});
```

