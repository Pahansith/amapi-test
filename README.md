# AMAPI Enroll

Minimal single-activity Android app (Java) that starts a **managed Google Account
enrollment** via the AMAPI Extensibility SDK. It only drives the AccountSetup
APIs — there is **no MDM/device provisioning** in this app.

## Flow

1. Enter an `enrollmentToken` in the text box and press the button.
2. The app calls `AccountSetupClient.startAccountSetup(request)` with the token,
   the notification-receiver service component, and the admin component.
3. When the SDK reports `AUTHENTICATION_ACTIVITY_LAUNCH_REQUIRED`, the app calls
   `launchAuthenticationActivity(...)`, which shows the Google account sign-in
   activity.
4. When the SDK reports `ADDED_ACCOUNT`, the app shows a **toast** with the
   `userId`, `deviceId`, and `email`.

## Key files

- `MainActivity.java` — text box + button; owns the `AccountSetupClient`, calls
  `startAccountSetup` and `launchAuthenticationActivity`, shows the toasts.
- `accountsetup/AccountSetupNotificationReceiver.java` — extends
  `NotificationReceiverService`, implements `AccountSetupListener`, handles
  `ADDED_ACCOUNT` / `AUTHENTICATION_ACTIVITY_LAUNCH_REQUIRED` /
  `ACCOUNT_SETUP_ERROR`.
- `accountsetup/AccountSetupEvents.java` — forwards callbacks from the service to
  the foreground activity.
- `DeviceAdminReceiver.java` — used only to build the admin `ComponentName`.

## Before it will build / run

- **SDK dependency:** The AMAPI SDK is not on public Maven. Uncomment and set the
  real coordinate in `app/build.gradle`, and add its repository in
  `settings.gradle`. Package names for the imports
  (`com.google.android.managementapi.accountsetup.*`) may need to match the
  actual SDK; adjust the imports if the shipped SDK differs.
- **Device/profile owner:** `startAccountSetup` requires the app to already be
  device owner or profile owner. The app checks this and toasts a warning
  otherwise; it does not provision itself.
