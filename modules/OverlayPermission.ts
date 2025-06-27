// Bridge to native Java code.
import { NativeModules } from 'react-native';

// Our specific permission module.
const { OverlayPermissionModule } = NativeModules;

// TS blueprint for the native module.
interface OverlayPermissionModuleInterface {
  // check if permission is granted.
  checkOverlayPermission(): Promise<boolean>;
  // ask for permission.
  requestOverlayPermission(): Promise<void>;
}

// Export with types.
export default OverlayPermissionModule as OverlayPermissionModuleInterface;
