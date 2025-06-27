// RN tools for app state and native events.
import { AppState, NativeEventEmitter, NativeModules, AppStateStatus } from 'react-native';

// Our overlay modules.
import OverlayPermission from './OverlayPermission';
import OverlayCore, { OverlayData, CardData } from './OverlayCore';

// Emitter for native -> JS events.
const eventEmitter = new NativeEventEmitter(NativeModules.OverlayCoreModule);

// Manages the entire overlay system.
class OverlayService {
    // --- Internal State ---
    private currentOverlay: OverlayData | null = null; // 'bubble', 'card', or null
    private tripDataForBubble: Omit<CardData, 'type'> | null = null; // Cache trip data for bubble
    private lastBadgeCount: number = 1; // Cache badge count for bubble

    // Set up listeners on init.
    constructor() {
        // Listen for app foreground/background.
        AppState.addEventListener('change', this.handleAppStateChange);
        console.log('OverlayService: Listening for app state changes.');

        // Listen for native UI events.
        eventEmitter.addListener('onBubbleClicked', this.handleBubbleClick);
        eventEmitter.addListener('onTripAccepted', this.handleTripAccept);
        eventEmitter.addListener('onTripIgnored', this.handleTripIgnored);
    }

    // --- Public API ---

    // Expose event listener.
    public addEventListener(eventName: string, handler: (...args: any[]) => any) {
        eventEmitter.addListener(eventName, handler);
    }

    // Expose event remover.
    public removeEventListener(eventName: string, handler: (...args: any[]) => any) {
        eventEmitter.removeAllListeners(eventName); // Simple cleanup.
    }

    // --- Event Handlers & Core Logic ---

    // Reshow overlay when app comes to foreground.
    private handleAppStateChange = async (nextAppState: AppStateStatus) => {
        console.log('OverlayService: App state ->', nextAppState);
        // If app is active and we had an overlay, show it again.
        if (nextAppState === 'active' && this.currentOverlay) {
            console.log('OverlayService: Re-showing overlay.');
            await this.showOverlay(this.currentOverlay);
        }
    };

    // User tapped bubble -> show card.
    private handleBubbleClick = () => {
        console.log('OverlayService: Bubble clicked.');
        if (this.tripDataForBubble) {
            this.showTripCard(this.tripDataForBubble);
        } else {
            console.warn('OverlayService: Bubble clicked, but no trip data available.');
        }
    };

    // User accepted trip.
    private handleTripAccept = (event: { finalFare: number }) => {
        console.log(`Trip accepted. Final fare: ${event.finalFare}`);
        this.hideOverlay(); // Trip accepted, just hide.
    };

    // User ignored trip.
    private handleTripIgnored = () => {
        console.log('Trip ignored.');
        // Ignored, so show bubble again.
        if (this.tripDataForBubble) {
            this.showBubble(this.lastBadgeCount, this.tripDataForBubble);
        } else {
            this.hideOverlay(); // Fallback: hide if no data.
        }
    };

    // Core logic to show an overlay.
    private async showOverlay(data: OverlayData): Promise<void> {
        try {
            if (this.currentOverlay) {
                await OverlayCore.hideOverlay(); // Hide existing overlay first.
            }
            // Check/request permission.
            const hasPermission = await OverlayPermission.checkOverlayPermission();
            if (!hasPermission) {
                await OverlayPermission.requestOverlayPermission();
            }
            console.log('OverlayService: Showing overlay:', data);
            await OverlayCore.showOverlay(data); // Call native method.
            this.currentOverlay = data; // Save current state.
        } catch (error) {
            console.error('OverlayService: Failed to show overlay:', error);
            this.currentOverlay = null; // Reset state on error.
            throw error; // Rethrow for caller.
        }
    }

    // Public method to show bubble.
    public async showBubble(badgeCount: number, tripData: Omit<CardData, 'type'>): Promise<void> {
        console.log('OverlayService: Request to show bubble.');
        // Cache data.
        this.tripDataForBubble = tripData;
        this.lastBadgeCount = badgeCount;
        // Call core show logic.
        await this.showOverlay({ type: 'bubble', badgeCount });
    }

    // Public method to show trip card.
    public async showTripCard(data: Omit<CardData, 'type'>): Promise<void> {
        console.log('OverlayService: Request to show trip card.');
        // Call core show logic.
        await this.showOverlay({ type: 'card', ...data });
    }

    // Public method to hide overlay.
    public async hideOverlay(): Promise<void> {
        console.log('OverlayService: Request to hide overlay.');
        if (!this.currentOverlay) return; // No-op if nothing is shown.
        try {
            await OverlayCore.hideOverlay(); // Call native method.
            // Reset state.
            this.currentOverlay = null;
            this.tripDataForBubble = null;
            console.log('OverlayService: Overlay hidden and state cleared.');
        } catch (error) {
            console.error('OverlayService: Failed to hide overlay:', error);
            throw error;
        }
    }

    // Public method to update bubble badge.
    public async updateBubble(badgeCount: number): Promise<void> {
        console.log('OverlayService: Request to update bubble.');
        // Can only update if bubble is visible.
        if (this.currentOverlay?.type !== 'bubble') {
            console.warn('OverlayService: Cannot update bubble, it is not visible.');
            return;
        }
        try {
            await OverlayCore.updateOverlay({ badgeCount }); // Call native update method.
            this.currentOverlay = { ...this.currentOverlay, badgeCount }; // Update internal state.
        } catch (error) {
            console.error('OverlayService: Failed to update bubble:', error);
        }
    }
}

// Singleton instance for the app.
const overlayService = new OverlayService();
// Export the instance.
export default overlayService;
