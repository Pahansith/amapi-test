package com.example.amapienroll;

/**
 * The admin component for this DPC. Must be an active admin on the device —
 * {@code startAccountSetup} throws {@code AccountSetupInvalidAdminComponentException}
 * if it is not, and {@code launchAuthenticationActivity} throws
 * {@code SecurityException} unless the caller is device or profile owner.
 */
public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
}
