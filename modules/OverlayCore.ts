// Bridge to native code.
import { NativeModules } from 'react-native';

// Our specific overlay module.
const { OverlayCoreModule } = NativeModules;

// Data for the bubble view.
export interface BubbleData {
  type: 'bubble'; // Tells native which view to inflate.
  badgeCount?: number; // Optional badge number.
}

// Data for the trip card view.
export interface CardData {
  type: 'card'; // Tells native which view to inflate.
  pickupAddress: string;
  dropoffAddress: string;
  distance: string;
  duration: string;
  baseFare: string;
  customerName: string;
  carType: string;
}

// An overlay can be a bubble or a card.
export type OverlayData = BubbleData | CardData;

// TS blueprint for the native module.
interface OverlayCoreModuleInterface {
  // Show an overlay.
  showOverlay(data: OverlayData): Promise<void>;
  // Hide any visible overlay.
  hideOverlay(): Promise<void>;
  // Update data on a visible overlay.
  updateOverlay(data: Omit<OverlayData, 'type'>): Promise<void>;
}

// Export with types.
export default OverlayCoreModule as OverlayCoreModuleInterface;
