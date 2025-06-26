import { NativeModules } from 'react-native';

const { OverlayPermissionModule } = NativeModules;

interface OverlayPermissionModuleInterface {
  checkOverlayPermission(): Promise<boolean>;
  requestOverlayPermission(): Promise<void>;
}

export default OverlayPermissionModule as OverlayPermissionModuleInterface;
