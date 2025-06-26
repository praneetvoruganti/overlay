# Overlay Permission Flow

This document outlines the user flow and technical details for handling the `SYSTEM_ALERT_WINDOW` (Draw over other apps) permission required by the application's overlay feature.

## 1. User Prompts

When the app needs to display an overlay for the first time, the user must grant the "Draw over other apps" permission. The `requestOverlayPermission()` method triggers a system dialog that takes the user to the corresponding settings screen.

-   **Intent Action**: `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
-   **Screen**: The user is redirected to the "Draw over other apps" or "Display over other apps" settings page for our specific application.
-   **User Action**: The user must manually toggle the switch to grant the permission.

There is no direct "Allow" or "Deny" dialog. The user's action of enabling the toggle is considered an approval. If they press the back button without enabling it, it is considered a denial.

## 2. Technical Callbacks (`onActivityResult`)

The result of the user's action is received in the `onActivityResult` callback within `OverlayPermissionModule.java`.

-   **Request Code**: The intent is launched with a specific request code (`OVERLAY_PERMISSION_REQUEST_CODE`).
-   **Result Handling**: When `onActivityResult` is triggered with our request code, we don't check the `resultCode` or `data`. Instead, we immediately re-check the permission status using `Settings.canDrawOverlays()`.
    -   If `Settings.canDrawOverlays()` returns `true`, the `requestOverlayPermission` Promise is resolved.
    -   If it returns `false`, the Promise is rejected with the error code `E_PERMISSION_DENIED`.

## 3. Error Cases and Promise Rejection

The `requestOverlayPermission()` Promise can be rejected in the following scenarios:

1.  **User Denial**: The user navigates to the settings screen but does not enable the permission toggle and returns to the app.
    -   **Error Code**: `E_PERMISSION_DENIED`
    -   **Message**: "User denied overlay permission"

2.  **Activity Not Found**: The `requestOverlayPermission` method is called when the app's main activity is not available.
    -   **Error Code**: `E_ACTIVITY_DOES_NOT_EXIST`
    -   **Message**: "Activity doesn't exist"

3.  **OS Revocation**: At any point, the user can manually go into the device settings and revoke the permission. The `checkOverlayPermission()` method will subsequently return `false`, and any feature requiring the overlay will need to re-request it.
