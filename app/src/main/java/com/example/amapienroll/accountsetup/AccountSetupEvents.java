package com.example.amapienroll.accountsetup;

import com.google.android.managementapi.accountsetup.model.AccountSetupAttempt;

/**
 * Tiny bridge between the {@link AccountSetupNotificationReceiver} service
 * (where account-setup callbacks are delivered by Android Device Policy) and
 * the foreground {@code MainActivity} (which owns the {@code AccountSetupClient}
 * bound to the activity's {@code ActivityResultRegistry} and therefore must be
 * the one to launch the Google authentication activity and show UI).
 */
public final class AccountSetupEvents {

    /** Implemented by the foreground activity. */
    public interface Listener {
        /** ADDED_ACCOUNT: the managed Google Account was added to the device. */
        void onAccountAdded(String userId, String deviceId, String email);

        /** AUTHENTICATION_ACTIVITY_LAUNCH_REQUIRED: user must sign in. */
        void onAuthenticationActivityRequired(AccountSetupAttempt attempt);

        /** ACCOUNT_SETUP_ERROR. */
        void onError(String failureReason);
    }

    private static volatile Listener listener;

    private AccountSetupEvents() {}

    public static void setListener(Listener l) {
        listener = l;
    }

    public static Listener getListener() {
        return listener;
    }
}
