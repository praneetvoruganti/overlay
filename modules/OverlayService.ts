import { AppState, NativeEventEmitter, NativeModules, AppStateStatus } from 'react-native';
import OverlayPermission from './OverlayPermission';
import OverlayCore, { OverlayData, CardData } from './OverlayCore';

class OverlayService {
    private currentOverlay: OverlayData | null = null;
    private tripDataForBubble: Omit<CardData, 'type'> | null = null;

    constructor() {
        AppState.addEventListener('change', this.handleAppStateChange);
        console.log('OverlayService: Initialized and listening for AppState changes.');

        const eventEmitter = new NativeEventEmitter(NativeModules.OverlayCoreModule);
        eventEmitter.addListener('onBubbleClicked', this.handleBubbleClick);
        eventEmitter.addListener('onTripAccepted', this.handleTripAccept);
        eventEmitter.addListener('onTripDeclined', this.handleTripDecline);
    }

    private handleAppStateChange = async (nextAppState: AppStateStatus) => {
        console.log('OverlayService: AppState changed to', nextAppState);
        if (nextAppState === 'active' && this.currentOverlay) {
            console.log('OverlayService: App is active, re-showing overlay.');
            // Re-show the overlay. First hide any potential lingering one.
            await this.showOverlay(this.currentOverlay);
        }
    };

    private handleBubbleClick = () => {
        console.log('OverlayService: Bubble clicked!');
        if (this.tripDataForBubble) {
            this.showTripCard(this.tripDataForBubble);
        } else {
            console.warn('OverlayService: Bubble clicked, but no trip data available to show card.');
        }
    };

    private handleTripAccept = () => {
        console.log('OverlayService: Trip accepted!');
        this.hideOverlay();
    };

    private handleTripDecline = () => {
        console.log('OverlayService: Trip declined!');
        this.hideOverlay();
    };

    private async showOverlay(data: OverlayData): Promise<void> {
        try {
            // Always hide previous overlay before showing a new one to avoid conflicts
            if (this.currentOverlay) {
                await OverlayCore.hideOverlay();
            }

            const hasPermission = await OverlayPermission.checkOverlayPermission();
            if (!hasPermission) {
                await OverlayPermission.requestOverlayPermission();
            }

            console.log('OverlayService: Showing overlay with data:', data);
            await OverlayCore.showOverlay(data);
            this.currentOverlay = data;
        } catch (error) {
            console.error('OverlayService: Error in showOverlay:', error);
            this.currentOverlay = null; // Reset state on failure
            throw error;
        }
    }

    public async showBubble(badgeCount: number, tripData: Omit<CardData, 'type'>): Promise<void> {
        console.log('OverlayService: showBubble called.');
        this.tripDataForBubble = tripData;
        await this.showOverlay({ type: 'bubble', badgeCount });
    }

    public async showTripCard(data: Omit<CardData, 'type'>): Promise<void> {
        console.log('OverlayService: showTripCard called.');
        await this.showOverlay({ type: 'card', ...data });
    }

    public async hideOverlay(): Promise<void> {
        console.log('OverlayService: hideOverlay called.');
        if (!this.currentOverlay) return;
        try {
            await OverlayCore.hideOverlay();
            this.currentOverlay = null;
            this.tripDataForBubble = null; // Clear trip data when flow is hidden
            console.log('OverlayService: Overlay hidden successfully.');
        } catch (error) {
            console.error('OverlayService: Error in hideOverlay:', error);
            throw error;
        }
    }

    public async updateBubble(badgeCount: number): Promise<void> {
        console.log('OverlayService: updateBubble called.');
        if (this.currentOverlay?.type !== 'bubble') {
            console.warn('OverlayService: Cannot update bubble, it is not the current overlay.');
            return;
        }
        try {
            await OverlayCore.updateOverlay({ badgeCount });
            // Update internal state as well
            this.currentOverlay = { ...this.currentOverlay, badgeCount };
        } catch (error) {
            console.error('OverlayService: Error updating bubble:', error);
        }
    }
}

const overlayService = new OverlayService();
export default overlayService;
