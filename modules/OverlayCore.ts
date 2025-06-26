import { NativeModules } from 'react-native';

const { OverlayCoreModule } = NativeModules;

export interface BubbleData {
  type: 'bubble';
  badgeCount?: number;
}

export interface CardData {
  type: 'card';
  fare?: string;
  pickup?: string;
  dropoff?: string;
}

export type OverlayData = BubbleData | CardData;

interface OverlayCoreModuleInterface {
  showOverlay(data: OverlayData): Promise<void>;
  hideOverlay(): Promise<void>;
  updateOverlay(data: Omit<OverlayData, 'type'>): Promise<void>;
}

export default OverlayCoreModule as OverlayCoreModuleInterface;
